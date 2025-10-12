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

package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.feed.PostView
import app.bsky.notification.ListNotificationsNotification
import app.bsky.notification.ListNotificationsReason
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.NotificationEntity
import sh.christian.ozone.api.AtUri

internal fun MultipleEntitySaver.add(
    viewingProfileId: ProfileId?,
    listNotificationsNotification: List<ListNotificationsNotification>,
    associatedPosts: List<PostView>,
) {
    viewingProfileId ?: return
    val postUris = mutableSetOf<PostUri>()
    associatedPosts.forEach { postView ->
        add(
            viewingProfileId = viewingProfileId,
            postView = postView,
        )
        postUris.add(postView.uri.atUri.let(::PostUri))
    }

    listNotificationsNotification.forEach { notification ->
        add(
            viewingProfileId = viewingProfileId,
            profileView = notification.author,
        )
        add(
            NotificationEntity(
                uri = notification.uri.atUri.let(::GenericUri),
                cid = notification.cid.cid.let(::GenericId),
                authorId = notification.author.did.did.let(::ProfileId),
                ownerId = viewingProfileId,
                reason = when (notification.reason) {
                    ListNotificationsReason.Follow -> Notification.Reason.Follow
                    ListNotificationsReason.Like -> Notification.Reason.Like
                    ListNotificationsReason.Mention -> Notification.Reason.Mention
                    ListNotificationsReason.Quote -> Notification.Reason.Quote
                    ListNotificationsReason.Reply -> Notification.Reason.Reply
                    ListNotificationsReason.Repost -> Notification.Reason.Repost
                    ListNotificationsReason.StarterpackJoined -> Notification.Reason.JoinedStarterPack
                    is ListNotificationsReason.Unknown -> Notification.Reason.Unknown
                    ListNotificationsReason.Verified -> Notification.Reason.Verified
                    ListNotificationsReason.Unverified -> Notification.Reason.Unverified
                },
                reasonSubject = notification.reasonSubject?.atUri?.let(::GenericUri),
                associatedPostUri = notification.associatedPostUri()
                    ?.atUri
                    ?.let(::PostUri)
                    ?.takeIf(postUris::contains),
                isRead = notification.isRead,
                indexedAt = notification.indexedAt,
            ),
        )
    }
}

internal fun ListNotificationsNotification.associatedPostUri(): AtUri? = when (reason) {
    is ListNotificationsReason.Unknown -> null
    is ListNotificationsReason.Like -> reasonSubject
    is ListNotificationsReason.Repost -> reasonSubject
    is ListNotificationsReason.Mention -> uri
    is ListNotificationsReason.Reply -> uri
    is ListNotificationsReason.Quote -> uri
    is ListNotificationsReason.Follow -> null
    is ListNotificationsReason.StarterpackJoined -> null
    ListNotificationsReason.Unverified -> null
    ListNotificationsReason.Verified -> null
}
