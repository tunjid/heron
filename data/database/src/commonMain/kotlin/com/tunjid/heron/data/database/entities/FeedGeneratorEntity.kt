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
import androidx.room.Relation
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.types.FeedGeneratorId
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileId
import kotlinx.datetime.Instant


@Entity(
    tableName = "feedGenerators",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["creatorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["uri"]),
        Index(value = ["cid"]),
        Index(value = ["creatorId"]),
        Index(value = ["indexedAt"]),
        Index(value = ["createdAt"]),
    ],
)
data class FeedGeneratorEntity(
    val cid: FeedGeneratorId,
    val did: FeedGeneratorId,
    @PrimaryKey
    val uri: FeedGeneratorUri,
    val avatar: ImageUri?,
    val likeCount: Long?,
    val creatorId: ProfileId,
    val displayName: String,
    val description: String?,
    val acceptsInteractions: Boolean?,
    val contentMode: String?,
    val indexedAt: Instant,
    val createdAt: Instant,
)

data class PopulatedFeedGeneratorEntity(
    @Embedded
    val entity: FeedGeneratorEntity,
    @Relation(
        parentColumn = "creatorId",
        entityColumn = "did"
    )
    val creator: ProfileEntity?,
    @Relation(
        parentColumn = "uri",
        entityColumn = "uri",
    )
    val labelEntities: List<LabelEntity>,
)

fun PopulatedFeedGeneratorEntity.asExternalModel() =
    FeedGenerator(
        cid = entity.cid,
        did = entity.did,
        uri = entity.uri,
        avatar = entity.avatar,
        likeCount = entity.likeCount,
        creator = creator.asExternalModel(),
        displayName = entity.displayName,
        description = entity.description,
        acceptsInteractions = entity.acceptsInteractions,
        contentMode = entity.contentMode,
        indexedAt = entity.indexedAt,
        labels = labelEntities.map(LabelEntity::asExternalModel),
    )
