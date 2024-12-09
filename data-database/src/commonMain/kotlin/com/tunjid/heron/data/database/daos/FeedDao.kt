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
        WHERE source = :source
    """
    )
    suspend fun deleteAllFeedsFor(
        source: Uri,
    )

    @Upsert
    suspend fun upsertFeedItems(
        entities: List<FeedItemEntity>,
    )

    @Query(
        """
            SELECT * FROM feedItems
            WHERE source = :source
            AND indexedAt < :before
            ORDER BY indexedAt
            DESC
            LIMIT :limit
        """
    )
    fun feedItems(
        source: Uri,
        before: Instant,
        limit: Long,
    ): Flow<List<FeedItemEntity>>

    @Query(
        """
            SELECT * FROM feedFetchKeys
            WHERE feedUri = :feedUri
            LIMIT 1
        """
    )
    suspend fun lastFetchKey(
        feedUri: Uri,
    ): FeedFetchKeyEntity?

    @Upsert
    suspend fun upsertFeedFetchKey(
        entity: FeedFetchKeyEntity,
    )
}
