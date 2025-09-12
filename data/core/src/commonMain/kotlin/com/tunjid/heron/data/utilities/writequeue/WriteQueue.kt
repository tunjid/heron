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

package com.tunjid.heron.data.utilities.writequeue

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.PostRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.inCurrentProfileSession
import com.tunjid.heron.data.repository.onEachSignedInProfile
import com.tunjid.heron.data.repository.signedInProfileId
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import okio.IOException

sealed class WriteQueue {

    internal abstract val postRepository: PostRepository
    internal abstract val profileRepository: ProfileRepository

    internal abstract val messageRepository: MessageRepository

    internal abstract val timelineRepository: TimelineRepository

    abstract val queueChanges: Flow<List<Writable>>

    abstract suspend fun enqueue(
        writable: Writable,
    ): Status

    abstract suspend fun awaitDequeue(
        writable: Writable,
    )

    abstract suspend fun drain()

    sealed interface Status {
        data object Enqueued : Status
        data object Dropped : Status
        data object Duplicate : Status
    }
}

internal class SnapshotWriteQueue @Inject constructor(
    override val postRepository: PostRepository,
    override val profileRepository: ProfileRepository,
    override val messageRepository: MessageRepository,
    override val timelineRepository: TimelineRepository,
) : WriteQueue() {
    // At some point this queue should be persisted to disk
    private val queue = mutableStateListOf<Writable>()

    override val queueChanges: Flow<List<Writable>>
        get() = snapshotFlow { queue.toList() }
            .distinctUntilChangedBy {
                it.map(Writable::queueId)
            }

    override suspend fun enqueue(
        writable: Writable,
    ): Status {
        // Enqueue on main
        return withContext(Dispatchers.Main) {
            // De-dup
            if (queue.any { writable.queueId == it.queueId }) return@withContext Status.Duplicate
            queue.add(writable)
            Status.Enqueued
        }
    }

    override suspend fun awaitDequeue(writable: Writable) {
        snapshotFlow {
            queue.firstOrNull { it.queueId == writable.queueId }
        }.first { it == null }
    }

    override suspend fun drain() {
        snapshotFlow { queue.lastOrNull() }
            .filterNotNull()
            .collect { writable ->
                withContext(Dispatchers.IO) {
                    with(writable) {
                        write()
                    }
                }
                // Dequeue on main
                withContext(Dispatchers.Main) {
                    queue.removeLast()
                }
            }
    }
}

internal class PersistedWriteQueue @Inject constructor(
    override val postRepository: PostRepository,
    override val profileRepository: ProfileRepository,
    override val messageRepository: MessageRepository,
    override val timelineRepository: TimelineRepository,
    private val savedStateDataSource: SavedStateDataSource,
) : WriteQueue() {

    override val queueChanges: Flow<List<Writable>>
        get() = savedStateDataSource.signedInProfileWrites()

    override suspend fun enqueue(
        writable: Writable,
    ): Status {
        var status: Status = Status.Dropped
        savedStateDataSource.inCurrentProfileSession {
            savedStateDataSource.updateWrites {
                when {
                    pendingWrites.any { writable.queueId == it.queueId } -> {
                        status = Status.Duplicate
                        this
                    }
                    else -> {
                        status = Status.Enqueued
                        copy(pendingWrites = listOf(writable) + pendingWrites)
                    }
                }
            }
        }
        return status
    }

    override suspend fun awaitDequeue(writable: Writable) {
        savedStateDataSource.signedInProfileWrites()
            .first { writes ->
                writes.none { it.queueId == writable.queueId }
            }
    }

    override suspend fun drain() = savedStateDataSource.onEachSignedInProfile {
        savedStateDataSource.signedInProfileWrites()
            .mapNotNull(List<Writable>::lastOrNull)
            .distinctUntilChangedBy(Writable::queueId)
            .collect { writable ->
                val outcome = withContext(Dispatchers.IO) {
                    with(writable) {
                        write()
                    }
                }
                savedStateDataSource.updateWrites {
                    copy(
                        failedWrites = when (outcome) {
                            is Outcome.Failure ->
                                failedWrites
                                    .plus(
                                        FailedWrite(
                                            writable = writable,
                                            failedAt = Clock.System.now(),
                                            reason = when (outcome.exception) {
                                                is IOException -> FailedWrite.Reason.IO
                                                else -> null
                                            },
                                        ),
                                    )
                                    .distinctBy { it.writable.queueId }
                                    .takeLast(MaximumFailedWrites)
                            Outcome.Success -> failedWrites
                        },
                        // Always dequeue write
                        pendingWrites = pendingWrites
                            .filter { it.queueId != writable.queueId }
                            .take(MaximumPendingWrites),
                    )
                }
            }
    }
}

private fun SavedStateDataSource.signedInProfileWrites() = savedState
    .mapNotNull { it.signedInProfileId }
    .distinctUntilChanged()
    .flatMapLatest { profileId ->
        savedState
            .mapNotNull { savedState ->
                savedState
                    .takeIf { it.signedInProfileId == profileId }
                    ?.signedInProfileData
                    ?.writes
                    ?.pendingWrites
            }
    }
    .distinctUntilChangedBy { pendingWrites ->
        pendingWrites.map(Writable::queueId)
    }

private suspend inline fun SavedStateDataSource.updateWrites(
    crossinline block: SavedState.Writes.(signedInProfileId: ProfileId?) -> SavedState.Writes,
) {
    updateSignedInProfileData { signedInProfileId ->
        copy(writes = writes.block(signedInProfileId))
    }
}

private const val MaximumPendingWrites = 15
private const val MaximumFailedWrites = 10
