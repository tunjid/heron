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
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.types.Id
import kotlinx.datetime.Instant


@Entity(
    tableName = "timelineItems",
    indices = [
        Index(value = ["indexedAt"]),
    ],
)
data class TimelineItemEntity(
    val postId: Id,
    val sourceId: String,
    @Embedded
    val reply: FeedReplyEntity?,
    val reposter: Id?,
    val isPinned: Boolean,
    val indexedAt: Instant,
    // TODO: Figure out a better ID for this
    @PrimaryKey
    val id: String = "${postId.id}-${reposter?.id}"
)

data class FeedReplyEntity(
    val rootPostId: Id,
    val parentPostId: Id,
)