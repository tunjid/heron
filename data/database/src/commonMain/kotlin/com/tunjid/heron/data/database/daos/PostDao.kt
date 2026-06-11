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
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.tunjid.heron.data.core.types.LikeUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.RepostUri
import com.tunjid.heron.data.database.entities.BookmarkEntity
import com.tunjid.heron.data.database.entities.EmbeddedPopulatedPostEntity
import com.tunjid.heron.data.database.entities.PopulatedPostEntity
import com.tunjid.heron.data.database.entities.PopulatedProfileEntity
import com.tunjid.heron.data.database.entities.PostAuthorsEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.PostLikeEntity
import com.tunjid.heron.data.database.entities.PostThreadEntity
import com.tunjid.heron.data.database.entities.ThreadedPostEntity
import com.tunjid.heron.data.database.entities.partial
import com.tunjid.heron.data.database.entities.postembeds.PostExternalAssociatedProfilesEntity
import com.tunjid.heron.data.database.entities.postembeds.PostExternalAssociatedRecordEntity
import com.tunjid.heron.data.database.entities.postembeds.PostExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.PostImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostPostEntity
import com.tunjid.heron.data.database.entities.postembeds.PostVideoEntity
import com.tunjid.heron.data.database.entities.profile.PostViewerStatisticsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    @Transaction
    @Upsert
    suspend fun upsertPosts(
        entities: List<PostEntity>,
    )

    @Transaction
    suspend fun insertOrPartiallyUpdatePosts(
        entities: List<PostEntity>,
    ) = partialUpsert(
        items = entities,
        partialMapper = PostEntity::partial,
        insertEntities = ::insertOrIgnorePosts,
        updatePartials = ::updatePartialPosts,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnorePosts(
        entities: List<PostEntity>,
    ): List<Long>

    @Update(entity = PostEntity::class)
    suspend fun updatePartialPosts(
        entities: List<PostEntity.Partial>,
    )

    @Transaction
    @Upsert
    suspend fun upsertPostLikes(
        entities: List<PostLikeEntity>,
    )

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreAuthorCrossRefEntities(
        crossReferences: List<PostAuthorsEntity>,
    )

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnorePostExternalEmbeds(
        crossReferences: List<PostExternalEmbedEntity>,
    )

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPostExternalAssociatedRecords(
        crossReferences: List<PostExternalAssociatedRecordEntity>,
    )

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPostExternalAssociatedProfiles(
        crossReferences: List<PostExternalAssociatedProfilesEntity>,
    )

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnorePostImages(
        crossReferences: List<PostImageEntity>,
    )

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnorePostVideos(
        crossReferences: List<PostVideoEntity>,
    )

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnorePostPosts(
        crossReferences: List<PostPostEntity>,
    )

    @Transaction
    @Query(
        """
            SELECT * FROM posts
            LEFT JOIN postViewerStatistics
                ON posts.uri = postViewerStatistics.postUri AND postViewerStatistics.viewingProfileId = :viewingProfileId
            LEFT JOIN profileViewerStates
                ON profileViewerStates.profileId = :viewingProfileId
                AND posts.authorId = profileViewerStates.otherProfileId
                AND :viewingProfileId IS NOT NULL
            WHERE uri IN (:postUris)
        """,
    )
    fun posts(
        viewingProfileId: String?,
        postUris: Collection<PostUri>,
    ): Flow<List<PopulatedPostEntity>>

    @Transaction
    @Query(
        """
            SELECT * FROM posts
            LEFT JOIN postViewerStatistics
                ON posts.uri = postViewerStatistics.postUri AND postViewerStatistics.viewingProfileId = :viewingProfileId
	        WHERE uri IN (:postUris)
        """,
    )
    fun postEntitiesByUri(
        viewingProfileId: String?,
        postUris: Set<PostUri>,
    ): Flow<List<PostEntity>>

    @Transaction
    @Query(
        """
            SELECT
                posts.*,
                postViewerStatistics.*,
                postPosts.postUri AS parentPostUri,
                postPosts.embeddedPostUri AS embeddedPostUri
            FROM posts AS posts
            LEFT JOIN postViewerStatistics
                ON posts.uri = postViewerStatistics.postUri AND postViewerStatistics.viewingProfileId = :viewingProfileId
            LEFT JOIN profileViewerStates
                ON profileViewerStates.profileId = :viewingProfileId
                AND posts.authorId = profileViewerStates.otherProfileId
                AND :viewingProfileId IS NOT NULL
            INNER JOIN postPosts AS postPosts
                ON posts.uri = postPosts.embeddedPostUri
	        WHERE postPosts.postUri IN (:postUris)
        """,
    )
    fun embeddedPosts(
        viewingProfileId: String?,
        postUris: Collection<PostUri>,
    ): Flow<List<EmbeddedPopulatedPostEntity>>

    @Transaction
    @Query(
        """
        SELECT uri, embeddedRecordUri FROM posts
        INNER JOIN bookmarks
            ON posts.uri = bookmarks.bookmarkedUri
        WHERE bookmarks.viewingProfileId = :viewingProfileId
        ORDER BY bookmarks.createdAt DESC
        LIMIT :limit
        OFFSET :offset
        """,
    )
    fun bookmarkedPostUriAndEmbeddedRecordUris(
        viewingProfileId: String,
        limit: Long,
        offset: Long,
    ): Flow<List<PostEntity.UriWithEmbeddedRecordUri>>

    @Transaction
    @Query(
        """
            SELECT uri, embeddedRecordUri FROM posts
            INNER JOIN postPosts AS postPosts
                ON posts.uri = postPosts.postUri
	        WHERE postPosts.embeddedPostUri = :quotedPostUri
            ORDER BY posts.indexedAt DESC
            LIMIT :limit
            OFFSET :offset
        """,
    )
    fun quotedPostUriAndEmbeddedRecordUris(
        quotedPostUri: String,
        limit: Long,
        offset: Long,
    ): Flow<List<PostEntity.UriWithEmbeddedRecordUri>>

    @Transaction
    @Query(
        """
            SELECT * FROM profiles
            INNER JOIN postLikes
                ON did = authorId
            LEFT JOIN profileViewerStates
                ON profileViewerStates.profileId = :viewingProfileId
                AND profileViewerStates.otherProfileId = authorId
	        WHERE postUri = :postUri
            ORDER BY indexedAt
            DESC
            LIMIT :limit
            OFFSET :offset
        """,
    )
    fun likedBy(
        postUri: String,
        viewingProfileId: String?,
        limit: Long,
        offset: Long,
    ): Flow<List<PopulatedProfileEntity>>

    @Transaction
    @Query(
        """
    UPDATE posts
    SET likeCount =
        CASE
            WHEN :isIncrement THEN COALESCE(likeCount, 0) + 1
            ELSE CASE
                     WHEN COALESCE(likeCount, 0) > 0 THEN likeCount - 1
                     ELSE 0
                 END
        END
    WHERE uri = :postUri
    """,
    )
    suspend fun updateLikeCount(
        postUri: String,
        isIncrement: Boolean,
    )

    @Transaction
    @Query(
        """
    UPDATE posts
    SET repostCount =
        CASE
            WHEN :isIncrement THEN COALESCE(repostCount, 0) + 1
            ELSE CASE
                     WHEN COALESCE(repostCount, 0) > 0 THEN repostCount - 1
                     ELSE 0
                 END
        END
    WHERE uri = :postUri
    """,
    )
    suspend fun updateRepostCount(
        postUri: String,
        isIncrement: Boolean,
    )

    @Transaction
    @Query(
        """
            SELECT * FROM profiles
            INNER JOIN postReposts
                ON did = authorId
            LEFT JOIN profileViewerStates
                ON profileViewerStates.profileId = :viewingProfileId
                AND profileViewerStates.otherProfileId = authorId
	        WHERE postUri = :postUri
            ORDER BY indexedAt
            DESC
            LIMIT :limit
            OFFSET :offset
        """,
    )
    fun repostedBy(
        postUri: String,
        viewingProfileId: String?,
        limit: Long,
        offset: Long,
    ): Flow<List<PopulatedProfileEntity>>

    @Upsert
    suspend fun upsertPostStatistics(
        entities: List<PostViewerStatisticsEntity>,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnorePostStatistics(
        entities: List<PostViewerStatisticsEntity>,
    ): List<Long>

    @Update(entity = PostViewerStatisticsEntity::class)
    suspend fun updatePostStatisticsLikes(
        entities: List<PostViewerStatisticsEntity.Partial.Like>,
    )

    @Update(entity = PostViewerStatisticsEntity::class)
    suspend fun updatePostStatisticsReposts(
        entities: List<PostViewerStatisticsEntity.Partial.Repost>,
    )

    @Update(entity = PostViewerStatisticsEntity::class)
    suspend fun updatePostStatisticsBookmarks(
        entities: List<PostViewerStatisticsEntity.Partial.Bookmark>,
    )

    @Upsert
    suspend fun upsertBookmarks(
        entities: List<BookmarkEntity>,
    )

    @Upsert
    suspend fun upsertPostThreads(
        entities: List<PostThreadEntity>,
    )

    @Transaction
    @Query(
        """
            WITH RECURSIVE
              -- 1. ParentHierarchy CTE: Recursively finds all parent URIs for the given post
              -- and calculates their generation (negative).
              ParentHierarchy(uri, rootPostUri, generation, ancestorCreated, postCreated, ancestorLikeCount, ancestorIsOp) AS (
                SELECT
                  pt.parentPostUri, -- each parent is its own root
                  pt.postUri AS rootPostUri,
                  -1 AS generation,
                  -1 AS ancestorCreated, -- Always a negative constant for ancestors
                  -1 AS postCreated, -- Always a negative constant for ancestors
                  0 AS ancestorLikeCount,
                  0 AS ancestorIsOp -- only used for replies (generation > 0)
                FROM
                  postThreads pt
                WHERE postUri = :postUri
                UNION ALL
                SELECT
                  pt.parentPostUri,
                  ph.rootPostUri,
                  ph.generation - 1,
                  ph.ancestorCreated - 1,
                  ph.postCreated - 1,
                  0,
                  0
                FROM
                  postThreads pt
                INNER JOIN
                  ParentHierarchy ph ON pt.postUri = ph.uri
              ),

              -- 1b. OpAuthor CTE: the thread's OP, i.e. the author of the thread root. The root is
              -- the furthest known ancestor (most negative generation); if the anchor has no
              -- ancestors it is itself the root. Evaluated once.
              OpAuthor(opId) AS (
                SELECT authorId FROM posts
                WHERE uri = COALESCE(
                  (SELECT uri FROM ParentHierarchy ORDER BY generation ASC LIMIT 1),
                  :postUri
                )
              ),

              -- 2. ChildHierarchy CTE: Recursively finds all child URIs for the given post
              -- and calculates their generation (positive).
              ChildHierarchy(uri, rootPostUri, generation, ancestorCreated, postCreated, ancestorLikeCount, ancestorIsOp) AS (
                SELECT
                  pt.postUri,
                  pt.postUri AS rootPostUri, -- add the very first reply to the OP as the root
                  1 AS generation,
                  p.createdAt as ancestorCreated, -- sort all replies by the very first reply to the OP
                  p.createdAt as postCreated, -- second sort is the actual post's createdAt
                  COALESCE(p.likeCount, 0) as ancestorLikeCount, -- like count of the root reply for top sort
                  CASE WHEN p.authorId = (SELECT opId FROM OpAuthor) THEN 1 ELSE 0 END as ancestorIsOp -- root reply authored by OP
                FROM
                  postThreads pt
                JOIN posts p ON pt.postUri = p.uri
                WHERE
                  parentPostUri = :postUri
                UNION ALL
                SELECT
                  pt.postUri,
                  ch.rootPostUri,
                  ch.generation + 1,
                  ch.ancestorCreated, -- preserve the ancestorCreatedDate in the thread
                  p.createdAt, -- pull in the actual post's createdAt date
                  ch.ancestorLikeCount, -- preserve the root reply's like count in the thread
                  ch.ancestorIsOp -- preserve whether the root reply is by the OP
                FROM
                  postThreads pt
                INNER JOIN posts p ON pt.postUri = p.uri
                INNER JOIN
                  ChildHierarchy ch ON pt.parentPostUri = ch.uri
              ),

              -- 3. FullThread CTE: Combines the URIs and generations from parents, children,
              -- and the post itself (generation 0).
              FullThread(uri, rootPostUri, generation, ancestorCreated, postCreated, ancestorLikeCount, ancestorIsOp) AS (
                SELECT uri, rootPostUri, generation, ancestorCreated, postCreated, ancestorLikeCount, ancestorIsOp FROM ParentHierarchy
                UNION
                SELECT uri, rootPostUri, generation, ancestorCreated, postCreated, ancestorLikeCount, ancestorIsOp FROM ChildHierarchy
                UNION
                SELECT :postUri, NULL, 0, 0, 0, 0, 0
              )

            -- 4. Final SELECT: Fetches all columns from the `posts` table for every URI
            -- identified in the FullThread CTE, along with its calculated generation and the root post URI.
            -- sortOrder: 0 = oldest first, 1 = newest first, 2 = top (most liked) first
            SELECT
              p.*,
              ft.rootPostUri AS rootPostUri,
              ft.generation AS generation,
              ft.ancestorCreated AS ancestorCreated,
              ft.postCreated AS postCreated,
              pt.parentPostUri AS parentPostUri
            FROM
              posts p
            JOIN
              FullThread ft ON p.uri = ft.uri
            LEFT JOIN
              postThreads pt ON pt.postUri = p.uri
            ORDER BY
              -- Tier 1: ancestors before main post before replies
              CASE
                WHEN ft.generation < 0 THEN 0
                WHEN ft.generation = 0 THEN 1
                ELSE 2
              END,
              -- Tier 2: within ancestors, sort by generation (most distant first)
              CASE
                WHEN ft.generation < 0 THEN ft.generation
              END,
              -- Tier 2.5: bump OP-authored top-level reply chains above the rest (server applyBumping).
              -- Keyed on the propagated root-reply flag so each chain's subtree stays contiguous.
              CASE
                WHEN ft.generation > 0 AND ft.ancestorIsOp = 1 THEN 0
                ELSE 1
              END,
              -- Tier 3: within replies, sort by chosen criteria for the root reply chain
              CASE
                WHEN ft.generation > 0 AND :sortOrder = 0 THEN ft.ancestorCreated
                WHEN ft.generation > 0 AND :sortOrder = 1 THEN -ft.ancestorCreated
                WHEN ft.generation > 0 AND :sortOrder = 2 THEN -ft.ancestorLikeCount
              END,
              -- Tier 3b: keep each top-level subtree contiguous; break root like-ties by recency
              CASE
                WHEN ft.generation > 0 THEN -ft.ancestorCreated
              END,
              -- Tier 4: within a subtree, group by depth so chains stay top-down
              ft.generation,
              -- Tier 4.5: bump OP-authored replies above their siblings at each deeper level
              -- (server applyBumping, applied per sibling group). Gen 1 is handled by Tier 2.5.
              CASE
                WHEN ft.generation > 1 AND p.authorId = (SELECT opId FROM OpAuthor) THEN 0
                ELSE 1
              END,
              -- Tier 5: order siblings at each depth by the chosen criteria (post's own values)
              CASE
                WHEN ft.generation > 0 AND :sortOrder = 0 THEN ft.postCreated
                WHEN ft.generation > 0 AND :sortOrder = 1 THEN -ft.postCreated
                WHEN ft.generation > 0 AND :sortOrder = 2 THEN -COALESCE(p.likeCount, 0)
              END,
              -- Tier 6: 'top' like-ties break by newest (match server); stable final fallback
              CASE
                WHEN :sortOrder = 0 THEN ft.postCreated
                ELSE -ft.postCreated
              END;
        """,
    )
    fun postThread(
        postUri: String,
        sortOrder: Int,
    ): Flow<List<ThreadedPostEntity>>

    @Query(
        """
            DELETE FROM posts
            WHERE uri = :postUri
        """,
    )
    suspend fun deletePost(
        postUri: PostUri,
    )

    @Query(
        """
            UPDATE postViewerStatistics
            SET likeUri = NULL
            WHERE likeUri = :likeUri
        """,
    )
    suspend fun deletePostViewerStatisticsLike(
        likeUri: LikeUri,
    )

    @Query(
        """
            UPDATE postViewerStatistics
            SET repostUri = NULL
            WHERE repostUri = :repostUri
        """,
    )
    suspend fun deletePostViewerStatisticsRepost(
        repostUri: RepostUri,
    )
}
