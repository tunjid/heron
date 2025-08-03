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

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.ProfileId
import kotlinx.datetime.Instant


@Entity(
    tableName = "timelineItems",
    indices = [
        Index(value = ["indexedAt"]),
        Index(value = ["viewingProfileId"]),
        Index(value = ["sourceId"]),
    ],
)
data class TimelineItemEntity(
    val postId: PostId,
    val viewingProfileId: ProfileId?,
    val sourceId: String,
    @Embedded
    val reply: FeedReplyEntity?,
    val reposter: ProfileId?,
    @ColumnInfo(
        defaultValue = "false",
    )
    val hasMedia: Boolean,
    val isPinned: Boolean,
    val indexedAt: Instant,
    // Timeline items are unique to the profile viewing them, and these other fields
    @PrimaryKey
    val id: String = "${viewingProfileId?.id}-$sourceId-${postId.id}-${reposter?.id}",
)

data class FeedReplyEntity(
    val rootPostId: PostId,
    val parentPostId: PostId,
    val grandParentPostAuthorId: PostId?,
)
