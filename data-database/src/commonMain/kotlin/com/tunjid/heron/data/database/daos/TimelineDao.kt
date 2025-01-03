package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tunjid.heron.data.database.entities.FeedGeneratorEntity
import com.tunjid.heron.data.database.entities.ListEntity
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
            OFFSET :offset
        """
    )
    fun feedItems(
        sourceId: String,
        before: Instant,
        limit: Long,
        offset: Long,
    ): Flow<List<TimelineItemEntity>>

    @Query(
        """
            SELECT * FROM timelineFetchKeys
            WHERE sourceId = :sourceId
            LIMIT 1
        """
    )
    fun lastFetchKey(
        sourceId: String,
    ): Flow<TimelineFetchKeyEntity?>

    @Upsert
    suspend fun upsertFeedFetchKey(
        entity: TimelineFetchKeyEntity,
    )

    @Query(
        """
            SELECT * FROM lists
            WHERE uri = :listUri
        """
    )
    fun list(
        listUri: String,
    ): Flow<ListEntity?>

    @Upsert
    suspend fun upsertLists(
        entities: List<ListEntity>,
    )

    @Query(
        """
            SELECT * FROM feedGenerators
            WHERE uri = :feedUri
        """
    )
    fun feedGenerator(
        feedUri: String,
    ): Flow<FeedGeneratorEntity?>

    @Upsert
    suspend fun upsertFeedGenerators(
        entities: List<FeedGeneratorEntity>,
    )
}
