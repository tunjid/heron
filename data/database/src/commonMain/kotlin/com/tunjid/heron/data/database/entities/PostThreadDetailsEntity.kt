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
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.PostUri

@Entity(
    tableName = "postThreadDetails",
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["uri"],
            childColumns = ["postUri"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
)
data class PostThreadDetailsEntity(
    @PrimaryKey
    val postUri: PostUri,
    val postIndex: Long,
    val postCount: Long,
)

fun PostThreadDetailsEntity.asExternalModel() = Post.ThreadDetails(
    postIndex = postIndex,
    postCount = postCount,
)

fun postThreadDetailsEntityOrNull(
    postUri: PostUri,
    postIndex: Long?,
    postCount: Long?,
): PostThreadDetailsEntity? =
    if (postIndex == null || postCount == null) null
    else PostThreadDetailsEntity(
        postUri = postUri,
        postIndex = postIndex,
        postCount = postCount,
    )
