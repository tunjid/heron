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
import com.tunjid.heron.timeline.ui.TimeDelta
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.ui.core.generated.resources.notifications_account_unverified
import heron.ui.core.generated.resources.notifications_account_verified
import heron.ui.core.generated.resources.notifications_joined_from_your_started_pack_description
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileVerificationRow(
    modifier: Modifier = Modifier,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    now: Instant,
    isRead: Boolean,
    isVerified: Boolean,
    notification: Notification,
    onProfileClicked: (Notification, Profile) -> Unit,
) {
    NotificationAggregateScaffold(
        paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
        modifier = modifier
            .clickable {
                onProfileClicked(notification, notification.author)
            },
        isRead = isRead,
        notification = notification,
        profiles = emptyList(),
        onProfileClicked = onProfileClicked,
        icon = {
            Icon(
                painter = rememberVectorPainter(Icons.Rounded.Person),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = stringResource(CommonStrings.notifications_joined_from_your_started_pack_description),
            )
        },
        content = {
            Row(horizontalArrangement = spacedBy(8.dp)) {
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = stringResource(
                        if (isVerified) CommonStrings.notifications_account_verified
                        else CommonStrings.notifications_account_unverified,
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
