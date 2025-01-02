package com.tunjid.heron.notifications.ui

import androidx.compose.runtime.Composable
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.ui.SharedElementScope
import kotlinx.datetime.Instant

@Composable
fun QuoteRow(
    sharedElementScope: SharedElementScope,
    now: Instant,
    notification: Notification.Quoted,
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
