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
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.StandardDocumentId
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.core.types.StandardPublicationUri
import kotlin.time.Instant

@Entity(
    tableName = "standardDocuments",
    foreignKeys = [
        ForeignKey(
            entity = StandardPublicationEntity::class,
            parentColumns = ["uri"],
            childColumns = ["publicationUri"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["uri"]),
        Index(value = ["publicationUri"]),
        Index(value = ["publishedAt"]),
    ],
)
data class StandardDocumentEntity(
    @PrimaryKey
    val uri: StandardDocumentUri,
    val cid: StandardDocumentId?,
    val title: String,
    val description: String?,
    val textContent: String?,
    val path: String?,
    val site: String,
    val publishedAt: Instant,
    val updatedAt: Instant?,
    val coverImage: ImageUri?,
    val bskyPostRefUri: PostUri?,
    val bskyPostRefCid: PostId?,
    val tags: String?,
    val publicationUri: StandardPublicationUri?,
)

data class PopulatedStandardDocumentEntity(
    @Embedded
    val entity: StandardDocumentEntity,
    @Relation(
        parentColumn = "publicationUri",
        entityColumn = "uri",
    )
    val publication: StandardPublicationEntity?,
)

fun PopulatedStandardDocumentEntity.asExternalModel() = StandardDocument(
    uri = entity.uri,
    cid = entity.cid,
    title = entity.title,
    description = entity.description,
    textContent = entity.textContent,
    path = entity.path,
    site = entity.site,
    publishedAt = entity.publishedAt,
    updatedAt = entity.updatedAt,
    coverImage = entity.coverImage,
    bskyPostRef = entity.bskyPostRefUri?.let { refUri ->
        Record.Reference(
            id = entity.bskyPostRefCid,
            uri = refUri,
        )
    },
    tags = entity.tags?.deserializeTags() ?: emptyList(),
    publication = publication?.asExternalModel(),
)

private fun String.deserializeTags(): List<String> =
    split("\n").filter(String::isNotEmpty)

private fun List<String>.serializeTags(): String =
    joinToString(",")
