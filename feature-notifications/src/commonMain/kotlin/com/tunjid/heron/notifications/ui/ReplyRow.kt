package com.tunjid.heron.notifications.ui

import androidx.compose.runtime.Composable
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.ui.SharedElementScope
import kotlinx.datetime.Instant

@Composable
fun ReplyRow(
    sharedElementScope: SharedElementScope,
    now: Instant,
    notification: Notification.RepliedTo,
    onProfileClicked: (Notification, Profile) -> Unit,
) {
    NotificationPostScaffold(
        sharedElementScope = sharedElementScope,
        now = now,
        notification = notification,
        sharedElementPrefix = "",
        onProfileClicked = onProfileClicked,
        onPostClicked = {},
        onImageClicked = {},
        onReplyToPost = {}
    )
}
