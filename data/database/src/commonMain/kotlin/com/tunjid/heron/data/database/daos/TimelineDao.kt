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
import com.tunjid.heron.data.database.entities.TimelineItemEntity
import com.tunjid.heron.data.database.entities.TimelinePreferencesEntity
import com.tunjid.heron.data.database.entities.fetchedAtPartial
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineDao {

    @Query(
        """
        DELETE FROM timelineItems
        WHERE sourceId = :sourceId
    """,
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
            AND CASE WHEN :viewingProfileId IS NOT NULL
                THEN viewingProfileId = :viewingProfileId
                ELSE viewingProfileId IS NULL
            END
            AND CASE WHEN :hideReplies = TRUE
                THEN parentPostUri IS NULL
                ELSE 1
            END
            AND CASE WHEN :hideReposts = TRUE
                THEN reposter IS NULL
                ELSE 1
            END
            AND CASE WHEN :hideQuotePosts = TRUE
                THEN embeddedRecordUri IS NULL OR embeddedRecordUri NOT LIKE '%app.bsky.feed.post%'
                ELSE 1
            END
            ORDER BY itemSort
            DESC
            LIMIT :limit
            OFFSET :offset
        """,
    )
    fun feedItems(
        viewingProfileId: String?,
        sourceId: String,
        before: Instant,
        limit: Long,
        offset: Long,
        hideReplies: Boolean,
        hideReposts: Boolean,
        hideQuotePosts: Boolean,
    ): Flow<List<TimelineItemEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM timelineItems
        WHERE sourceId = :sourceId
        AND CASE WHEN :viewingProfileId IS NOT NULL
            THEN viewingProfileId = :viewingProfileId
            ELSE viewingProfileId IS NULL
        END
    """,
    )
    fun count(
        viewingProfileId: String?,
        sourceId: String,
    ): Flow<Long>

    @Query(
        """
            SELECT * FROM timelinePreferences
            WHERE sourceId = :sourceId
            AND CASE WHEN :viewingProfileId IS NOT NULL
                THEN viewingProfileId = :viewingProfileId
                ELSE viewingProfileId IS NULL
            END
            LIMIT 1
        """,
    )
    fun lastFetchKey(
        viewingProfileId: String?,
        sourceId: String,
    ): Flow<TimelinePreferencesEntity?>

    @Transaction
    suspend fun insertOrPartiallyUpdateTimelineFetchedAt(
        entities: List<TimelinePreferencesEntity>,
    ) = partialUpsert(
        items = entities,
        partialMapper = TimelinePreferencesEntity::fetchedAtPartial,
        insertEntities = ::insertOrIgnoreTimelinePreferences,
        updatePartials = ::updatePartialTimelinePreferencesFetchedAt,
    )

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreTimelinePreferences(
        entities: List<TimelinePreferencesEntity>,
    ): List<Long>

    @Transaction
    @Update(entity = TimelinePreferencesEntity::class)
    suspend fun updatePartialTimelinePreferencesFetchedAt(
        entities: List<TimelinePreferencesEntity.Partial.FetchedAt>,
    )

    @Transaction
    @Update(entity = TimelinePreferencesEntity::class)
    suspend fun updatePreferredTimelinePresentation(
        partial: TimelinePreferencesEntity.Partial.PreferredPresentation,
    )
}
