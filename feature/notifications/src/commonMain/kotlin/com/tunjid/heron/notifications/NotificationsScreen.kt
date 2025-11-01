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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.utilities.asGenericUri
import com.tunjid.heron.notifications.ui.FollowRow
import com.tunjid.heron.notifications.ui.JoinedStarterPackRow
import com.tunjid.heron.notifications.ui.LikeRow
import com.tunjid.heron.notifications.ui.MentionRow
import com.tunjid.heron.notifications.ui.ProfileVerificationRow
import com.tunjid.heron.notifications.ui.QuoteRow
import com.tunjid.heron.notifications.ui.ReplyRow
import com.tunjid.heron.notifications.ui.RepostRow
import com.tunjid.heron.notifications.ui.avatarSharedElementKey
import com.tunjid.heron.notifications.ui.sharedElementPrefix
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.composePostDestination
import com.tunjid.heron.scaffold.navigation.conversationDestination
import com.tunjid.heron.scaffold.navigation.pathDestination
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.navigation.recordDestination
import com.tunjid.heron.scaffold.navigation.signInDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.paneClip
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.isRefreshing
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.post.PostInteractionsSheetState.Companion.rememberUpdatedPostInteractionState
import com.tunjid.heron.timeline.ui.post.PostOptionsSheetState.Companion.rememberUpdatedPostOptionsState
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.UiTokens.bottomNavAndInsetPaddingValues
import com.tunjid.tiler.compose.PivotedTilingEffect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.datetime.Clock

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NotificationsScreen(
    paneScaffoldState: PaneScaffoldState,
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit,
) {
    val listState = rememberLazyListState()
    val postInteractionState = rememberUpdatedPostInteractionState(
        isSignedIn = paneScaffoldState.isSignedIn,
        onSignInClicked = {
            actions(Action.Navigate.To(signInDestination()))
        },
        onInteractionConfirmed = {
            actions(Action.SendPostInteraction(it))
        },
        onQuotePostClicked = { repost ->
            actions(
                Action.Navigate.To(
                    composePostDestination(
                        type = Post.Create.Quote(repost),
                        sharedElementPrefix = null,
                    ),
                ),
            )
        },
    )
    val postOptionsState = rememberUpdatedPostOptionsState(
        signedInProfileId = state.signedInProfile?.did,
        recentConversations = state.recentConversations,
        onShareInConversationClicked = { currentPost, conversation ->
            actions(
                Action.Navigate.To(
                    conversationDestination(
                        id = conversation.id,
                        members = conversation.members,
                        sharedElementPrefix = conversation.id.id,
                        sharedUri = currentPost.uri.asGenericUri(),
                        referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                    ),
                ),
            )
        },
    )

    val items by rememberUpdatedState(state.aggregateNotifications())
    val now = remember { Clock.System.now() }
    val onAggregatedProfileClicked: (Notification, Profile) -> Unit = remember {
        { notification, profile ->
            actions(
                Action.Navigate.To(
                    profileDestination(
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                        profile = profile,
                        avatarSharedElementKey = notification.avatarSharedElementKey(profile),
                    ),
                ),
            )
        }
    }
    val onLinkTargetClicked: (Notification.PostAssociated, LinkTarget) -> Unit = remember {
        { _, linkTarget ->
            if (linkTarget is LinkTarget.Navigable) actions(
                Action.Navigate.To(
                    pathDestination(
                        path = linkTarget.path,
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                    ),
                ),
            )
        }
    }
    val onProfileClicked: (Notification.PostAssociated, Profile) -> Unit = remember {
        { notification, profile ->
            actions(
                Action.Navigate.To(
                    profileDestination(
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                        profile = profile,
                        avatarSharedElementKey = notification.associatedPost.avatarSharedElementKey(
                            notification.sharedElementPrefix(),
                        ),
                    ),
                ),
            )
        }
    }
    val onPostClicked = remember {
        { notification: Notification.PostAssociated ->
            actions(
                Action.Navigate.To(
                    recordDestination(
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                        sharedElementPrefix = notification.sharedElementPrefix(),
                        record = notification.associatedPost,
                    ),
                ),
            )
        }
    }
    val onReplyToPost = remember {
        { notification: Notification.PostAssociated ->
            actions(
                Action.Navigate.To(
                    if (paneScaffoldState.isSignedOut) signInDestination()
                    else composePostDestination(
                        type = Post.Create.Reply(
                            parent = notification.associatedPost,
                        ),
                        sharedElementPrefix = notification.sharedElementPrefix(),
                    ),
                ),
            )
        }
    }
    val onPostInteraction = postInteractionState::onInteraction

    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = modifier
            .fillMaxSize(),
        state = pullToRefreshState,
        isRefreshing = state.isRefreshing,
        onRefresh = {
            actions(
                Action.Tile(TilingState.Action.Refresh),
            )
        },
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset {
                        IntOffset(x = 0, y = listState.layoutInfo.beforeContentPadding)
                    },
                state = pullToRefreshState,
                isRefreshing = state.isRefreshing,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxSize()
                .paneClip(),
            state = listState,
            contentPadding = bottomNavAndInsetPaddingValues(
                top = UiTokens.statusBarHeight + UiTokens.toolbarHeight,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = !paneScaffoldState.isTransitionActive,
        ) {
            items(
                items = items,
                key = AggregatedNotification::id,
                itemContent = { item ->
                    val itemModifier = Modifier
                        .animateBounds(
                            lookaheadScope = paneScaffoldState,
                        )
                        .animateItem()

                    when (val notification = item.notification) {
                        is Notification.Followed -> FollowRow(
                            modifier = itemModifier,
                            paneMovableElementSharedTransitionScope = paneScaffoldState,
                            now = now,
                            isRead = item.isRead,
                            notification = notification,
                            aggregatedProfiles = item.aggregatedProfiles,
                            onProfileClicked = onAggregatedProfileClicked,
                        )

                        is Notification.JoinedStarterPack -> JoinedStarterPackRow(
                            modifier = itemModifier,
                            paneMovableElementSharedTransitionScope = paneScaffoldState,
                            now = now,
                            isRead = item.isRead,
                            notification = notification,
                            aggregatedProfiles = item.aggregatedProfiles,
                            onProfileClicked = onAggregatedProfileClicked,
                        )

                        is Notification.Liked -> LikeRow(
                            modifier = itemModifier,
                            paneMovableElementSharedTransitionScope = paneScaffoldState,
                            now = now,
                            isRead = item.isRead,
                            notification = notification,
                            aggregatedProfiles = item.aggregatedProfiles,
                            onProfileClicked = onAggregatedProfileClicked,
                            onPostClicked = onPostClicked,
                        )

                        is Notification.Mentioned -> MentionRow(
                            modifier = itemModifier,
                            paneMovableElementSharedTransitionScope = paneScaffoldState,
                            now = now,
                            isRead = item.isRead,
                            notification = notification,
                            onLinkTargetClicked = onLinkTargetClicked,
                            onProfileClicked = onProfileClicked,
                            onPostClicked = onPostClicked,
                            onPostInteraction = onPostInteraction,
                            onPostOptionsClicked = postOptionsState::showOptions,
                        )

                        is Notification.Quoted -> QuoteRow(
                            modifier = itemModifier,
                            paneMovableElementSharedTransitionScope = paneScaffoldState,
                            now = now,
                            isRead = item.isRead,
                            notification = notification,
                            onLinkTargetClicked = onLinkTargetClicked,
                            onProfileClicked = onProfileClicked,
                            onPostClicked = onPostClicked,
                            onPostInteraction = onPostInteraction,
                            onPostOptionsClicked = postOptionsState::showOptions,
                        )

                        is Notification.RepliedTo -> ReplyRow(
                            modifier = itemModifier,
                            paneMovableElementSharedTransitionScope = paneScaffoldState,
                            now = now,
                            isRead = item.isRead,
                            notification = notification,
                            onLinkTargetClicked = onLinkTargetClicked,
                            onProfileClicked = onProfileClicked,
                            onPostClicked = onPostClicked,
                            onReplyToPost = onReplyToPost,
                            onPostInteraction = onPostInteraction,
                            onPostOptionsClicked = postOptionsState::showOptions,
                        )

                        is Notification.Reposted -> RepostRow(
                            modifier = itemModifier,
                            paneMovableElementSharedTransitionScope = paneScaffoldState,
                            now = now,
                            isRead = item.isRead,
                            notification = notification,
                            aggregatedProfiles = item.aggregatedProfiles,
                            onProfileClicked = onAggregatedProfileClicked,
                            onPostClicked = onPostClicked,
                        )

                        is Notification.Unknown -> Unit
                        is Notification.Unverified -> ProfileVerificationRow(
                            modifier = itemModifier,
                            paneMovableElementSharedTransitionScope = paneScaffoldState,
                            now = now,
                            isRead = item.isRead,
                            isVerified = false,
                            notification = notification,
                            onProfileClicked = onAggregatedProfileClicked,
                        )

                        is Notification.Verified -> ProfileVerificationRow(
                            modifier = itemModifier,
                            paneMovableElementSharedTransitionScope = paneScaffoldState,
                            now = now,
                            isRead = item.isRead,
                            isVerified = true,
                            notification = notification,
                            onProfileClicked = onAggregatedProfileClicked,
                        )
                    }
                },
            )
        }
    }

    listState.PivotedTilingEffect(
        items = items,
        onQueryChanged = { query ->
            actions(
                Action.Tile(
                    tilingAction = TilingState.Action.LoadAround(
                        query ?: state.tilingData.currentQuery,
                    ),
                ),
            )
        },
    )

    LaunchedEffect(listState) {
        snapshotFlow {
            if (listState.lastScrolledForward) return@snapshotFlow null

            val firstVisibleNotification = items.getOrNull(
                listState.firstVisibleItemIndex,
            ) ?: return@snapshotFlow null
            firstVisibleNotification.notification.indexedAt
        }
            .filterNotNull()
            .collect {
                actions(Action.MarkNotificationsRead(it))
            }
    }
}
