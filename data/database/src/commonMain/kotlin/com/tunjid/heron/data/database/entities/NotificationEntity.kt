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

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import kotlinx.datetime.Instant

@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["uri"],
            childColumns = ["associatedPostUri"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["uri"]),
        Index(value = ["cid"]),
        Index(value = ["authorId"]),
        Index(value = ["indexedAt"]),
    ],
)
data class NotificationEntity(
    val cid: GenericId,
    @PrimaryKey
    val uri: GenericUri,
    val authorId: ProfileId,
    val reason: Notification.Reason,
    val reasonSubject: GenericUri?,
    val associatedPostUri: PostUri?,
    val isRead: Boolean,
    val indexedAt: Instant,
)

data class PopulatedNotificationEntity(
    @Embedded
    val entity: NotificationEntity,
    @Relation(
        parentColumn = "authorId",
        entityColumn = "did",
    )
    val author: ProfileEntity,
)

fun PopulatedNotificationEntity.asExternalModel(
    associatedPost: Post?,
) = when (entity.reason) {
    Notification.Reason.Unknown -> unknown()

    Notification.Reason.Like ->
        if (associatedPost == null) unknown()
        else Notification.Liked(
            cid = entity.cid,
            uri = entity.uri,
            indexedAt = entity.indexedAt,
            author = author.asExternalModel(),
            reasonSubject = entity.uri,
            isRead = entity.isRead,
            associatedPost = associatedPost,
        )

    Notification.Reason.Repost ->
        if (associatedPost == null) unknown()
        else Notification.Reposted(
            cid = entity.cid,
            uri = entity.uri,
            indexedAt = entity.indexedAt,
            author = author.asExternalModel(),
            reasonSubject = entity.uri,
            isRead = entity.isRead,
            associatedPost = associatedPost,
        )

    Notification.Reason.Follow -> Notification.Followed(
        cid = entity.cid,
        uri = entity.uri,
        indexedAt = entity.indexedAt,
        author = author.asExternalModel(),
        reasonSubject = entity.uri,
        isRead = entity.isRead,
    )

    Notification.Reason.Mention ->
        if (associatedPost == null) unknown()
        else Notification.Mentioned(
            cid = entity.cid,
            uri = entity.uri,
            indexedAt = entity.indexedAt,
            author = author.asExternalModel(),
            reasonSubject = entity.uri,
            isRead = entity.isRead,
            associatedPost = associatedPost,
        )

    Notification.Reason.Reply ->
        if (associatedPost == null) unknown()
        else Notification.RepliedTo(
            cid = entity.cid,
            uri = entity.uri,
            indexedAt = entity.indexedAt,
            author = author.asExternalModel(),
            reasonSubject = entity.uri,
            isRead = entity.isRead,
            associatedPost = associatedPost,
        )

    Notification.Reason.Quote ->
        if (associatedPost == null) unknown()
        else Notification.Quoted(
            cid = entity.cid,
            uri = entity.uri,
            indexedAt = entity.indexedAt,
            author = author.asExternalModel(),
            reasonSubject = entity.uri,
            isRead = entity.isRead,
            associatedPost = associatedPost,
        )

    Notification.Reason.JoinedStarterPack -> Notification.JoinedStarterPack(
        cid = entity.cid,
        uri = entity.uri,
        indexedAt = entity.indexedAt,
        author = author.asExternalModel(),
        reasonSubject = entity.uri,
        isRead = entity.isRead,
    )

    Notification.Reason.Verified -> Notification.Verified(
        cid = entity.cid,
        uri = entity.uri,
        indexedAt = entity.indexedAt,
        author = author.asExternalModel(),
        reasonSubject = entity.uri,
        isRead = entity.isRead,
    )

    Notification.Reason.Unverified -> Notification.Unverified(
        cid = entity.cid,
        uri = entity.uri,
        indexedAt = entity.indexedAt,
        author = author.asExternalModel(),
        reasonSubject = entity.uri,
        isRead = entity.isRead,
    )
}

private fun PopulatedNotificationEntity.unknown() =
    Notification.Unknown(
        cid = entity.cid,
        uri = entity.uri,
        indexedAt = entity.indexedAt,
        author = author.asExternalModel(),
        reasonSubject = entity.uri,
        isRead = entity.isRead,
    )
