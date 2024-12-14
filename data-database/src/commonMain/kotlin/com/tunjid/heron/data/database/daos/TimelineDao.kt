package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tunjid.heron.data.database.entities.TimelineFetchKeyEntity
import com.tunjid.heron.data.database.entities.TimelineItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface TimelineDao {

    @Query(
        """
        DELETE FROM timelineItems
        WHERE sourceId = :sourceId
    """
    )
    suspend fun deleteAllFeedsFor(
        sourceId: String,
    )

    @Upsert
    suspend fun upsertTimelineItems(
        entities: List<TimelineItemEntity>,
    )

    @Query(
        """
            SELECT * FROM timelineItems
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
    ): Flow<List<TimelineItemEntity>>

    @Query(
        """
            SELECT * FROM timelineFetchKeys
            WHERE sourceId = :sourceId
            LIMIT 1
        """
    )
    suspend fun lastFetchKey(
        sourceId: String,
    ): TimelineFetchKeyEntity?

    @Upsert
    suspend fun upsertFeedFetchKey(
        entity: TimelineFetchKeyEntity,
    )
}
