package com.tunjid.heron.notifications.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.ui.TimeDelta
import heron.feature_notifications.generated.resources.Res
import heron.feature_notifications.generated.resources.liked_your_post
import heron.feature_notifications.generated.resources.liked_your_post_description
import heron.feature_notifications.generated.resources.multiple_liked_your_post
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource

@Composable
fun LikeRow(
    now: Instant,
    notifications: List<Notification.Liked>,
    onPostClicked: (Post) -> Unit,
    onProfileClicked: (Profile) -> Unit,

    ) {
    val firstProfile = notifications.first().author
    NotificationRowScaffold(
        modifier = Modifier.clickable {

        },
        onProfileClicked = onProfileClicked,
        profiles = notifications.map { it.author },
        icon = {
            Icon(
                painter = rememberVectorPainter(Icons.Rounded.Favorite),
                tint = Color.Red,
                contentDescription = stringResource(Res.string.liked_your_post_description),
            )
        },
        content = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = notificationText(
                            notifications,
                            Res.string.liked_your_post,
                            Res.string.multiple_liked_your_post,
                        ),
                    )

                    TimeDelta(
                        modifier = Modifier.alignByBaseline(),
                        delta = now - notifications.first().indexedAt,
                    )
                }
                Text(
                    text = notifications.first().associatedPost.record?.text ?: "",
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.outline),
                )
            }
        },
    )
}
