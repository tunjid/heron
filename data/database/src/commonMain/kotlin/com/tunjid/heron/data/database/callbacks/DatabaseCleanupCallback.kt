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
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.time.TimeMark
import kotlin.time.TimeSource

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
        val deadline = TimeSource.Monotonic.markNow() + CleanupBudget

        // Timeline items protect the posts they reference, so stale timelines must go first
        // for the post cull below to find anything to delete.
        deleteStaleTimelines(
            staleBefore = Clock.System.now() - StaleTimelineAge,
            deadline = deadline,
        )

        val postCount = prepare("SELECT COUNT(*) FROM posts").use { statement ->
            statement.step()
            statement.getLong(0)
        }

        if (postCount > MaxPosts) {
            val deletedCount = deletePosts(
                limit = postCount - TargetPosts,
                deadline = deadline,
            )
            if (deletedCount > 0) {
                // Reclaim media and profiles orphaned by the cascade above.
                execSQL(DeleteOrphanedImagesSql)
                execSQL(DeleteOrphanedVideosSql)
                execSQL(DeleteOrphanedExternalEmbedsSql)
                execSQL(DeleteOrphanedProfilesSql)
            }
        }

        execSQL(DeleteOldNotificationsSql)
    }

    /**
     * Deletes timelines not refreshed since [staleBefore], oldest first, until [deadline].
     *
     * Each timeline is removed whole, along with its preferences row, so it reopens exactly as if
     * it had never been visited: a fresh load instead of resuming from a stale anchor and cursor.
     * Timelines with a preferred presentation are kept, as it is stored on the same row.
     */
    private fun SQLiteConnection.deleteStaleTimelines(
        staleBefore: Instant,
        deadline: TimeMark,
    ) {
        val staleTimelines = prepare(StaleTimelinesSql).use { statement ->
            statement.bindLong(
                index = 1,
                value = staleBefore.toEpochMilliseconds(),
            )
            buildList {
                while (statement.step()) {
                    add(
                        StaleTimeline(
                            id = statement.getText(index = 0),
                            sourceId = statement.getText(index = 1),
                            viewingProfileId =
                            if (statement.isNull(index = 2)) null
                            else statement.getText(index = 2),
                        ),
                    )
                }
            }
        }

        prepare(DeleteTimelineItemsSql).use { deleteItems ->
            prepare(DeleteTimelinePreferencesSql).use { deletePreferences ->
                for (timeline in staleTimelines) {
                    if (deadline.hasPassedNow()) break

                    deleteItems.bindText(
                        index = 1,
                        value = timeline.sourceId,
                    )
                    when (val viewingProfileId = timeline.viewingProfileId) {
                        null -> deleteItems.bindNull(index = 2)
                        else -> deleteItems.bindText(
                            index = 2,
                            value = viewingProfileId,
                        )
                    }
                    deleteItems.step()
                    deleteItems.reset()

                    deletePreferences.bindText(
                        index = 1,
                        value = timeline.id,
                    )
                    deletePreferences.step()
                    deletePreferences.reset()
                }
            }
        }
    }

    /**
     * Deletes up to [limit] unprotected posts in batches of [PostDeleteBatchSize] until
     * [deadline], returning how many were deleted.
     */
    private fun SQLiteConnection.deletePosts(
        limit: Long,
        deadline: TimeMark,
    ): Long {
        var deletedCount = 0L
        // Protected posts never become deletable mid-cleanup, so each batch resumes after the
        // last one instead of rescanning them.
        var lastRowId = Long.MIN_VALUE
        prepare(DeletablePostRowIdsSql).use { statement ->
            while (deletedCount < limit && !deadline.hasPassedNow()) {
                statement.bindLong(
                    index = 1,
                    value = lastRowId,
                )
                statement.bindLong(
                    index = 2,
                    value = minOf(PostDeleteBatchSize, limit - deletedCount),
                )
                val rowIds = buildList {
                    while (statement.step()) add(statement.getLong(index = 0))
                }
                statement.reset()

                if (rowIds.isEmpty()) break

                execSQL("DELETE FROM posts WHERE rowid IN (${rowIds.joinToString()})")
                deletedCount += rowIds.size
                lastRowId = rowIds.last()
            }
        }
        return deletedCount
    }
}

private class StaleTimeline(
    val id: String,
    val sourceId: String,
    val viewingProfileId: String?,
)

/**
 * Selects the rowids of the oldest unprotected posts after a given rowid, capped at a limit.
 *
 * Posts are written insert-or-ignore then updated in place, so rowid order is the order posts
 * were first cached in. Walking it lets SQLite stop at the limit instead of sorting every post.
 *
 * A post is protected (never deleted) if it is the unknown-post sentinel, or referenced by a
 * timeline item (as post/root/parent), a bookmark, pinned/bookmarked viewer statistics, a
 * notification, a message, or embedded in another post. Deliberately *not* protected: posts
 * reachable only through `postThreads` — thread context is allowed to be culled because it
 * re-fetches on open, and protecting it here would pin those posts permanently (the `postThreads`
 * edges only cascade-delete when their posts do, so protecting both endpoints never releases them).
 */
private val DeletablePostRowIdsSql = """
    SELECT p.rowid FROM posts p
    LEFT JOIN timelineItems t1 ON p.uri = t1.postUri
    LEFT JOIN timelineItems t2 ON p.uri = t2.rootPostUri
    LEFT JOIN timelineItems t3 ON p.uri = t3.parentPostUri
    LEFT JOIN bookmarks b ON p.uri = b.bookmarkedUri
    LEFT JOIN postViewerStatistics pvs ON p.uri = pvs.postUri
        AND (pvs.pinned = 1 OR pvs.bookmarked = 1)
    LEFT JOIN notifications n ON p.uri = n.associatedPostUri
    LEFT JOIN messagePosts mp ON p.uri = mp.postUri
    LEFT JOIN postPosts pp ON p.uri = pp.embeddedPostUri
    WHERE p.rowid > ?
      AND p.uri != '${Constants.unknownPostUri.uri}'
      AND t1.postUri IS NULL
      AND t2.rootPostUri IS NULL
      AND t3.parentPostUri IS NULL
      AND b.bookmarkedUri IS NULL
      AND pvs.postUri IS NULL
      AND n.associatedPostUri IS NULL
      AND mp.postUri IS NULL
      AND pp.embeddedPostUri IS NULL
    ORDER BY p.rowid ASC
    LIMIT ?
""".trimIndent()

private const val StaleTimelinesSql = """
    SELECT id, sourceId, viewingProfileId FROM timelinePreferences
    WHERE lastFetchedAt < ?
      AND preferredPresentation IS NULL
    ORDER BY lastFetchedAt ASC
"""

private const val DeleteTimelineItemsSql =
    "DELETE FROM timelineItems WHERE sourceId = ? AND viewingProfileId IS ?"

private const val DeleteTimelinePreferencesSql =
    "DELETE FROM timelinePreferences WHERE id = ?"

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
        UNION SELECT verifiedProfileId FROM profileVerifications
        UNION SELECT issuerProfileId FROM profileVerifications
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
private const val PostDeleteBatchSize = 500L
private const val MaxNotificationsPerOwner = 500

private val CleanupBudget = 250.milliseconds
private val StaleTimelineAge = 14.days
