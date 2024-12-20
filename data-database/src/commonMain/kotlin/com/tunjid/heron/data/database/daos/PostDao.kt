package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.EmbeddedPopulatedPostEntity
import com.tunjid.heron.data.database.entities.PopulatedPostEntity
import com.tunjid.heron.data.database.entities.PostAuthorsEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.PostThreadEntity
import com.tunjid.heron.data.database.entities.ThreadedPopulatedPostEntity
import com.tunjid.heron.data.database.entities.postembeds.PostExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.PostImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostPostEntity
import com.tunjid.heron.data.database.entities.postembeds.PostVideoEntity
import com.tunjid.heron.data.database.entities.profile.PostViewerStatisticsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    @Upsert
    suspend fun upsertPosts(
        entities: List<PostEntity>,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreAuthorCrossRefEntities(
        crossReferences: List<PostAuthorsEntity>,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnorePostExternalEmbeds(
        crossReferences: List<PostExternalEmbedEntity>,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnorePostImages(
        crossReferences: List<PostImageEntity>,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnorePostVideos(
        crossReferences: List<PostVideoEntity>,
    )

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
        postIds: Set<Id>,
    ): Flow<List<PopulatedPostEntity>>

    @Transaction
    @Query(
        """
            SELECT * FROM posts
	        WHERE uri IN (:postUris)
        """
    )
    fun postEntitiesByUri(
        postUris: Set<Uri>,
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
        postIds: Set<Id>,
    ): Flow<List<EmbeddedPopulatedPostEntity>>

    @Upsert
    suspend fun upsertPostStatistics(
        entities: List<PostViewerStatisticsEntity>,
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
    ): Flow<List<ThreadedPopulatedPostEntity>>
}
