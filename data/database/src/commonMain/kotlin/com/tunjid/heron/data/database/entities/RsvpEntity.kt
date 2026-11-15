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
import androidx.room.Relation
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Rsvp
import com.tunjid.heron.data.core.types.EventUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RsvpId
import com.tunjid.heron.data.core.types.RsvpUri
import kotlin.time.Instant

@Entity(
    tableName = "rsvps",
    primaryKeys = [
        "eventUri",
        "authorId",
    ],
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["uri"],
            childColumns = ["eventUri"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
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
        Index(value = ["eventUri"]),
        Index(value = ["authorId"]),
        Index(value = ["sortedAt"]),
    ],
)
data class RsvpEntity(
    val uri: RsvpUri,
    val cid: RsvpId?,
    val eventUri: EventUri,
    val authorId: ProfileId,
    val status: String,
    val sortedAt: Instant,
) {
    data class Deletion(
        val eventUri: EventUri,
        val authorId: ProfileId,
    )
}

data class PopulatedRsvpEntity(
    @Embedded
    val entity: RsvpEntity,
    @Relation(
        parentColumn = "authorId",
        entityColumn = "did",
    )
    val author: ProfileEntity?,
)

fun PopulatedRsvpEntity.asExternalModel() = entity.asExternalModel(
    author = author.asExternalModel(),
)

internal fun RsvpEntity.asExternalModel(
    author: Profile,
) = Rsvp(
    uri = uri,
    cid = cid,
    eventUri = eventUri,
    status = Rsvp.Status.fromId(status),
    sortedAt = sortedAt,
    author = author,
)
