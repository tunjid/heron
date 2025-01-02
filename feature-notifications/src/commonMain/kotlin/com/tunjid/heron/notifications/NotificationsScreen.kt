/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.notifications

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.notifications.ui.FollowRow
import com.tunjid.heron.notifications.ui.JoinedStarterPackRow
import com.tunjid.heron.notifications.ui.LikeRow
import com.tunjid.heron.notifications.ui.MentionRow
import com.tunjid.heron.notifications.ui.QuoteRow
import com.tunjid.heron.notifications.ui.ReplyRow
import com.tunjid.heron.notifications.ui.RepostRow
import com.tunjid.heron.scaffold.scaffold.StatusBarHeight
import com.tunjid.heron.scaffold.scaffold.TabsHeight
import com.tunjid.heron.scaffold.scaffold.ToolbarHeight
import com.tunjid.heron.ui.SharedElementScope
import com.tunjid.tiler.compose.PivotedTilingEffect
import kotlinx.datetime.Clock

@Composable
internal fun NotificationsScreen(
    sharedElementScope: SharedElementScope,
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit,
) {
    val listState = rememberLazyListState()
    val items by rememberUpdatedState(state.aggregateNotifications())
    val now = remember { Clock.System.now() }
    val onProfileClicked: (Notification, Profile) -> Unit = remember { { _, _ -> } }

    LazyColumn(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxSize()
            .clip(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                )
            ),
        state = listState,
        contentPadding = PaddingValues(
            top = StatusBarHeight + TabsHeight,
            start = 8.dp,
            end = 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        userScrollEnabled = !sharedElementScope.isTransitionActive,
    ) {
        items(
            items = items,
            key = AggregatedNotification::id,
            itemContent = { item ->
                when (val notification = item.notification) {
                    is Notification.Followed -> FollowRow(
                        now = now,
                        notification = notification,
                        aggregatedProfiles = item.aggregatedProfiles,
                        onProfileClicked = onProfileClicked,
                    )

                    is Notification.JoinedStarterPack -> JoinedStarterPackRow(
                        now = now,
                        notification = notification,
                        aggregatedProfiles = item.aggregatedProfiles,
                        onProfileClicked = onProfileClicked,
                    )

                    is Notification.Liked -> LikeRow(
                        now = now,
                        notification = notification,
                        aggregatedProfiles = item.aggregatedProfiles,
                        onProfileClicked = onProfileClicked,
                        onPostClicked = {},
                    )

                    is Notification.Mentioned -> MentionRow(
                        sharedElementScope = sharedElementScope,
                        now = now,
                        notification = notification,
                        onProfileClicked = onProfileClicked,
                    )

                    is Notification.Quoted -> QuoteRow(
                        sharedElementScope = sharedElementScope,
                        now = now,
                        notification = notification,
                        onProfileClicked = onProfileClicked,
                    )

                    is Notification.RepliedTo -> ReplyRow(
                        sharedElementScope = sharedElementScope,
                        now = now,
                        notification = notification,
                        onProfileClicked = onProfileClicked,
                    )

                    is Notification.Reposted -> RepostRow(
                        now = now,
                        notification = notification,
                        aggregatedProfiles = item.aggregatedProfiles,
                        onProfileClicked = onProfileClicked,
                        onPostClicked = {},
                    )

                    is Notification.Unknown -> Unit
                }
            }
        )
    }

    listState.PivotedTilingEffect(
        items = items,
        onQueryChanged = { query ->
            actions(
                Action.LoadAround(query ?: state.currentQuery)
            )
        }
    )
}
