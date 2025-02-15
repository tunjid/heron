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

package com.tunjid.heron.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.composables.accumulatedoffsetnestedscrollconnection.rememberAccumulatedOffsetNestedScrollConnection
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.domain.timeline.TimelineLoadAction
import com.tunjid.heron.domain.timeline.TimelineStateHolder
import com.tunjid.heron.domain.timeline.TimelineStatus
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.paneClip
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.withQuotedPostPrefix
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.effects.PauseVideoOnTabChangeEffect
import com.tunjid.heron.timeline.ui.effects.TimelineRefreshEffect
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.timeline.ui.rememberPostActions
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.tabIndex
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.threepane.ThreePane
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
internal fun HomeScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val updatedTimelineStateHolders by rememberUpdatedState(
        state.timelineStateHolders
    )
    val pagerState = rememberPagerState {
        updatedTimelineStateHolders.size
    }

    Box(
        modifier = modifier
            .padding(horizontal = 8.dp)
    ) {
        val tabsOffsetNestedScrollConnection = rememberAccumulatedOffsetNestedScrollConnection(
            maxOffset = { Offset.Zero },
            minOffset = { Offset(x = 0f, y = (-UiTokens.tabsHeight).toPx()) },
        )
        HorizontalPager(
            modifier = Modifier
                .nestedScroll(tabsOffsetNestedScrollConnection)
                .offset {
                    tabsOffsetNestedScrollConnection.offset.round() + IntOffset(
                        x = 0,
                        y = UiTokens.tabsHeight.roundToPx(),
                    )
                }
                .paneClip(),
            state = pagerState,
            key = { page -> updatedTimelineStateHolders.keyAt(page) },
            pageContent = { page ->
                val timelineStateHolder = remember {
                    updatedTimelineStateHolders.stateHolderAt(page)
                }
                HomeTimeline(
                    panedSharedElementScope = paneScaffoldState,
                    timelineStateHolder = timelineStateHolder,
                    actions = actions,
                )
            }
        )
        HomeTabs(
            modifier = Modifier
                .offset {
                    tabsOffsetNestedScrollConnection.offset.round()
                },
            pagerState = pagerState,
            tabs = remember(state.sourceIdsToHasUpdates, state.timelines) {
                state.timelines.map { timeline ->
                    Tab(
                        title = timeline.name,
                        hasUpdate = state.sourceIdsToHasUpdates[timeline.sourceId] == true,
                    )
                }
            },
            onRefreshTabClicked = { page ->
                updatedTimelineStateHolders.stateHolderAt(page)
                    .accept(TimelineLoadAction.Refresh)
            }
        )

        pagerState.PauseVideoOnTabChangeEffect()

        LaunchedEffect(Unit) {
            snapshotFlow { pagerState.currentPage }
                .collect { page ->
                    if (page < updatedTimelineStateHolders.size) actions(
                        Action.SetCurrentTab(
                            updatedTimelineStateHolders.stateHolderAt(page)
                                .state
                                .value
                                .timeline
                                .sourceId
                        )
                    )
                }
        }
    }
}

@Composable
private fun HomeTabs(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    tabs: List<Tab>,
    onRefreshTabClicked: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    Tabs(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxWidth(),
        tabs = tabs,
        selectedTabIndex = pagerState.tabIndex,
        onTabSelected = {
            scope.launch {
                pagerState.animateScrollToPage(it)
            }
        },
        onTabReselected = onRefreshTabClicked,
    )
}

@Composable
private fun HomeTimeline(
    panedSharedElementScope: PanedSharedElementScope,
    timelineStateHolder: TimelineStateHolder,
    actions: (Action) -> Unit,
) {

    val gridState = rememberLazyStaggeredGridState()
    val timelineState by timelineStateHolder.state.collectAsStateWithLifecycle()
    val items by rememberUpdatedState(timelineState.items)

    val density = LocalDensity.current
    val videoStates = remember { ThreadedVideoPositionStates() }

    PullToRefreshBox(
        modifier = Modifier
            .pullToRefresh(
                isRefreshing = timelineState.status is TimelineStatus.Refreshing,
                state = rememberPullToRefreshState(),
                onRefresh = { timelineStateHolder.accept(TimelineLoadAction.Refresh) }
            )
            .fillMaxSize(),
        isRefreshing = timelineState.status is TimelineStatus.Refreshing,
        state = rememberPullToRefreshState(),
        onRefresh = { timelineStateHolder.accept(TimelineLoadAction.Refresh) }
    ) {
        LazyVerticalStaggeredGrid(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    timelineStateHolder.accept(
                        TimelineLoadAction.GridSize(
                            floor(it.width / with(density) { CardSize.toPx() }).roundToInt()
                        )
                    )
                },
            state = gridState,
            columns = StaggeredGridCells.Adaptive(CardSize),
            verticalItemSpacing = 8.dp,
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = !panedSharedElementScope.isTransitionActive,
        ) {
            items(
                items = items,
                key = TimelineItem::id,
                itemContent = { item ->
                    TimelineItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .threadedVideoPosition(
                                state = videoStates.getOrCreateStateFor(item)
                            ),
                        panedSharedElementScope = panedSharedElementScope,
                        now = remember { Clock.System.now() },
                        item = item,
                        sharedElementPrefix = timelineState.timeline.sourceId,
                        postActions = rememberPostActions(
                            onPostClicked = { post: Post, quotingPostId: Id? ->
                                actions(
                                    Action.Navigate.DelegateTo(
                                        NavigationAction.Common.ToPost(
                                            referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                            sharedElementPrefix = timelineState.timeline.sourceId.withQuotedPostPrefix(
                                                quotingPostId = quotingPostId,
                                            ),
                                            post = post,
                                        )
                                    )
                                )
                            },
                            onProfileClicked = { profile: Profile, post: Post, quotingPostId: Id? ->
                                actions(
                                    Action.Navigate.DelegateTo(
                                        NavigationAction.Common.ToProfile(
                                            referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                            profile = profile,
                                            avatarSharedElementKey = post
                                                .avatarSharedElementKey(
                                                    prefix = timelineState.timeline.sourceId,
                                                    quotingPostId = quotingPostId,
                                                )
                                                .takeIf { post.author.did == profile.did }
                                        )
                                    )
                                )
                            },
                            onPostMediaClicked = { media: Embed.Media, index: Int, post: Post, quotingPostId: Id? ->
                                actions(
                                    Action.Navigate.DelegateTo(
                                        NavigationAction.Common.ToMedia(
                                            post = post,
                                            media = media,
                                            startIndex = index,
                                            sharedElementPrefix = timelineState.timeline.sourceId.withQuotedPostPrefix(
                                                quotingPostId = quotingPostId,
                                            ),
                                        )
                                    )
                                )
                            },
                            onReplyToPost = { post: Post ->
                                actions(
                                    Action.Navigate.DelegateTo(
                                        NavigationAction.Common.ComposePost(
                                            type = Post.Create.Reply(
                                                parent = post,
                                            ),
                                            sharedElementPrefix = timelineState.timeline.sourceId.withQuotedPostPrefix(),
                                        )
                                    )
                                )
                            },
                            onPostInteraction = {
                                actions(
                                    Action.SendPostInteraction(it)
                                )
                            }
                        ),
                    )
                }
            )
        }
    }

    if (panedSharedElementScope.paneState.pane == ThreePane.Primary) {
        val videoPlayerController = LocalVideoPlayerController.current
        gridState.interpolatedVisibleIndexEffect(
            denominator = 10,
            itemsAvailable = items.size,
        ) { interpolatedIndex ->
            val flooredIndex = floor(interpolatedIndex).toInt()
            val fraction = interpolatedIndex - flooredIndex
            items.getOrNull(flooredIndex)
                ?.let(videoStates::retrieveStateFor)
                ?.videoIdAt(fraction)
                ?.let(videoPlayerController::play)
                ?: videoPlayerController.pauseActiveVideo()
        }
    }

    gridState.PivotedTilingEffect(
        items = items,
        onQueryChanged = { query ->
            timelineStateHolder.accept(
                TimelineLoadAction.LoadAround(query ?: timelineState.currentQuery)
            )
        }
    )

    gridState.TimelineRefreshEffect(
        timelineState = timelineState,
        onRefresh = { animateScrollToItem(index = 0) }
    )

    LaunchedEffect(gridState) {
        snapshotFlow {
            Action.UpdatePageWithUpdates(
                sourceId = timelineState.timeline.sourceId,
                hasUpdates = timelineState.hasUpdates,
            )
        }
            .collect(actions)
    }
}

private val CardSize = 340.dp
