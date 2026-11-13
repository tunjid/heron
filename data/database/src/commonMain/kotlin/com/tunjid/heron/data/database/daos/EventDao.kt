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
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.tunjid.heron.data.core.models.Rsvp
import com.tunjid.heron.data.core.types.EventUri
import com.tunjid.heron.data.core.types.RsvpUri
import com.tunjid.heron.data.database.entities.EventEntity
import com.tunjid.heron.data.database.entities.PopulatedEventEntity
import com.tunjid.heron.data.database.entities.RsvpEntity
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    // --- Events ---

    @Query(
        """
            SELECT
                events.*,
                profiles.did as author_did,
                profiles.handle as author_handle,
                profiles.displayName as author_displayName,
                profiles.avatar as author_avatar,
                rsvps.uri as rsvp_uri,
                rsvps.cid as rsvp_cid,
                rsvps.eventUri as rsvp_eventUri,
                rsvps.viewingProfileId as rsvp_viewingProfileId,
                rsvps.status as rsvp_status,
                rsvps.sortedAt as rsvp_sortedAt
            FROM events
            LEFT JOIN rsvps
                ON events.uri = rsvps.eventUri
                AND rsvps.viewingProfileId IS :viewingProfileId
            INNER JOIN profiles
                ON events.authorId = profiles.did
            WHERE events.uri = :uri
        """,
    )
    fun event(
        viewingProfileId: String?,
        uri: EventUri,
    ): Flow<PopulatedEventEntity?>

    @Query(
        """
            SELECT
                events.*,
                profiles.did as author_did,
                profiles.handle as author_handle,
                profiles.displayName as author_displayName,
                profiles.avatar as author_avatar,
                rsvps.uri as rsvp_uri,
                rsvps.cid as rsvp_cid,
                rsvps.eventUri as rsvp_eventUri,
                rsvps.viewingProfileId as rsvp_viewingProfileId,
                rsvps.status as rsvp_status,
                rsvps.sortedAt as rsvp_sortedAt
            FROM events
            LEFT JOIN rsvps
                ON events.uri = rsvps.eventUri
                AND rsvps.viewingProfileId IS :viewingProfileId
            INNER JOIN profiles
                ON events.authorId = profiles.did
            WHERE events.uri IN (:uris)
        """,
    )
    fun events(
        viewingProfileId: String?,
        uris: Collection<EventUri>,
    ): Flow<List<PopulatedEventEntity>>

    @Query(
        """
            SELECT
                events.*,
                profiles.did as author_did,
                profiles.handle as author_handle,
                profiles.displayName as author_displayName,
                profiles.avatar as author_avatar,
                rsvps.uri as rsvp_uri,
                rsvps.cid as rsvp_cid,
                rsvps.eventUri as rsvp_eventUri,
                rsvps.viewingProfileId as rsvp_viewingProfileId,
                rsvps.status as rsvp_status,
                rsvps.sortedAt as rsvp_sortedAt
            FROM events
            LEFT JOIN rsvps
                ON events.uri = rsvps.eventUri
                AND rsvps.viewingProfileId IS :viewingProfileId
            INNER JOIN profiles
                ON events.authorId = profiles.did
            WHERE events.authorId = :authorId
            ORDER BY events.sortedAt DESC
            LIMIT :limit
            OFFSET :offset
        """,
    )
    fun authorEvents(
        viewingProfileId: String?,
        authorId: String,
        limit: Long,
        offset: Long,
    ): Flow<List<PopulatedEventEntity>>

    @Query(
        """
            SELECT
                events.*,
                profiles.did as author_did,
                profiles.handle as author_handle,
                profiles.displayName as author_displayName,
                profiles.avatar as author_avatar,
                rsvps.uri as rsvp_uri,
                rsvps.cid as rsvp_cid,
                rsvps.eventUri as rsvp_eventUri,
                rsvps.viewingProfileId as rsvp_viewingProfileId,
                rsvps.status as rsvp_status,
                rsvps.sortedAt as rsvp_sortedAt
            FROM events
            INNER JOIN rsvps
                ON events.uri = rsvps.eventUri
                AND rsvps.viewingProfileId = :viewingProfileId
            INNER JOIN profiles
                ON events.authorId = profiles.did
            WHERE :status IS NULL OR rsvps.status = :status
            ORDER BY rsvps.sortedAt DESC
            LIMIT :limit
            OFFSET :offset
        """,
    )
    fun rsvpedEvents(
        viewingProfileId: String,
        status: String?,
        limit: Long,
        offset: Long,
    ): Flow<List<PopulatedEventEntity>>

    @Upsert
    suspend fun upsertEvents(
        entities: List<EventEntity>,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreEvents(
        entities: List<EventEntity>,
    ): List<Long>

    @Query(
        """
            DELETE FROM events
            WHERE uri = :uri
        """,
    )
    suspend fun deleteEvent(
        uri: EventUri,
    )

    @Query(
        """
            DELETE FROM events
            WHERE authorId = :authorId
        """,
    )
    suspend fun deleteEventsForAuthor(
        authorId: String,
    )

    @Query(
        """
    UPDATE events
    SET goingCount = MAX(
            0,
            goingCount
                + (CASE WHEN :newStatus = :goingStatus THEN 1 ELSE 0 END)
                - (CASE WHEN :previousStatus = :goingStatus THEN 1 ELSE 0 END)
        ),
        interestedCount = MAX(
            0,
            interestedCount
                + (CASE WHEN :newStatus = :interestedStatus THEN 1 ELSE 0 END)
                - (CASE WHEN :previousStatus = :interestedStatus THEN 1 ELSE 0 END)
        ),
        notGoingCount = MAX(
            0,
            notGoingCount
                + (CASE WHEN :newStatus = :notGoingStatus THEN 1 ELSE 0 END)
                - (CASE WHEN :previousStatus = :notGoingStatus THEN 1 ELSE 0 END)
        )
    WHERE uri = :eventUri
    """,
    )
    suspend fun adjustRsvpCounts(
        eventUri: String,
        previousStatus: String?,
        newStatus: String,
        goingStatus: String,
        interestedStatus: String,
        notGoingStatus: String,
    )

    // --- RSVPs ---

    @Query(
        """
    UPDATE events
    SET goingCount = MAX(
            0,
            goingCount - (SELECT COUNT(*) FROM rsvps WHERE uri = :rsvpUri AND status = :goingStatus)
        ),
        interestedCount = MAX(
            0,
            interestedCount - (SELECT COUNT(*) FROM rsvps WHERE uri = :rsvpUri AND status = :interestedStatus)
        ),
        notGoingCount = MAX(
            0,
            notGoingCount - (SELECT COUNT(*) FROM rsvps WHERE uri = :rsvpUri AND status = :notGoingStatus)
        )
    WHERE uri IN (SELECT eventUri FROM rsvps WHERE uri = :rsvpUri)
    """,
    )
    suspend fun decrementRsvpCounts(
        rsvpUri: RsvpUri,
        goingStatus: String,
        interestedStatus: String,
        notGoingStatus: String,
    )

    @Upsert
    suspend fun upsertRsvps(
        entities: List<RsvpEntity>,
    )

    @Delete(entity = RsvpEntity::class)
    suspend fun deleteRsvps(
        entities: List<RsvpEntity.Deletion>,
    )

    @Query(
        """
            DELETE FROM rsvps
            WHERE uri = :uri
        """,
    )
    suspend fun deleteRsvp(
        uri: RsvpUri,
    )

    @Query(
        """
            DELETE FROM rsvps
            WHERE viewingProfileId = :viewingProfileId
            AND (:status IS NULL OR status = :status)
        """,
    )
    suspend fun deleteRsvpsForViewer(
        viewingProfileId: String,
        status: String?,
    )
}
