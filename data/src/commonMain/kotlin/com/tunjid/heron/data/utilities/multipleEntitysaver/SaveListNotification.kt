package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.feed.PostView
import app.bsky.notification.ListNotificationsNotification
import app.bsky.notification.ListNotificationsReason
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.NotificationEntity
import sh.christian.ozone.api.AtUri


internal fun MultipleEntitySaver.add(
    viewingProfileId: Id,
    listNotificationsNotification: List<ListNotificationsNotification>,
    associatedPosts: List<PostView>,
) {

    val postUrisToPostIds = mutableMapOf<Uri, Id>()
    associatedPosts.forEach {
        add(
            viewingProfileId = viewingProfileId,
            postView = it,
        )
        postUrisToPostIds[it.uri.atUri.let(::Uri)] = it.cid.cid.let(::Id)
    }

    listNotificationsNotification.forEach { notification ->
        add(
            viewingProfileId = viewingProfileId,
            profileView = notification.author
        )
        add(
            NotificationEntity(
                uri = notification.uri.atUri.let(::Uri),
                cid = notification.cid.cid.let(::Id),
                authorId = notification.author.did.did.let(::Id),
                reason = when(notification.reason) {
                    ListNotificationsReason.Follow -> Notification.Reason.Follow
                    ListNotificationsReason.Like -> Notification.Reason.Like
                    ListNotificationsReason.Mention -> Notification.Reason.Mention
                    ListNotificationsReason.Quote -> Notification.Reason.Quote
                    ListNotificationsReason.Reply -> Notification.Reason.Reply
                    ListNotificationsReason.Repost -> Notification.Reason.Repost
                    ListNotificationsReason.StarterpackJoined -> Notification.Reason.JoinedStarterPack
                    is ListNotificationsReason.Unknown -> Notification.Reason.Unknown
                },
                reasonSubject = notification.reasonSubject?.atUri?.let(::Uri),
                associatedPostId = notification.associatedPostUri()
                    ?.atUri
                    ?.let(::Uri)
                    ?.let(postUrisToPostIds::get),
                isRead = notification.isRead,
                indexedAt = notification.indexedAt,
            )
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
}