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

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
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
        Index(value = ["indexedAt"]),
    ],
)
data class ListEntity(
    @PrimaryKey
    val cid: Id,
    val uri: Uri,
    val creatorId: Id,
    val name: String,
    val description: String?,
    val avatar: Uri?,
    val listItemCount: Long?,
    val purpose: String,
    val indexedAt: Instant,
)

fun ListEntity.asExternalModel() =
    FeedList(
        cid = cid,
        uri = uri,
        creatorId = creatorId,
        name = name,
        description = description,
        avatar = avatar,
        listItemCount = listItemCount,
        purpose = purpose,
        indexedAt = indexedAt,
    )

