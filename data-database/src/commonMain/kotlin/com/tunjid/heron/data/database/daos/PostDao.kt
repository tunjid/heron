package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.database.entities.EmbeddedPopulatedPostEntity
import com.tunjid.heron.data.database.entities.PopulatedPostEntity
import com.tunjid.heron.data.database.entities.PostAuthorsEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.PostThreadEntity
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

    @Query(
        """
            SELECT * FROM postThreads
	        WHERE postId IN (:postIds)
            ORDER BY postId
        """
    )
    fun postThreads(
        postIds: Set<Id>,
    ): Flow<List<PostThreadEntity>>
}
