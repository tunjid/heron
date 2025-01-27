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
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
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
        Index(value = ["indexedAt"]),
    ],
)
data class FeedGeneratorEntity(
    @PrimaryKey
    val cid: Id,
    val did: Id,
    val uri: Uri,
    val avatar: Uri?,
    val likeCount: Long?,
    val creatorId: Id,
    val displayName: String,
    val description: String?,
    val acceptsInteractions: Boolean?,
    val indexedAt: Instant,
)

fun FeedGeneratorEntity.asExternalModel() =
    FeedGenerator(
        cid = cid,
        did = did,
        uri = uri,
        avatar = avatar,
        likeCount = likeCount,
        creatorId = creatorId,
        displayName = displayName,
        description = description,
        acceptsInteractions = acceptsInteractions,
        indexedAt = indexedAt,
    )

