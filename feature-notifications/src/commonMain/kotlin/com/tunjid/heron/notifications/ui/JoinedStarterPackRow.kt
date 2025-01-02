package com.tunjid.heron.notifications.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.ui.TimeDelta
import heron.feature_notifications.generated.resources.Res
import heron.feature_notifications.generated.resources.joined_from_your_started_pack_description
import heron.feature_notifications.generated.resources.joined_from_your_starter_pack
import heron.feature_notifications.generated.resources.multiple_joined_from_your_starter_pack
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource


@Composable
fun JoinedStarterPackRow(
    now: Instant,
    notification: Notification.JoinedStarterPack,
    aggregatedProfiles: List<Profile>,
    onProfileClicked: (Notification, Profile) -> Unit,
) {
    NotificationAggregateScaffold(
        modifier = Modifier
            .clickable {
                onProfileClicked(notification, notification.author)
            },
        notification = notification,
        onProfileClicked = onProfileClicked,
        profiles = aggregatedProfiles,
        icon = {
            Icon(
                painter = rememberVectorPainter(Icons.Rounded.Person),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = stringResource(Res.string.joined_from_your_started_pack_description),
            )
        },
        content = {
            Row(horizontalArrangement = spacedBy(8.dp)) {
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = notificationText(
                        notification = notification,
                        aggregatedSize = aggregatedProfiles.size,
                        singularResource = Res.string.joined_from_your_starter_pack,
                        pluralResource = Res.string.multiple_joined_from_your_starter_pack,
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
