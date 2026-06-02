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

package com.tunjid.heron.data.database.entities.postembeds

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.PostEntity

/**
 * The Atmosphere records (eg. site.standard.document / site.standard.publication) that back a
 * post's [ExternalEmbedEntity] via `app.bsky.embed.external#associatedRefs`. These become entries
 * in the post's `embeddedRecords`.
 */
@Entity(
    tableName = "postExternalAssociatedRecords",
    primaryKeys = [
        "postUri",
        "externalEmbedUri",
        "recordUri",
    ],
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["uri"],
            childColumns = ["postUri"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExternalEmbedEntity::class,
            parentColumns = ["uri"],
            childColumns = ["externalEmbedUri"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["postUri"]),
        Index(value = ["externalEmbedUri"]),
        Index(value = ["recordUri"]),
    ],
)
data class PostExternalAssociatedRecordEntity(
    val postUri: PostUri,
    val externalEmbedUri: Uri,
    val recordUri: EmbeddableRecordUri,
    val recordCid: Id?,
)
