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
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import kotlinx.datetime.Instant


@Entity(
    tableName = "starterPacks",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["creatorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["indexedAt"]),
    ],
)
data class StarterPackEntity(
    @PrimaryKey
    val cid: Id,
    val uri: Uri,
    val creatorId: Id,
    val listUri: Uri?,
    val name: String,
    val joinedWeekCount: Long?,
    val joinedAllTimeCount: Long?,
    val indexedAt: Instant,
    val createdAt: Instant,
)


data class PopulatedStarterPackEntity(
    @Embedded
    val entity: StarterPackEntity,
    @Relation(
        parentColumn = "creatorId",
        entityColumn = "did"
    )
    val creator: ProfileEntity?,
    @Relation(
        parentColumn = "listUri",
        entityColumn = "uri",
    )
    val list: ListEntity?,
)

fun PopulatedStarterPackEntity.asExternalModel() =
    StarterPack(
        cid = entity.cid,
        uri = entity.uri,
        name = entity.name,
        creator = creator.asExternalModel(),
        list = list?.asExternalModel(),
        joinedWeekCount = entity.joinedWeekCount,
        joinedAllTimeCount = entity.joinedAllTimeCount,
        indexedAt = entity.indexedAt,
    )

