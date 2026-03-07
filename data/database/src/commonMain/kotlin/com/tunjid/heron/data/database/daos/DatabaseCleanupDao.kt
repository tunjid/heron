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

package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId

@Dao
interface DatabaseCleanupDao {

    @Query(
        """
        SELECT COUNT(*)
        FROM posts
        """,
    )
    suspend fun postCount(): Long

    /**
     * Finds posts eligible for deletion, ordered by importance (least important first).
     *
     * A post is protected (not deletable) if it is:
     * - A sentinel post (e.g., unknown/blocked/not-found placeholders via [sentinelPostUris])
     * - Referenced by any timeline item (as postUri, rootPostUri, or parentPostUri)
     * - Bookmarked (in the bookmarks table)
     * - Pinned or bookmarked in postViewerStatistics
     * - Referenced by a notification
     * - Referenced in a message
     * - Embedded in another post (conservative: protects for one extra cycle)
     *
     * Importance score: likeCount + repostCount*2 + replyCount + quoteCount*2
     * Tiebreaker: older indexedAt deleted first.
     */
    @Query(
        """
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
        WHERE p.uri NOT IN (:sentinelPostUris)
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
        LIMIT :limit
        """,
    )
    suspend fun findDeletablePostUris(sentinelPostUris: List<PostUri>, limit: Int): List<PostUri>

    @Query(
        """
        DELETE FROM posts
        WHERE uri IN (:uris)
        """,
    )
    suspend fun deletePostsByUri(uris: List<PostUri>)

    @Query(
        """
        DELETE FROM images
        WHERE fullSize NOT IN (
            SELECT imageUri
            FROM postImages
        )
        """,
    )
    suspend fun deleteOrphanedImages()

    @Query(
        """
        DELETE FROM videos
        WHERE cid NOT IN (
            SELECT videoId
            FROM postVideos
        )
        """,
    )
    suspend fun deleteOrphanedVideos()

    @Query(
        """
        DELETE FROM externalEmbeds
        WHERE uri NOT IN (
            SELECT externalEmbedUri
            FROM postExternalEmbeds
        )
        """,
    )
    suspend fun deleteOrphanedExternalEmbeds()

    /**
     * Deletes old notifications beyond [maxPerOwner] for each owner profile,
     * keeping the newest ones. A notification is deleted if there are already
     * [maxPerOwner] or more notifications for the same owner with a more recent indexedAt.
     */
    @Query(
        """
        DELETE FROM notifications
        WHERE (
            SELECT COUNT(*)
            FROM notifications n2
            WHERE n2.ownerId = notifications.ownerId
              AND n2.indexedAt > notifications.indexedAt
        ) >= :maxPerOwner
        """,
    )
    suspend fun deleteOldNotifications(maxPerOwner: Int)

    /**
     * Deletes profiles not referenced by any table with a CASCADE foreign key to profiles.
     * Uses UNION to collect all referenced profile IDs across the schema.
     * Sentinel profiles (unknown, guest, pending) are excluded via [sentinelProfileIds].
     */
    @Query(
        """
        DELETE FROM profiles
        WHERE did NOT IN (:sentinelProfileIds)
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
        """,
    )
    suspend fun deleteOrphanedProfiles(sentinelProfileIds: List<ProfileId>)
}
