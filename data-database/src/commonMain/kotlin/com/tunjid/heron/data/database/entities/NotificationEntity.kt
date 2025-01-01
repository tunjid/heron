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
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import kotlinx.datetime.Instant

@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["cid"],
            childColumns = ["associatedPostId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class NotificationEntity(
    @PrimaryKey
    val cid: Id,
    val uri: Uri,
    val authorId: Id,
    val reason: Notification.Reason,
    val reasonSubject: Uri?,
    val associatedPostId: Id?,
    val isRead: Boolean,
    val indexedAt: Instant,
)

data class PopulatedNotificationEntity(
    @Embedded
    val entity: NotificationEntity,
    @Relation(
        parentColumn = "authorId",
        entityColumn = "did"
    )
    val author: ProfileEntity,
)

fun PopulatedNotificationEntity.asExternalModel(
    associatedPost: Post?,
) = Notification(
    cid = entity.cid,
    uri = entity.uri,
    indexedAt = entity.indexedAt,
    author = author.asExternalModel(),
    reason = entity.reason,
    reasonSubject = entity.uri,
    content = associatedPost?.let {
        when (entity.reason) {
            Notification.Reason.Unknown -> null
            Notification.Reason.Like -> Notification.Content.Liked(
                post = it
            )

            Notification.Reason.Repost -> Notification.Content.Reposted(
                post = it
            )

            Notification.Reason.Follow -> Notification.Content.Followed
            Notification.Reason.Mention -> Notification.Content.Mentioned(
                post = it
            )

            Notification.Reason.Reply -> Notification.Content.RepliedTo(
                post = it
            )

            Notification.Reason.Quote -> Notification.Content.Quoted(
                post = it
            )

            Notification.Reason.JoinedStarterPack -> Notification.Content.JoinedStarterPack
        }
    },
    isRead = entity.isRead,
)
