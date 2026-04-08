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

package com.tunjid.heron.standard.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.utilities.path
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.pathDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.timeline.ui.DismissableRefreshIndicator
import com.tunjid.heron.timeline.ui.EmptyContent
import com.tunjid.heron.timeline.ui.standard.Publication
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.modifiers.gridColumnCount
import com.tunjid.heron.ui.modifiers.shapedClickable
import com.tunjid.tiler.compose.PivotedTilingEffect
import heron.feature.standard_subscription.generated.resources.Res
import heron.feature.standard_subscription.generated.resources.empty_subscriptions
import heron.feature.standard_subscription.generated.resources.empty_subscriptions_description

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun StandardSubscriptionScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyStaggeredGridState()
    val items by rememberUpdatedState(state.tilingData.items)
    val density = LocalDensity.current
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = modifier
            .fillMaxSize(),
        isRefreshing = state.isRefreshing,
        state = pullToRefreshState,
        onRefresh = {
            actions(Action.Tile(TilingState.Action.Refresh))
        },
        indicator = {
            DismissableRefreshIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset {
                        IntOffset(x = 0, y = gridState.layoutInfo.beforeContentPadding)
                    },
                state = pullToRefreshState,
                isRefreshing = state.isRefreshing,
                onDismissRequest = {},
            )
        },
    ) {
        LazyVerticalStaggeredGrid(
            modifier = Modifier
                .fillMaxSize()
                .gridColumnCount(density) { numColumns ->
                    actions(
                        Action.Tile(TilingState.Action.GridSize(numColumns = numColumns)),
                    )
                },
            state = gridState,
            columns = StaggeredGridCells.Adaptive(360.dp),
            verticalItemSpacing = 8.dp,
            contentPadding = UiTokens.bottomNavAndInsetPaddingValues(
                top = UiTokens.statusBarHeight + UiTokens.toolbarHeight,
                isCompact = paneScaffoldState.prefersCompactBottomNav,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = !paneScaffoldState.isTransitionActive,
        ) {
            if (items.isEmpty()) item {
                EmptyContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    titleRes = Res.string.empty_subscriptions,
                    descriptionRes = Res.string.empty_subscriptions_description,
                    icon = Icons.Rounded.NotificationsOff,
                )
            }
            else items(
                items = items,
                key = { it.uri.uri },
                itemContent = { publication ->
                    Publication(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .animateItem()
                            .shapedClickable {
                                actions(
                                    Action.Navigate.To(
                                        pathDestination(
                                            path = publication.uri.path,
                                            referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                        ),
                                    ),
                                )
                            }
                            .padding(horizontal = 8.dp),
                        paneTransitionScope = paneScaffoldState,
                        sharedElementPrefix = SharedElementPrefix,
                        publication = publication,
                        onSubscriptionToggled = { publication, subscription ->
                            actions(
                                if (subscription != null) Action.TogglePublicationSubscription.Unsubscribe(
                                    subscriptionUri = subscription.uri,
                                )
                                else Action.TogglePublicationSubscription.Subscribe(
                                    publicationUri = publication.uri,
                                ),
                            )
                        },
                    )
                },
            )
        }
    }

    gridState.PivotedTilingEffect(
        items = items,
        onQueryChanged = { query ->
            actions(
                Action.Tile(
                    TilingState.Action.LoadAround(
                        query = query ?: state.tilingData.currentQuery,
                    ),
                ),
            )
        },
    )
}

private const val SharedElementPrefix = "standard-subscriptions"
