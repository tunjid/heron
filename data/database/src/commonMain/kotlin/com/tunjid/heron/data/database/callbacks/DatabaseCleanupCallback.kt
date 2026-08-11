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

package com.tunjid.heron.data.database.callbacks

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.tunjid.heron.data.core.models.Constants

/**
 * Runs a bounded cache cleanup exactly once, when the database is first opened, before any DAO
 * query can observe the data.
 */
internal object DatabaseCleanupCallback : RoomDatabase.Callback() {

    override fun onOpen(connection: SQLiteConnection) {
        // Best-effort: a cleanup failure must never prevent the database from opening.
        // The next launch will try again.
        try {
            connection.cleanUp()
        } catch (_: Exception) {
        }
    }

    private fun SQLiteConnection.cleanUp() {
        val postCount = prepare("SELECT COUNT(*) FROM posts").use { statement ->
            statement.step()
            statement.getLong(0)
        }

        if (postCount > MaxPosts) {
            val deleteCount = minOf(postCount - TargetPosts, MaxDeletesPerOpen)
            execSQL(deletePostsSql(limit = deleteCount))
            // Reclaim media and profiles orphaned by the cascade above.
            execSQL(DeleteOrphanedImagesSql)
            execSQL(DeleteOrphanedVideosSql)
            execSQL(DeleteOrphanedExternalEmbedsSql)
            execSQL(DeleteOrphanedProfilesSql)
        }

        execSQL(DeleteOldNotificationsSql)
    }
}

/**
 * Deletes the least important posts (engagement score then age, oldest first), capped at [limit].
 *
 * A post is protected (never deleted) if it is the unknown-post sentinel, or referenced by a
 * timeline item (as post/root/parent), a bookmark, pinned/bookmarked viewer statistics, a
 * notification, a message, or embedded in another post. Deliberately *not* protected: posts
 * reachable only through `postThreads` — thread context is allowed to be culled because it
 * re-fetches on open, and protecting it here would pin those posts permanently (the `postThreads`
 * edges only cascade-delete when their posts do, so protecting both endpoints never releases them).
 */
private fun deletePostsSql(limit: Long) = """
    DELETE FROM posts
    WHERE uri IN (
        SELECT p.uri FROM posts p
        LEFT JOIN timelineItems t1 ON p.uri = t1.postUri
        LEFT JOIN timelineItems t2 ON p.uri = t2.rootPostUri
        LEFT JOIN timelineItems t3 ON p.uri = t3.parentPostUri
        LEFT JOIN bookmarks b ON p.uri = b.bookmarkedUri
        LEFT JOIN postViewerStatistics pvs ON p.uri = pvs.postUri
            AND (pvs.pinned = 1 OR pvs.bookmarked = 1)
        LEFT JOIN notifications n ON p.uri = n.associatedPostUri
        LEFT JOIN messagePosts mp ON p.uri = mp.postUri
        LEFT JOIN postPosts pp ON p.uri = pp.embeddedPostUri
        WHERE p.uri != '${Constants.unknownPostUri.uri}'
          AND t1.postUri IS NULL
          AND t2.rootPostUri IS NULL
          AND t3.parentPostUri IS NULL
          AND b.bookmarkedUri IS NULL
          AND pvs.postUri IS NULL
          AND n.associatedPostUri IS NULL
          AND mp.postUri IS NULL
          AND pp.embeddedPostUri IS NULL
        ORDER BY
            (COALESCE(p.likeCount, 0) + COALESCE(p.repostCount, 0) * 2
             + COALESCE(p.replyCount, 0) + COALESCE(p.quoteCount, 0) * 2) ASC,
            p.indexedAt ASC
        LIMIT $limit
    )
""".trimIndent()

private const val DeleteOrphanedImagesSql =
    "DELETE FROM images WHERE fullSize NOT IN (SELECT imageUri FROM postImages)"

private const val DeleteOrphanedVideosSql =
    "DELETE FROM videos WHERE cid NOT IN (SELECT videoId FROM postVideos)"

private const val DeleteOrphanedExternalEmbedsSql =
    "DELETE FROM externalEmbeds WHERE uri NOT IN (SELECT externalEmbedUri FROM postExternalEmbeds)"

private val DeleteOrphanedProfilesSql = """
    DELETE FROM profiles
    WHERE did NOT IN (
        '${Constants.unknownAuthorId.id}',
        '${Constants.guestProfileId.id}',
        '${Constants.pendingProfileId.id}'
    )
      AND did NOT IN (
        SELECT authorId FROM posts
        UNION SELECT grandParentPostAuthorId FROM timelineItems WHERE grandParentPostAuthorId IS NOT NULL
        UNION SELECT ownerId FROM conversations
        UNION SELECT memberId FROM conversationMembers
        UNION SELECT senderId FROM messages
        UNION SELECT authorId FROM notifications
        UNION SELECT ownerId FROM notifications
        UNION SELECT creatorId FROM feedGenerators
        UNION SELECT creatorId FROM lists
        UNION SELECT creatorId FROM starterPacks
        UNION SELECT creatorId FROM labelers
        UNION SELECT publisherId FROM standardPublications
        UNION SELECT authorId FROM standardDocuments
    )
""".trimIndent()

private val DeleteOldNotificationsSql = """
    DELETE FROM notifications
    WHERE (
        SELECT COUNT(*)
        FROM notifications n2
        WHERE n2.ownerId = notifications.ownerId
          AND n2.indexedAt > notifications.indexedAt
    ) >= $MaxNotificationsPerOwner
""".trimIndent()

private const val MaxPosts = 10_000L
private const val TargetPosts = 7_500L

/**
 * Upper bound on posts deleted per open. Keeps the synchronous first-open cull short; a larger
 * backlog is worked down over subsequent launches.
 */
private const val MaxDeletesPerOpen = 3_000L
private const val MaxNotificationsPerOwner = 500
