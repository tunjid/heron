package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.FeedItemEntity
import kotlinx.coroutines.flow.Flow

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
            LIMIT :limit
        """
    )
    fun feedItems(
        source: Uri,
        limit: Long,
    ): Flow<List<FeedItemEntity>>
}
