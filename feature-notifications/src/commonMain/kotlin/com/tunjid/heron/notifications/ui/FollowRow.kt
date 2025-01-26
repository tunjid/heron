package com.tunjid.heron.notifications.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.heron.ui.TimeDelta
import heron.feature_notifications.generated.resources.Res
import heron.feature_notifications.generated.resources.followed_you
import heron.feature_notifications.generated.resources.followed_you_description
import heron.feature_notifications.generated.resources.multiple_followed
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource

@Composable
fun FollowRow(
    modifier: Modifier = Modifier,
    panedSharedElementScope: PanedSharedElementScope,
    now: Instant,
    notification: Notification.Followed,
    aggregatedProfiles: List<Profile>,
    onProfileClicked: (Notification, Profile) -> Unit,
) {
    NotificationAggregateScaffold(
        panedSharedElementScope = panedSharedElementScope,
        modifier = modifier
            .clickable {
                onProfileClicked(notification, notification.author)
            },
        onProfileClicked = onProfileClicked,
        notification = notification,
        profiles = aggregatedProfiles,
        icon = {
            Icon(
                painter = rememberVectorPainter(Icons.Default.Person),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = stringResource(Res.string.followed_you_description),
            )
        },
        content = {
            Row(horizontalArrangement = spacedBy(8.dp)) {
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = notificationText(
                        notification = notification,
                        aggregatedSize = aggregatedProfiles.size,
                        singularResource = Res.string.followed_you,
                        pluralResource = Res.string.multiple_followed,
                    ),
                )
                TimeDelta(
                    modifier = Modifier.alignByBaseline(),
                    delta = now - notification.indexedAt,
                )
            }
        },
    )
}
