/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri

sealed interface EmbedEntity
sealed interface EmbedEntityCrossRef

/**
 * Cross reference for many to many relationship between [Post] and [VideoEntity]
 */
@Entity(
    tableName = "posts_videos",
    primaryKeys = ["post_id", "video_id"],
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["cid"],
            childColumns = ["post_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["cid"],
            childColumns = ["video_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["post_id"]),
        Index(value = ["video_id"]),
    ],
)
data class PostVideoCrossRef(
    @ColumnInfo(name = "post_id")
    val postId: Id,
    @ColumnInfo(name = "video_id")
    val videoId: Id,
): EmbedEntityCrossRef

/**
 * Cross reference for many to many relationship between [Post] and [ImageEntity]
 */
@Entity(
    tableName = "posts_images",
    primaryKeys = ["post_id", "image_uri"],
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["cid"],
            childColumns = ["post_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = ["full_size"],
            childColumns = ["image_uri"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["post_id"]),
        Index(value = ["image_uri"]),
    ],
)
data class PostImageCrossRef(
    @ColumnInfo(name = "post_id")
    val postId: Id,
    @ColumnInfo(name = "image_uri")
    val imageUri: Uri,
): EmbedEntityCrossRef

/**
 * Cross reference for many to many relationship between [Post] and [ExternalEmbedEntity]
 */
@Entity(
    tableName = "posts_external_embeds",
    primaryKeys = ["post_id", "external_embed_uri"],
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["cid"],
            childColumns = ["post_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExternalEmbedEntity::class,
            parentColumns = ["uri"],
            childColumns = ["external_embed_uri"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["post_id"]),
        Index(value = ["external_embed_uri"]),
    ],
)
data class PostExternalEmbedCrossRef(
    @ColumnInfo(name = "post_id")
    val postId: Id,
    @ColumnInfo(name = "external_embed_uri")
    val externalEmbedUri: Uri,
): EmbedEntityCrossRef
