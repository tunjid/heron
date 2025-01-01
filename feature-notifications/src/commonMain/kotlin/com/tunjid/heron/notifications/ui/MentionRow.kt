package com.tunjid.heron.notifications.ui

import androidx.compose.runtime.Composable
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Profile
import kotlinx.datetime.Instant

@Composable
fun MentionRow(
    now: Instant,
    notification: Notification.Mentioned,
    onProfileClicked: (Profile) -> Unit,
) {

}
