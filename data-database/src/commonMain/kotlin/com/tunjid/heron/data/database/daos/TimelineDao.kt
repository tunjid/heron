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
