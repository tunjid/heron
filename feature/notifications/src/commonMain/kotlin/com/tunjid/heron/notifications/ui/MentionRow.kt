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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import kotlin.time.Instant

@Composable
fun MentionRow(
    modifier: Modifier = Modifier,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    now: Instant,
    isRead: Boolean,
    showEngagementMetrics: Boolean,
    notification: Notification.Mentioned,
    onLinkTargetClicked: (Notification.PostAssociated, LinkTarget) -> Unit,
    onProfileClicked: (Notification.PostAssociated, Profile) -> Unit,
    onPostClicked: (Notification.PostAssociated) -> Unit,
    onPostInteraction: (Notification.PostAssociated, PostAction.Options) -> Unit,
) {
    NotificationPostScaffold(
        modifier = modifier,
        paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
        now = now,
        isRead = isRead,
        showEngagementMetrics = showEngagementMetrics,
        notification = notification,
        onLinkTargetClicked = onLinkTargetClicked,
        onProfileClicked = onProfileClicked,
        onPostClicked = onPostClicked,
        onPostMediaClicked = { _, _, _ -> },
        onPostInteraction = onPostInteraction,
    )
}
