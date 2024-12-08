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

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import kotlinx.datetime.Instant


@Entity(
    tableName = "feedItems",
)
data class FeedItemEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    val postId: Id,
    val source: Uri?,
    @Embedded
    val reply: FeedReplyEntity?,
    val reposter: Id?,
    val isPinned: Boolean,
    val indexedAt: Instant,
)

data class FeedReplyEntity(
    val rootPostId: Id,
    val parentPostId: Id,
)
