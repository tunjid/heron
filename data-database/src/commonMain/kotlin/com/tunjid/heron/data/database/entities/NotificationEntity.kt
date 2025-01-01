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
) = if (associatedPost != null) when (entity.reason) {
    Notification.Reason.Unknown -> Notification.Unknown(
        cid = entity.cid,
        uri = entity.uri,
        indexedAt = entity.indexedAt,
        author = author.asExternalModel(),
        reasonSubject = entity.uri,
        isRead = entity.isRead,
    )

    Notification.Reason.Like -> Notification.Liked(
        cid = entity.cid,
        uri = entity.uri,
        indexedAt = entity.indexedAt,
        author = author.asExternalModel(),
        reasonSubject = entity.uri,
        isRead = entity.isRead,
        associatedPost = associatedPost
    )

    Notification.Reason.Repost -> Notification.Reposted(
        cid = entity.cid,
        uri = entity.uri,
        indexedAt = entity.indexedAt,
        author = author.asExternalModel(),
        reasonSubject = entity.uri,
        isRead = entity.isRead,
        associatedPost = associatedPost
    )

    Notification.Reason.Follow -> Notification.Followed(
        cid = entity.cid,
        uri = entity.uri,
        indexedAt = entity.indexedAt,
        author = author.asExternalModel(),
        reasonSubject = entity.uri,
        isRead = entity.isRead,
    )

    Notification.Reason.Mention -> Notification.Mentioned(
        cid = entity.cid,
        uri = entity.uri,
        indexedAt = entity.indexedAt,
        author = author.asExternalModel(),
        reasonSubject = entity.uri,
        isRead = entity.isRead,
        associatedPost = associatedPost
    )

    Notification.Reason.Reply -> Notification.RepliedTo(
        cid = entity.cid,
        uri = entity.uri,
        indexedAt = entity.indexedAt,
        author = author.asExternalModel(),
        reasonSubject = entity.uri,
        isRead = entity.isRead,
        associatedPost = associatedPost
    )

    Notification.Reason.Quote -> Notification.Quoted(
        cid = entity.cid,
        uri = entity.uri,
        indexedAt = entity.indexedAt,
        author = author.asExternalModel(),
        reasonSubject = entity.uri,
        isRead = entity.isRead,
        associatedPost = associatedPost
    )

    Notification.Reason.JoinedStarterPack -> Notification.JoinedStarterPack(
        cid = entity.cid,
        uri = entity.uri,
        indexedAt = entity.indexedAt,
        author = author.asExternalModel(),
        reasonSubject = entity.uri,
        isRead = entity.isRead,
    )
} else Notification.Unknown(
    cid = entity.cid,
    uri = entity.uri,
    indexedAt = entity.indexedAt,
    author = author.asExternalModel(),
    reasonSubject = entity.uri,
    isRead = entity.isRead,
)
