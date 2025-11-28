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
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ListId
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import kotlinx.datetime.Instant

@Entity(
    tableName = "lists",
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
data class ListEntity(
    val cid: ListId,
    @PrimaryKey
    val uri: ListUri,
    val creatorId: ProfileId,
    val name: String,
    val description: String?,
    val avatar: ImageUri?,
    val listItemCount: Long?,
    val purpose: String,
    val indexedAt: Instant,
    val createdAt: Instant,
) {
    data class Partial(
        val cid: Id,
        val uri: ListUri,
        val creatorId: ProfileId,
        val name: String,
        val avatar: ImageUri?,
        val listItemCount: Long?,
        val purpose: String,
    )
}

data class PopulatedListEntity(
    @Embedded
    val entity: ListEntity,
    @Relation(
        parentColumn = "creatorId",
        entityColumn = "did",
    )
    val creator: ProfileEntity,
    @Relation(
        parentColumn = "uri",
        entityColumn = "uri",
    )
    val labelEntities: List<LabelEntity>,
) : PopulatedRecordEntity {
    override val recordUri: RecordUri
        get() = entity.uri
}

fun ListEntity.partial() =
    ListEntity.Partial(
        cid = cid,
        uri = uri,
        creatorId = creatorId,
        name = name,
        avatar = avatar,
        listItemCount = listItemCount,
        purpose = purpose,
    )

fun ListEntity.asExternalModel(
    creator: Profile,
    labels: List<Label>,
): FeedList {
    check(creator.did == creatorId) {
        "passed in creator does not match creator Id"
    }
    return FeedList(
        cid = cid,
        uri = uri,
        creator = creator,
        name = name,
        description = description,
        avatar = avatar,
        listItemCount = listItemCount,
        purpose = purpose,
        indexedAt = indexedAt,
        labels = labels,
    )
}

fun PopulatedListEntity.asExternalModel(): FeedList =
    entity.asExternalModel(
        creator = creator.asExternalModel(),
        labels = labelEntities.map(LabelEntity::asExternalModel),
    )
