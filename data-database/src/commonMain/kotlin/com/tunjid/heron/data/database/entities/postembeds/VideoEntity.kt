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
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.database.entities.PostEntity


@Entity(
    tableName = "videos",
)
data class VideoEntity(
    @PrimaryKey
    val cid: GenericId,
    val playlist: GenericUri,
    val thumbnail: ImageUri?,
    val alt: String?,
    val width: Long?,
    val height: Long?,
) : PostEmbed

/**
 * Cross reference for many to many relationship between [Post] and [VideoEntity]
 */
@Entity(
    tableName = "postVideos",
    primaryKeys = ["postId", "videoId"],
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["cid"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["cid"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["postId"]),
        Index(value = ["videoId"]),
    ],
)
data class PostVideoEntity(
    val postId: PostId,
    val videoId: GenericId,
)

fun VideoEntity.asExternalModel() = Video(
    cid = cid,
    playlist = playlist,
    thumbnail = thumbnail,
    alt = alt,
    width = width,
    height = height,
)
