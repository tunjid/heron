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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FiberNew
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.timeline.ui.TimeDelta
import com.tunjid.heron.ui.UiTokens.BookmarkBlue
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.ui.core.generated.resources.notifications_multiple_post_subscription
import heron.ui.core.generated.resources.notifications_post_subscription
import heron.ui.core.generated.resources.notifications_post_subscription_description
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

@Composable
fun SubscribedRow(
    modifier: Modifier = Modifier,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    now: Instant,
    isRead: Boolean,
    notification: Notification.SubscribedPost,
    aggregatedProfiles: List<Profile>,
    onProfileClicked: (Notification, Profile) -> Unit,
    onPostClicked: (Notification.PostAssociated) -> Unit,
) {
    NotificationAggregateScaffold(
        paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
        modifier = modifier.clickable { onPostClicked(notification) },
        isRead = isRead,
        notification = notification,
        profiles = aggregatedProfiles,
        onProfileClicked = onProfileClicked,
        icon = {
            Icon(
                painter = rememberVectorPainter(Icons.Rounded.FiberNew),
                tint = BookmarkBlue,
                contentDescription =
                    stringResource(CommonStrings.notifications_post_subscription_description),
            )
        },
        content = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text =
                            notificationText(
                                notification = notification,
                                aggregatedSize = aggregatedProfiles.size,
                                singularResource = CommonStrings.notifications_post_subscription,
                                pluralResource =
                                    CommonStrings.notifications_multiple_post_subscription,
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
