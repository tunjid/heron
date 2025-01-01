package com.tunjid.heron.notifications.ui

import androidx.compose.runtime.Composable
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Profile
import kotlinx.datetime.Instant

@Composable
fun QuoteRow(
    now: Instant,
    notification: Notification.Quoted,
    onProfileClicked: (Profile) -> Unit,
) {

}
