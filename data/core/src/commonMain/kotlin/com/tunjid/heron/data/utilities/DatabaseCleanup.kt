/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.data.utilities

import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.database.daos.DatabaseCleanupDao
import com.tunjid.heron.data.di.IODispatcher
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
class DatabaseCleanup(
    @param:IODispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val databaseCleanupDao: DatabaseCleanupDao,
) {
    /**
     * Cleans up stale posts when the database exceeds [MaxPosts].
     *
     * Waits [StartupDelay] to avoid competing with initial data loads, then runs
     * on the IO dispatcher to keep cleanup off the main thread.
     *
     * Deletes the least important posts (by engagement score and age) down to [TargetPosts],
     * then removes orphaned media entities and profiles left behind.
     *
     * Each batch deletion and orphan cleanup runs as its own implicit Room transaction
     * rather than wrapping everything in a single large transaction. This avoids blocking
     * concurrent writes (e.g., timeline fetches) during startup while still being safe:
     * partial progress is valid since each batch is independently consistent, and any
     * posts that sneak in between the count check and deletion simply mean slightly fewer
     * posts are removed — the next cleanup cycle will catch up.
     */
    suspend fun cleanup() {
        delay(StartupDelay)
        withContext(ioDispatcher) {
            cleanupPosts()
            cleanupNotifications()
            cleanupOrphans()
        }
    }

    private suspend fun cleanupPosts() {
        val count = databaseCleanupDao.postCount()
        if (count <= MaxPosts) return

        val deleteCount = (count - TargetPosts).toInt()
        val candidates = databaseCleanupDao.findDeletablePostUris(
            sentinelPostUris = listOf(
                Constants.unknownPostUri,
            ),
            limit = deleteCount,
        )

        candidates.chunked(BatchSize).forEach { batch ->
            databaseCleanupDao.deletePostsByUri(batch)
        }
    }

    /**
     * Caps notifications per owner to [MaxNotificationsPerOwner], keeping the newest.
     */
    private suspend fun cleanupNotifications() {
        databaseCleanupDao.deleteOldNotifications(maxPerOwner = MaxNotificationsPerOwner)
    }

    /**
     * Cleans up orphaned entities left behind after post and notification deletion:
     * standalone media entities whose junction rows were cascade-deleted,
     * and profiles no longer referenced by any table.
     */
    private suspend fun cleanupOrphans() {
        databaseCleanupDao.deleteOrphanedImages()
        databaseCleanupDao.deleteOrphanedVideos()
        databaseCleanupDao.deleteOrphanedExternalEmbeds()
        databaseCleanupDao.deleteOrphanedProfiles(
            sentinelProfileIds = listOf(
                Constants.unknownAuthorId,
                Constants.guestProfileId,
                Constants.pendingProfileId,
            ),
        )
    }
}

private val StartupDelay = 5.seconds
private const val MaxPosts = 10_000L
private const val TargetPosts = 7_500L
private const val MaxNotificationsPerOwner = 500
private const val BatchSize = 500
