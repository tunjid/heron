package com.tunjid.heron.notifications.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.ui.PanedSharedElementScope
import kotlinx.datetime.Instant

@Composable
fun ReplyRow(
    modifier: Modifier = Modifier,
    panedSharedElementScope: PanedSharedElementScope,
    now: Instant,
    notification: Notification.RepliedTo,
    onProfileClicked: (Notification.PostAssociated, Profile) -> Unit,
    onPostClicked: (Notification.PostAssociated) -> Unit,
    onPostInteraction: (Post.Interaction) -> Unit,
) {
    NotificationPostScaffold(
        modifier = modifier,
        panedSharedElementScope = panedSharedElementScope,
        now = now,
        notification = notification,
        onProfileClicked = onProfileClicked,
        onPostClicked = onPostClicked,
        onPostMediaClicked = { _, _, _ -> },
        onReplyToPost = {},
        onPostInteraction = onPostInteraction,
    )
}
