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
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import kotlinx.datetime.Instant


@Entity(
    tableName = "timelineItems",
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["uri"],
            childColumns = ["postUri"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["uri"],
            childColumns = ["reply.rootPostUri"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["uri"],
            childColumns = ["reply.parentPostUri"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["reply.grandParentPostAuthorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["postUri"]),
        Index(value = ["indexedAt"]),
        Index(value = ["viewingProfileId"]),
        Index(value = ["sourceId"]),
        Index(value = ["reply.rootPostUri"]),
        Index(value = ["reply.parentPostUri"]),
        Index(value = ["reply.grandParentPostAuthorId"]),
    ],
)
data class TimelineItemEntity(
    val postUri: PostUri,
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
    val id: String = "${viewingProfileId?.id}-$sourceId-${postUri.uri}-${reposter?.id}",
)

data class FeedReplyEntity(
    val rootPostUri: PostUri,
    val parentPostUri: PostUri,
    val grandParentPostAuthorId: ProfileId?,
)
