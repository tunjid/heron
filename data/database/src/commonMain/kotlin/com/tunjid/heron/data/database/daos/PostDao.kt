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
        """
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
        """
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
        """
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
        """
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
        """
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
    """
    )
    suspend fun updateLikeCount(
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
        """
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

    @Upsert
    suspend fun upsertPostThreads(
        entities: List<PostThreadEntity>,
    )

    @Transaction
    @Query(
        """
            WITH RECURSIVE 
            parentGeneration AS (
                SELECT postUri,
                    parentPostUri,
                    -1 AS generation,
                    postUri AS rootPostUri,
                    -1 AS sort1
                FROM postThreads
                WHERE postUri = :postUri
            ),
            parents AS (
                SELECT * FROM parentGeneration
                UNION ALL
                SELECT parent.postUri,
                    parent.parentPostUri,
                    generation-1 AS generation,
                    rootPostUri,
                    sort1-1 AS sort1
                FROM postThreads parent
                JOIN parentGeneration g
                  ON parent.postUri = g.parentPostUri 
            ),
            replyGeneration AS (
                SELECT postUri,
                    parentPostUri,
                    1 AS generation,
                    postUri AS rootPostUri,
                    posts.createdAt AS sort1
                FROM postThreads
                INNER JOIN posts
                ON postUri = posts.uri
                WHERE parentPostUri = :postUri
            ),
            replies AS (
                SELECT * FROM replyGeneration
                UNION ALL
                SELECT reply.postUri,
                    reply.parentPostUri,
                    generation+1 AS generation,
                    rootPostUri,
                    sort1 AS sort1
                FROM postThreads reply
                JOIN replyGeneration g
                  ON reply.parentPostUri = g.postUri
            )

            SELECT * FROM(
                SELECT *
                FROM posts
                INNER JOIN parents
                ON uri = parents.postUri
                WHERE uri != :postUri
            )
            
            UNION
            
            SELECT * FROM(
                SELECT posts.*,
                posts.uri,
                postThreads.parentPostUri,
                0,
                NULL,
                0 AS sort1
                FROM posts
                INNER JOIN postThreads
                WHERE uri == :postUri
                LIMIT 1
            )
            
            UNION
            
            SELECT * FROM(
                SELECT *
                FROM posts
                INNER JOIN replies
                ON uri = replies.postUri
                WHERE uri != :postUri
            )
            
            ORDER BY sort1, generation
        """
    )
    fun postThread(
        postUri: String,
    ): Flow<List<ThreadedPostEntity>>
}
