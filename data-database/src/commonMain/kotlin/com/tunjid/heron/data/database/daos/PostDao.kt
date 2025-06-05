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
import com.tunjid.heron.data.core.types.PostId
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
            WHERE cid IN (:postIds)
        """
    )
    fun posts(
        postIds: Set<PostId>,
    ): Flow<List<PopulatedPostEntity>>

    @Transaction
    @Query(
        """
            SELECT * FROM posts
	        WHERE uri IN (:postUris)
        """
    )
    fun postEntitiesByUri(
        postUris: Set<PostUri>,
    ): Flow<List<PostEntity>>

    @Transaction
    @Query(
        """
            SELECT * FROM posts
            INNER JOIN postPosts
            ON cid = postPosts.embeddedPostId
	        WHERE postId IN (:postIds)
        """
    )
    fun embeddedPosts(
        postIds: Set<PostId>,
    ): Flow<List<EmbeddedPopulatedPostEntity>>

    @Transaction
    @Query(
        """
            SELECT * FROM posts
            INNER JOIN postPosts
            ON cid = postPosts.embeddedPostId
	        WHERE embeddedPostId = :quotedPostId
            ORDER BY indexedAt
        """
    )
    fun quotedPosts(
        quotedPostId: String,
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
	        WHERE postId = :postId
            ORDER BY indexedAt
            DESC
            LIMIT :limit
            OFFSET :offset
        """
    )
    fun likedBy(
        postId: String,
        viewingProfileId: String?,
        limit: Long,
        offset: Long,
    ): Flow<List<PopulatedProfileEntity>>

    @Transaction
    @Query(
        """
            SELECT * FROM profiles
            INNER JOIN postReposts
                ON did = authorId
            LEFT JOIN profileViewerStates
                ON profileViewerStates.profileId = :viewingProfileId
                AND profileViewerStates.otherProfileId = authorId
	        WHERE postId = :postId
            ORDER BY indexedAt
            DESC
            LIMIT :limit
            OFFSET :offset
        """
    )
    fun repostedBy(
        postId: String,
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
                SELECT postId,
                    parentPostId,
                    -1 AS generation,
                    postId AS rootPostId,
                    -1 AS sort1
                FROM postThreads
                WHERE postId = :postId
            ),
            parents AS (
                SELECT * FROM parentGeneration
                UNION ALL
                SELECT parent.postId,
                    parent.parentPostId,
                    generation-1 AS generation,
                    rootPostId,
                    sort1-1 AS sort1
                FROM postThreads parent
                JOIN parentGeneration g
                  ON parent.postId = g.parentPostId 
            ),
            replyGeneration AS (
                SELECT postId,
                    parentPostId,
                    1 AS generation,
                    postId AS rootPostId,
                    posts.createdAt AS sort1
                FROM postThreads
                INNER JOIN posts
                ON postId = posts.cid
                WHERE parentPostId = :postId
            ),
            replies AS (
                SELECT * FROM replyGeneration
                UNION ALL
                SELECT reply.postId,
                    reply.parentPostId,
                    generation+1 AS generation,
                    rootPostId,
                    sort1 AS sort1
                FROM postThreads reply
                JOIN replyGeneration g
                  ON reply.parentPostId = g.postId
            )

            SELECT * FROM(
                SELECT *
                FROM posts
                INNER JOIN parents
                ON cid = parents.postId
                WHERE cid != :postId
            )
            
            UNION
            
            SELECT * FROM(
                SELECT posts.*,
                posts.cid,
                postThreads.parentPostId,
                0,
                NULL,
                0 AS sort1
                FROM posts
                INNER JOIN postThreads
                WHERE cid == :postId
                LIMIT 1
            )
            
            UNION
            
            SELECT * FROM(
                SELECT *
                FROM posts
                INNER JOIN replies
                ON cid = replies.postId
                WHERE cid != :postId
            )
            
            ORDER BY sort1, generation
        """
    )
    fun postThread(
        postId: String,
    ): Flow<List<ThreadedPostEntity>>
}
