package com.tunjid.heron.notifications.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.images.shapes.ImageShape
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun NotificationAggregateScaffold(
    modifier: Modifier = Modifier,
    notification: Notification,
    profiles: List<Profile>,
    onProfileClicked: (Notification, Profile) -> Unit,
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.TopEnd,
        ) {
            icon()
        }

        Column(verticalArrangement = spacedBy(8.dp)) {
            Row {
                profiles.forEach { profile ->
                    AsyncImage(
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onProfileClicked(notification, profile) },
                        args = remember {
                            ImageArgs(
                                url = profile.avatar?.uri,
                                contentScale = ContentScale.Crop,
                                contentDescription = profile.displayName ?: profile.handle.id,
                                shape = ImageShape.Circle,
                            )
                        }
                    )
                }
            }
            Box {
                content()
            }
        }
    }
}

@Composable
internal fun notificationText(
    notification: Notification,
    aggregatedSize: Int,
    singularResource: StringResource,
    pluralResource: StringResource,
): String {
    val author = notification.author
    val profileText = author.displayName ?: "@${author.handle}"
    return if (aggregatedSize == 0) stringResource(singularResource, profileText)
    else stringResource(pluralResource, profileText, aggregatedSize)
}