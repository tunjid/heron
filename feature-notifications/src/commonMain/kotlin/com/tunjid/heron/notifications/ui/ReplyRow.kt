package com.tunjid.heron.notifications.ui

import androidx.compose.runtime.Composable
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Profile
import kotlinx.datetime.Instant

@Composable
fun ReplyRow(
    now: Instant,
    notification: Notification.RepliedTo,
    onProfileClicked: (Profile) -> Unit,
) {

}
