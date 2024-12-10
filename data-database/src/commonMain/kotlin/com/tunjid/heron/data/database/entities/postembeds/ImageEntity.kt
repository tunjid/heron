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

package com.tunjid.heron.data.database.entities.postembeds

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.models.Image
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.PostEntity

@Entity(
    tableName = "images",
)
data class ImageEntity(
    @PrimaryKey
    val fullSize: Uri,
    val thumb: Uri,
    val alt: String,
    val width: Long?,
    val height: Long?,
): PostEmbed


/**
 * Cross reference for many to many relationship between [Post] and [ImageEntity]
 */
@Entity(
    tableName = "postImages",
    primaryKeys = ["postId", "imageUri"],
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["cid"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = ["fullSize"],
            childColumns = ["imageUri"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["postId"]),
        Index(value = ["imageUri"]),
    ],
)
data class PostImageEntity(
    val postId: Id,
    val imageUri: Uri,
)


fun ImageEntity.asExternalModel() = Image(
    fullsize = fullSize,
    thumb = thumb,
    alt = alt,
    width = width,
    height = height,
)
