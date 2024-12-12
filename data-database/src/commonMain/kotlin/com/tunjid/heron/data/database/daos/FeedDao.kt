package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.FeedFetchKeyEntity
import com.tunjid.heron.data.database.entities.FeedItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface FeedDao {

    @Query(
        """
        DELETE FROM feedItems
        WHERE sourceId = :sourceId
    """
    )
    suspend fun deleteAllFeedsFor(
        sourceId: String,
    )

    @Upsert
    suspend fun upsertFeedItems(
        entities: List<FeedItemEntity>,
    )

    @Query(
        """
            SELECT * FROM feedItems
            WHERE sourceId = :sourceId
            AND indexedAt < :before
            ORDER BY indexedAt
            DESC
            LIMIT :limit
        """
    )
    fun feedItems(
        sourceId: String,
        before: Instant,
        limit: Long,
    ): Flow<List<FeedItemEntity>>

    @Query(
        """
            SELECT * FROM feedFetchKeys
            WHERE sourceId = :sourceId
            LIMIT 1
        """
    )
    suspend fun lastFetchKey(
        sourceId: String,
    ): FeedFetchKeyEntity?

    @Upsert
    suspend fun upsertFeedFetchKey(
        entity: FeedFetchKeyEntity,
    )
}
