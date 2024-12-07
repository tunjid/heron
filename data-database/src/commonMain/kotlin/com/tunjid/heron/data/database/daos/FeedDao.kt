package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.FeedItemEntity

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
}