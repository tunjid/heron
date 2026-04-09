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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.link
import com.tunjid.heron.timeline.ui.TimeDelta
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.UiTokens.BookmarkBlue
import com.tunjid.heron.ui.icons.Description
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.modifiers.rootShapedClickable
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.notifications_document_published
import heron.ui.core.generated.resources.notifications_document_published_description
import heron.ui.core.generated.resources.notifications_multiple_document_published
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

@Composable
fun DocumentPublishedRow(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    now: Instant,
    isRead: Boolean,
    notification: Notification.DocumentPublished,
    aggregatedProfiles: List<Profile>,
    onProfileClicked: (Notification, Profile) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    NotificationAggregateScaffold(
        paneTransitionScope = paneTransitionScope,
        modifier = modifier
            .rootShapedClickable {
                runCatching {
                    notification.associatedDocument
                        .link
                        ?.let(uriHandler::openUri)
                }
            },
        isRead = isRead,
        notification = notification,
        profiles = aggregatedProfiles,
        onProfileClicked = onProfileClicked,
        icon = {
            Icon(
                painter = rememberVectorPainter(HeronIcons.Description),
                tint = BookmarkBlue,
                contentDescription = stringResource(CommonStrings.notifications_document_published_description),
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
                            singularResource = CommonStrings.notifications_document_published,
                            pluralResource = CommonStrings.notifications_multiple_document_published,
                        ),
                    )

                    TimeDelta(
                        modifier = Modifier.alignByBaseline(),
                        delta = now - notification.indexedAt,
                    )
                }
                Text(
                    text = notification.associatedDocument.title,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.outline),
                )
            }
        },
    )
}
