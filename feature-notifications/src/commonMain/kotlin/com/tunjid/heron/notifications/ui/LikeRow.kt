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
import com.tunjid.heron.ui.SharedElementScope
import com.tunjid.heron.ui.TimeDelta
import heron.feature_notifications.generated.resources.Res
import heron.feature_notifications.generated.resources.liked_your_post
import heron.feature_notifications.generated.resources.liked_your_post_description
import heron.feature_notifications.generated.resources.multiple_liked_your_post
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource

@Composable
fun LikeRow(
    sharedElementScope: SharedElementScope,
    now: Instant,
    notification: Notification.Liked,
    aggregatedProfiles: List<Profile>,
    onPostClicked: (Post) -> Unit,
    onProfileClicked: (Notification, Profile) -> Unit,
) {
    NotificationAggregateScaffold(
        sharedElementScope = sharedElementScope,
        modifier = Modifier.clickable {

        },
        onProfileClicked = onProfileClicked,
        notification = notification,
        profiles = aggregatedProfiles,
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
                            notification = notification,
                            aggregatedSize = aggregatedProfiles.size,
                            singularResource = Res.string.liked_your_post,
                            pluralResource = Res.string.multiple_liked_your_post,
                        ),
                    )

                    TimeDelta(
                        modifier = Modifier.alignByBaseline(),
                        delta = now - notification.indexedAt,
                    )
                }
                Text(
                    text = notification.associatedPost.record?.text ?: "",
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.outline),
                )
            }
        },
    )
}
