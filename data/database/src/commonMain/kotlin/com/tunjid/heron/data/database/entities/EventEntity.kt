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

package com.tunjid.heron.data.database.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.models.Event
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.core.types.EventId
import com.tunjid.heron.data.core.types.EventUri
import com.tunjid.heron.data.core.types.ProfileId
import kotlin.time.Instant

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["uri"]),
        Index(value = ["authorId"]),
        Index(value = ["sortedAt"]),
        Index(value = ["startsAt"]),
    ],
)
data class EventEntity(
    @PrimaryKey
    val uri: EventUri,
    val cid: EventId?,
    val authorId: ProfileId,
    val name: String,
    val description: String?,
    val createdAt: Instant,
    val startsAt: Instant?,
    val endsAt: Instant?,
    val mode: String?,
    val status: String?,
    val base64EncodedPlaces: String?,
    val rsvpExpected: Boolean?,
    val sortedAt: Instant,
    val goingCount: Long,
    val interestedCount: Long,
    val notGoingCount: Long,
)

data class PopulatedEventEntity(
    @Embedded
    val entity: EventEntity,
    @Embedded(prefix = "author_")
    val author: ProfileEntity,
    @Embedded(prefix = "rsvp_")
    val rsvp: RsvpEntity?,
)

fun PopulatedEventEntity.asExternalModel() = Event(
    uri = entity.uri,
    cid = entity.cid,
    author = author.asExternalModel(),
    name = entity.name,
    description = entity.description,
    createdAt = entity.createdAt,
    startsAt = entity.startsAt,
    endsAt = entity.endsAt,
    mode = Event.Mode.fromId(entity.mode),
    status = Event.Status.fromId(entity.status),
    places = entity.base64EncodedPlaces
        ?.fromBase64EncodedUrl<Event.Places>()
        ?: EmptyPlaces,
    rsvpExpected = entity.rsvpExpected == true,
    sortedAt = entity.sortedAt,
    goingCount = entity.goingCount,
    interestedCount = entity.interestedCount,
    notGoingCount = entity.notGoingCount,
    rsvp = rsvp?.asExternalModel(),
)

private val EmptyPlaces = Event.Places(
    locations = emptyList(),
    links = emptyList(),
)
