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

package com.tunjid.heron.notifications

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
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.notifications.ui.FollowRow
import com.tunjid.heron.notifications.ui.JoinedStarterPackRow
import com.tunjid.heron.notifications.ui.LikeRow
import com.tunjid.heron.notifications.ui.MentionRow
import com.tunjid.heron.notifications.ui.QuoteRow
import com.tunjid.heron.notifications.ui.ReplyRow
import com.tunjid.heron.notifications.ui.RepostRow
import com.tunjid.heron.notifications.ui.avatarSharedElementKey
import com.tunjid.heron.notifications.ui.sharedElementPrefix
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.scaffold.StatusBarHeight
import com.tunjid.heron.scaffold.scaffold.ToolbarHeight
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.tiler.compose.PivotedTilingEffect
import kotlinx.datetime.Clock

@Composable
internal fun NotificationsScreen(
    panedSharedElementScope: PanedSharedElementScope,
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit,
) {
    val listState = rememberLazyListState()
    val items by rememberUpdatedState(state.aggregateNotifications())
    val now = remember { Clock.System.now() }
    val onAggregatedProfileClicked: (Notification, Profile) -> Unit = remember {
        { notification, profile ->
            actions(
                Action.Navigate.DelegateTo(
                    NavigationAction.Common.ToProfile(
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                        profile = profile,
                        avatarSharedElementKey = notification.avatarSharedElementKey(profile)
                    )
                )
            )
        }
    }
    val onProfileClicked: (Notification.PostAssociated, Profile) -> Unit = remember {
        { notification, profile ->
            actions(
                Action.Navigate.DelegateTo(
                    NavigationAction.Common.ToProfile(
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                        profile = profile,
                        avatarSharedElementKey = notification.associatedPost.avatarSharedElementKey(
                            notification.sharedElementPrefix()
                        )
                    )
                )
            )
        }
    }
    val onPostClicked = remember {
        { notification: Notification.PostAssociated ->
            actions(
                Action.Navigate.DelegateTo(
                    NavigationAction.Common.ToPost(
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                        sharedElementPrefix = notification.sharedElementPrefix(),
                        post = notification.associatedPost,
                    )
                )
            )
        }
    }
    val onReplyToPost = remember {
        { notification: Notification.PostAssociated ->
            actions(
                Action.Navigate.DelegateTo(
                    NavigationAction.Common.ComposePost(
                        type = Post.Create.Reply(
                            parent = notification.associatedPost,
                        ),
                        sharedElementPrefix = notification.sharedElementPrefix(),
                    )
                )
            )
        }
    }
    val onPostInteraction = remember {
        { interaction: Post.Interaction ->
            actions(Action.SendPostInteraction(interaction))
        }
    }

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
            top = StatusBarHeight + ToolbarHeight,
            start = 8.dp,
            end = 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = !panedSharedElementScope.isTransitionActive,
    ) {
        items(
            items = items,
            key = AggregatedNotification::id,
            itemContent = { item ->
                val itemModifier = Modifier
                    .animateItem()

                when (val notification = item.notification) {
                    is Notification.Followed -> FollowRow(
                        modifier = itemModifier,
                        panedSharedElementScope = panedSharedElementScope,
                        now = now,
                        notification = notification,
                        aggregatedProfiles = item.aggregatedProfiles,
                        onProfileClicked = onAggregatedProfileClicked,
                    )

                    is Notification.JoinedStarterPack -> JoinedStarterPackRow(
                        modifier = itemModifier,
                        panedSharedElementScope = panedSharedElementScope,
                        now = now,
                        notification = notification,
                        aggregatedProfiles = item.aggregatedProfiles,
                        onProfileClicked = onAggregatedProfileClicked,
                    )

                    is Notification.Liked -> LikeRow(
                        modifier = itemModifier,
                        panedSharedElementScope = panedSharedElementScope,
                        now = now,
                        notification = notification,
                        aggregatedProfiles = item.aggregatedProfiles,
                        onProfileClicked = onAggregatedProfileClicked,
                        onPostClicked = onPostClicked,
                    )

                    is Notification.Mentioned -> MentionRow(
                        modifier = itemModifier,
                        panedSharedElementScope = panedSharedElementScope,
                        now = now,
                        notification = notification,
                        onProfileClicked = onProfileClicked,
                        onPostClicked = onPostClicked,
                        onPostInteraction = onPostInteraction,
                    )

                    is Notification.Quoted -> QuoteRow(
                        modifier = itemModifier,
                        panedSharedElementScope = panedSharedElementScope,
                        now = now,
                        notification = notification,
                        onProfileClicked = onProfileClicked,
                        onPostClicked = onPostClicked,
                        onPostInteraction = onPostInteraction,
                    )

                    is Notification.RepliedTo -> ReplyRow(
                        modifier = itemModifier,
                        panedSharedElementScope = panedSharedElementScope,
                        now = now,
                        notification = notification,
                        onProfileClicked = onProfileClicked,
                        onPostClicked = onPostClicked,
                        onReplyToPost = onReplyToPost,
                        onPostInteraction = onPostInteraction,
                    )

                    is Notification.Reposted -> RepostRow(
                        modifier = itemModifier,
                        panedSharedElementScope = panedSharedElementScope,
                        now = now,
                        notification = notification,
                        aggregatedProfiles = item.aggregatedProfiles,
                        onProfileClicked = onAggregatedProfileClicked,
                        onPostClicked = onPostClicked,
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
