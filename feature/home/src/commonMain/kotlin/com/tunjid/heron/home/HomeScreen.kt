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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.composables.accumulatedoffsetnestedscrollconnection.rememberAccumulatedOffsetNestedScrollConnection
import com.tunjid.composables.lazy.pendingScrollOffsetState
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.core.models.uri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.utilities.path
import com.tunjid.heron.home.ui.RestoreLastViewedTabEffect
import com.tunjid.heron.home.ui.TabsCollapseEffect
import com.tunjid.heron.home.ui.TabsExpansionEffect
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.composePostDestination
import com.tunjid.heron.scaffold.navigation.conversationDestination
import com.tunjid.heron.scaffold.navigation.galleryDestination
import com.tunjid.heron.scaffold.navigation.pathDestination
import com.tunjid.heron.scaffold.navigation.postDestination
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.navigation.settingsDestination
import com.tunjid.heron.scaffold.navigation.signInDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.paneClip
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.isRefreshing
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.state.TimelineStateHolder
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.effects.TimelineRefreshEffect
import com.tunjid.heron.timeline.ui.post.PostInteractionsSheetState.Companion.rememberUpdatedPostInteractionState
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.timeline.ui.postActions
import com.tunjid.heron.timeline.utilities.canAutoPlayVideo
import com.tunjid.heron.timeline.utilities.cardSize
import com.tunjid.heron.timeline.utilities.lazyGridHorizontalItemSpacing
import com.tunjid.heron.timeline.utilities.lazyGridVerticalItemSpacing
import com.tunjid.heron.timeline.utilities.pendingOffsetFor
import com.tunjid.heron.timeline.utilities.sharedElementPrefix
import com.tunjid.heron.timeline.utilities.timelineHorizontalPadding
import com.tunjid.heron.ui.PagerTopGapCloseEffect
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.tabIndex
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.threepane.ThreePane
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun HomeScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val updatedTimelineStateHolders by rememberUpdatedState(
        state.timelineStateHolders,
    )

    val pagerState = rememberPagerState {
        updatedTimelineStateHolders.count { it is HomeScreenStateHolders.Pinned }
    }
    val scope = rememberCoroutineScope()
    val topClearance = UiTokens.statusBarHeight + UiTokens.toolbarHeight

    pagerState.RestoreLastViewedTabEffect(
        lastViewedTabUri = state.currentTabUri,
        timelines = state.timelines,
    )

    Box(
        modifier = modifier,
    ) {
        val tabsOffsetNestedScrollConnection = rememberAccumulatedOffsetNestedScrollConnection(
            maxOffset = { Offset.Zero },
            minOffset = { Offset(x = 0f, y = -UiTokens.toolbarHeight.toPx()) },
        )
        HorizontalPager(
            modifier = Modifier
                .nestedScroll(tabsOffsetNestedScrollConnection)
                .paneClip(),
            state = pagerState,
            key = { page ->
                updatedTimelineStateHolders[page]
                    .state
                    .value
                    .timeline
                    .sourceId
            },
            pageContent = { page ->
                val gridState = rememberLazyStaggeredGridState()
                val timelineStateHolder = updatedTimelineStateHolders[page]
                HomeTimeline(
                    paneScaffoldState = paneScaffoldState,
                    gridState = gridState,
                    timelineStateHolder = timelineStateHolder,
                    tabsOffset = tabsOffsetNestedScrollConnection::offset,
                    actions = actions,
                    conversations = state.conversations,
                )
                tabsOffsetNestedScrollConnection.PagerTopGapCloseEffect(
                    pagerState = pagerState,
                    firstVisibleItemIndex = gridState::firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = gridState::firstVisibleItemScrollOffset,
                    scrollBy = gridState::animateScrollBy,
                )
            },
        )
        HomeTabs(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = 0,
                        y = topClearance.roundToPx(),
                    ) + tabsOffsetNestedScrollConnection.offset.round()
                },
            sharedTransitionScope = paneScaffoldState,
            selectedTabIndex = pagerState::tabIndex,
            saveRequestId = state.timelinePreferenceSaveRequestId,
            currentTabUri = state.currentTabUri,
            isSignedIn = state.signedInProfile != null,
            tabLayout = state.tabLayout,
            timelines = state.timelines,
            sourceIdsToHasUpdates = state.sourceIdsToHasUpdates,
            onCollapsedTabSelected = { page ->
                scope.launch {
                    pagerState.animateScrollToPage(page)
                }
            },
            onCollapsedTabReselected = { page ->
                updatedTimelineStateHolders
                    .getOrNull(page)
                    ?.accept
                    ?.invoke(
                        TimelineState.Action.Tile(
                            tilingAction = TilingState.Action.Refresh,
                        ),
                    )
            },
            onExpandedTabSelected = { page ->
                when (val holder = updatedTimelineStateHolders.getOrNull(page)) {
                    is HomeScreenStateHolders.Pinned -> scope.launch {
                        pagerState.animateScrollToPage(page)
                    }

                    is HomeScreenStateHolders.Saved -> holder.state.value.timeline.uri?.path?.let {
                        actions(Action.Navigate.To(pathDestination(it)))
                    }

                    null -> Unit
                }
            },
            onLayoutChanged = { layout ->
                actions(Action.SetTabLayout(layout = layout))
            },
            onTimelinePresentationUpdated = click@{ index, presentation ->
                val timelineStateHolder = updatedTimelineStateHolders.getOrNull(index)
                    ?: return@click
                timelineStateHolder.accept(
                    TimelineState.Action.UpdatePreferredPresentation(
                        timeline = timelineStateHolder.state.value.timeline,
                        presentation = presentation,
                    ),
                )
            },
            onTimelinePreferencesSaved = { timelines ->
                actions(
                    Action.UpdateTimeline.Update(timelines),
                )
            },
            onSettingsIconClick = {
                actions(
                    Action.Navigate.To(settingsDestination()),
                )
            },
        )

        tabsOffsetNestedScrollConnection.TabsExpansionEffect(
            isExpanded = state.tabLayout is TabLayout.Expanded,
        )

        tabsOffsetNestedScrollConnection.TabsCollapseEffect(
            state.tabLayout,
            onCollapsed = { actions(Action.SetTabLayout(it)) },
        )

        LaunchedEffect(Unit) {
            snapshotFlow { pagerState.currentPage }
                .collect { page ->
                    val holder = updatedTimelineStateHolders.getOrNull(page) ?: return@collect
                    val currentTabUri = holder
                        .state
                        .value
                        .timeline
                        .uri
                        ?: return@collect
                    actions(Action.SetCurrentTab(currentTabUri = currentTabUri))
                }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeTimeline(
    gridState: LazyStaggeredGridState,
    paneScaffoldState: PaneScaffoldState,
    timelineStateHolder: TimelineStateHolder,
    tabsOffset: () -> Offset,
    actions: (Action) -> Unit,
    conversations: List<Conversation>,
) {
    val timelineState by timelineStateHolder.state.collectAsStateWithLifecycle()
    val items by rememberUpdatedState(timelineState.tiledItems)
    val pendingScrollOffsetState = gridState.pendingScrollOffsetState()

    val density = LocalDensity.current
    val videoStates = remember { ThreadedVideoPositionStates(TimelineItem::id) }
    val presentation = timelineState.timeline.presentation
    val pullToRefreshState = rememberPullToRefreshState()
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
                        sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
                    ),
                ),
            )
        },
        recentConversations = conversations,
        onConversationClicked = { conversation, postUri ->
            actions(
                Action.Navigate.To(
                    conversationDestination(
                        id = conversation.id,
                        members = conversation.members,
                        sharedElementPrefix = conversation.id.id,
                        sharedPostUri = postUri,
                        referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                    ),
                ),
            )
        },
    )

    PullToRefreshBox(
        modifier = Modifier
            .padding(
                horizontal = animateDpAsState(
                    presentation.timelineHorizontalPadding,
                ).value,
            )
            .fillMaxSize(),
        isRefreshing = timelineState.isRefreshing,
        state = pullToRefreshState,
        onRefresh = {
            timelineStateHolder.accept(
                TimelineState.Action.Tile(
                    tilingAction = TilingState.Action.Refresh,
                ),
            )
        },
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset {
                        IntOffset(x = 0, y = gridState.layoutInfo.beforeContentPadding)
                    },
                state = pullToRefreshState,
                isRefreshing = timelineState.isRefreshing,
            )
        },
    ) {
        LookaheadScope {
            LazyVerticalStaggeredGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        val itemWidth = with(density) {
                            presentation.cardSize.toPx()
                        }
                        timelineStateHolder.accept(
                            TimelineState.Action.Tile(
                                tilingAction = TilingState.Action.GridSize(
                                    numColumns = floor(it.width / itemWidth).roundToInt(),
                                ),
                            ),
                        )
                    },
                state = gridState,
                columns = StaggeredGridCells.Adaptive(presentation.cardSize),
                verticalItemSpacing = presentation.lazyGridVerticalItemSpacing,
                contentPadding = UiTokens.bottomNavAndInsetPaddingValues(
                    top = UiTokens.statusBarHeight + UiTokens.toolbarHeight + UiTokens.tabsHeight,
                ),
                horizontalArrangement = Arrangement.spacedBy(
                    presentation.lazyGridHorizontalItemSpacing,
                ),
                userScrollEnabled = !paneScaffoldState.isTransitionActive,
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
                                    state = videoStates.getOrCreateStateFor(item),
                                ),
                            paneMovableElementSharedTransitionScope = paneScaffoldState,
                            presentationLookaheadScope = this@LookaheadScope,
                            now = remember { Clock.System.now() },
                            item = item,
                            sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
                            presentation = presentation,
                            postActions = remember(timelineState.timeline.sourceId) {
                                postActions(
                                    onLinkTargetClicked = { _, linkTarget ->
                                        if (linkTarget is LinkTarget.Navigable) actions(
                                            Action.Navigate.To(
                                                pathDestination(
                                                    path = linkTarget.path,
                                                    referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                                ),
                                            ),
                                        )
                                    },
                                    onPostClicked = { post: Post, quotingPostUri: PostUri? ->
                                        pendingScrollOffsetState.value =
                                            gridState.pendingOffsetFor(item)
                                        actions(
                                            Action.Navigate.To(
                                                postDestination(
                                                    referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                                    sharedElementPrefix = timelineState.timeline.sharedElementPrefix(
                                                        quotingPostUri = quotingPostUri,
                                                    ),
                                                    post = post,
                                                ),
                                            ),
                                        )
                                    },
                                    onProfileClicked = { profile: Profile, post: Post, quotingPostUri: PostUri? ->
                                        pendingScrollOffsetState.value =
                                            gridState.pendingOffsetFor(item)
                                        actions(
                                            Action.Navigate.To(
                                                profileDestination(
                                                    referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                                    profile = profile,
                                                    avatarSharedElementKey = post
                                                        .avatarSharedElementKey(
                                                            prefix = timelineState.timeline.sharedElementPrefix,
                                                            quotingPostUri = quotingPostUri,
                                                        )
                                                        .takeIf { post.author.did == profile.did },
                                                ),
                                            ),
                                        )
                                    },
                                    onPostMediaClicked = { media: Embed.Media, index: Int, post: Post, quotingPostUri: PostUri? ->
                                        pendingScrollOffsetState.value =
                                            gridState.pendingOffsetFor(item)
                                        actions(
                                            Action.Navigate.To(
                                                galleryDestination(
                                                    post = post,
                                                    media = media,
                                                    startIndex = index,
                                                    sharedElementPrefix = timelineState.timeline.sharedElementPrefix(
                                                        quotingPostUri = quotingPostUri,
                                                    ),
                                                ),
                                            ),
                                        )
                                    },
                                    onReplyToPost = { post: Post ->
                                        pendingScrollOffsetState.value =
                                            gridState.pendingOffsetFor(item)
                                        actions(
                                            Action.Navigate.To(
                                                if (paneScaffoldState.isSignedOut) signInDestination()
                                                else composePostDestination(
                                                    type = Post.Create.Reply(
                                                        parent = post,
                                                    ),
                                                    sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
                                                ),
                                            ),
                                        )
                                    },
                                    onPostInteraction = postInteractionState::onInteraction,
                                )
                            },
                        )
                    },
                )
            }
        }
    }

    if (paneScaffoldState.paneState.pane == ThreePane.Primary) {
        val videoPlayerController = LocalVideoPlayerController.current
        gridState.interpolatedVisibleIndexEffect(
            denominator = 10,
            itemsAvailable = items.size,
        ) { interpolatedIndex ->
            val flooredIndex = floor(interpolatedIndex).toInt()
            val fraction = interpolatedIndex - flooredIndex
            items.getOrNull(flooredIndex)
                ?.takeIf(TimelineItem::canAutoPlayVideo)
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
                TimelineState.Action.Tile(
                    tilingAction = TilingState.Action.LoadAround(
                        query = query ?: timelineState.tilingData.currentQuery,
                    ),
                ),
            )
        },
    )

    gridState.TimelineRefreshEffect(
        timelineState = timelineState,
        onRefresh = {
            animateScrollToItem(
                index = 0,
                scrollOffset = abs(tabsOffset().y.roundToInt()),
            )
        },
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
