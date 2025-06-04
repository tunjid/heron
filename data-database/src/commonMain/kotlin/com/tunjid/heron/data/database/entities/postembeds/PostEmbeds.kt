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
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.database.entities.PostEntity

sealed interface PostEmbed

/**
 * Cross reference for many to many relationship between [Post] and another [Post].
 */
@Entity(
    tableName = "postPosts",
    primaryKeys = ["postId", "embeddedPostId"],
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["cid"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["cid"],
            childColumns = ["embeddedPostId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["postId"]),
        Index(value = ["embeddedPostId"]),
    ],
)
data class PostPostEntity(
    val postId: PostId,
    val embeddedPostId: PostId,
)