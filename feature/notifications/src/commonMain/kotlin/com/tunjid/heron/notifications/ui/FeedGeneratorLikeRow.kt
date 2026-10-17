/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.tunjid.heron.notifications.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.timeline.ui.TimeDelta
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.UiTokens.LikeRed
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.regular.Favorite
import com.tunjid.heron.ui.modifiers.rootShapedClickable
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.notifications_liked_your_feed
import heron.ui.core.generated.resources.notifications_liked_your_feed_description
import heron.ui.core.generated.resources.notifications_multiple_liked_your_feed
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

@Composable
fun FeedGeneratorLikeRow(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    now: Instant,
    isRead: Boolean,
    notification: Notification.LikedFeedGenerator,
    aggregatedProfiles: List<Profile>,
    onProfileClicked: (Notification, Profile) -> Unit,
    onFeedGeneratorClicked: (FeedGenerator) -> Unit,
) {
    NotificationAggregateScaffold(
        paneTransitionScope = paneTransitionScope,
        modifier = modifier
            .rootShapedClickable {
                onFeedGeneratorClicked(notification.associatedFeedGenerator)
            },
        isRead = isRead,
        notification = notification,
        profiles = aggregatedProfiles,
        onProfileClicked = onProfileClicked,
        icon = {
            Icon(
                painter = rememberVectorPainter(HeronIcons.Regular.Favorite),
                tint = LikeRed,
                contentDescription = stringResource(CommonStrings.notifications_liked_your_feed_description),
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
                            singularResource = CommonStrings.notifications_liked_your_feed,
                            pluralResource = CommonStrings.notifications_multiple_liked_your_feed,
                        ),
                    )

                    TimeDelta(
                        modifier = Modifier.alignByBaseline(),
                        delta = now - notification.indexedAt,
                    )
                }
                Text(
                    text = notification.associatedFeedGenerator.displayName,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.outline),
                )
            }
        },
    )
}
