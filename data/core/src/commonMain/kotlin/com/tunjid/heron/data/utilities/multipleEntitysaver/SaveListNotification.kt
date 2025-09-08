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
import app.bsky.notification.ListNotificationsNotificationReason
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
                reason = when (notification.reason) {
                    ListNotificationsNotificationReason.Follow -> Notification.Reason.Follow
                    ListNotificationsNotificationReason.Like -> Notification.Reason.Like
                    ListNotificationsNotificationReason.Mention -> Notification.Reason.Mention
                    ListNotificationsNotificationReason.Quote -> Notification.Reason.Quote
                    ListNotificationsNotificationReason.Reply -> Notification.Reason.Reply
                    ListNotificationsNotificationReason.Repost -> Notification.Reason.Repost
                    ListNotificationsNotificationReason.StarterpackJoined -> Notification.Reason.JoinedStarterPack
                    is ListNotificationsNotificationReason.Unknown -> Notification.Reason.Unknown
                    ListNotificationsNotificationReason.Verified -> Notification.Reason.Verified
                    ListNotificationsNotificationReason.Unverified -> Notification.Reason.Unverified
                    ListNotificationsNotificationReason.LikeViaRepost -> Notification.Reason.LikedRepost
                    ListNotificationsNotificationReason.RepostViaRepost -> Notification.Reason.RepostedRepost
                    // TODO: Treat as unknown for now
                    ListNotificationsNotificationReason.SubscribedPost -> Notification.Reason.Unknown
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
    is ListNotificationsNotificationReason.Unknown -> null
    is ListNotificationsNotificationReason.Like -> reasonSubject
    is ListNotificationsNotificationReason.Repost -> reasonSubject
    is ListNotificationsNotificationReason.Mention -> uri
    is ListNotificationsNotificationReason.Reply -> uri
    is ListNotificationsNotificationReason.Quote -> uri
    is ListNotificationsNotificationReason.Follow -> null
    is ListNotificationsNotificationReason.StarterpackJoined -> null
    ListNotificationsNotificationReason.Unverified -> null
    ListNotificationsNotificationReason.Verified -> null
    ListNotificationsNotificationReason.LikeViaRepost -> reasonSubject
    ListNotificationsNotificationReason.RepostViaRepost -> reasonSubject
    ListNotificationsNotificationReason.SubscribedPost -> null
}
