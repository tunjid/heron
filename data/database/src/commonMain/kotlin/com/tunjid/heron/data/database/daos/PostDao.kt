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
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.database.entities.EmbeddedPopulatedPostEntity
import com.tunjid.heron.data.database.entities.PopulatedPostEntity
import com.tunjid.heron.data.database.entities.PopulatedProfileEntity
import com.tunjid.heron.data.database.entities.PostAuthorsEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.PostLikeEntity
import com.tunjid.heron.data.database.entities.PostThreadEntity
import com.tunjid.heron.data.database.entities.ThreadedPostEntity
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
            LEFT JOIN (
                SELECT * FROM postViewerStatistics
                WHERE viewingProfileId = :viewingProfileId
            )
            ON uri = postUri
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
            LEFT JOIN (
                SELECT * FROM postViewerStatistics
                WHERE viewingProfileId = :viewingProfileId
            )
            ON uri = postUri
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
            LEFT JOIN (
                SELECT * FROM postViewerStatistics
                WHERE viewingProfileId = :viewingProfileId
            ) AS postViewerStatistics
            ON posts.uri = postViewerStatistics.postUri
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
            SELECT
                posts.*,
                postViewerStatistics.*,
                postPosts.postUri AS parentPostUri
            FROM posts AS posts
            LEFT JOIN (
                SELECT * FROM postViewerStatistics
                WHERE viewingProfileId = :viewingProfileId
            ) AS postViewerStatistics
            ON posts.uri = postViewerStatistics.postUri
            INNER JOIN postPosts AS postPosts
            ON posts.uri = postPosts.embeddedPostUri
	        WHERE postPosts.embeddedPostUri = :quotedPostUri
            ORDER BY posts.indexedAt
        """,
    )
    fun quotedPosts(
        viewingProfileId: String?,
        quotedPostUri: String,
    ): Flow<List<PopulatedPostEntity>>

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
    UPDATE posts
    SET bookmarkCount =
        CASE
            WHEN :isBookmarked THEN COALESCE(bookmarkCount, 0) + 1
            ELSE CASE
                     WHEN COALESCE(bookmarkCount, 0) > 0 THEN bookmarkCount - 1
                     ELSE 0
                 END
        END
    WHERE uri = :postUri
    """,
    )
    suspend fun updateBookmarkCount(
        postUri: String,
        isBookmarked: Boolean,
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
    suspend fun upsertPostThreads(
        entities: List<PostThreadEntity>,
    )

    @Transaction
    @Query(
        """
            WITH RECURSIVE
              -- 1. ParentHierarchy CTE: Recursively finds all parent URIs for the given post
              -- and calculates their generation (negative).
              ParentHierarchy(uri, rootPostUri, generation, sort) AS (
                SELECT
                  pt.parentPostUri, -- each parent is its own root
                  pt.postUri AS rootPostUri,
                  -1 AS generation,
                  -1 AS sort -- sort parents of the OP strictly by their generation
                FROM
                  postThreads pt
                WHERE postUri = :postUri
                UNION ALL
                SELECT
                  pt.parentPostUri,
                  ph.rootPostUri,
                  ph.generation - 1,
                  ph.sort -1
                FROM
                  postThreads pt
                INNER JOIN
                  ParentHierarchy ph ON pt.postUri = ph.uri
              ),

              -- 2. ChildHierarchy CTE: Recursively finds all child URIs for the given post
              -- and calculates their generation (positive).
              ChildHierarchy(uri, rootPostUri, generation, sort) AS (
                SELECT
                  pt.postUri,
                  pt.postUri AS rootPostUri, -- add the very first reply to the OP as the root
                  1 AS generation,
                  p.createdAt as sort -- sort all replies by the very first reply to the OP
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
                  ch.sort
                FROM
                  postThreads pt
                INNER JOIN
                  ChildHierarchy ch ON pt.parentPostUri = ch.uri
              ),

              -- 3. FullThread CTE: Combines the URIs and generations from parents, children,
              -- and the post itself (generation 0).
              FullThread(uri, rootPostUri, generation, sort) AS (
                SELECT uri, rootPostUri, generation, sort FROM ParentHierarchy
                UNION
                SELECT uri, rootPostUri, generation, sort FROM ChildHierarchy
                UNION
                SELECT :postUri, NULL, 0, 0
              )

            -- 4. Final SELECT: Fetches all columns from the `posts` table for every URI
            -- identified in the FullThread CTE, along with its calculated generation and the root post URI.
            SELECT
              p.*,
              ft.rootPostUri AS rootPostUri,
              ft.generation AS generation,
              ft.sort AS sort
            FROM
              posts p
            JOIN
              FullThread ft ON p.uri = ft.uri
            ORDER BY
              ft.sort, ft.generation; -- sort by the first reply to the op, then the generation
        """,
    )
    fun postThread(
        postUri: String,
    ): Flow<List<ThreadedPostEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM posts WHERE uri = :postUri)")
    suspend fun postExists(postUri: String): Boolean
}
