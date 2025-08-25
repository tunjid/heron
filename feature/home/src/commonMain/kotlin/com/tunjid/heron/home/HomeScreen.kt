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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.composables.accumulatedoffsetnestedscrollconnection.rememberAccumulatedOffsetNestedScrollConnection
import com.tunjid.composables.lazy.pendingScrollOffsetState
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.composePostDestination
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
import com.tunjid.heron.timeline.utilities.pendingOffsetFor
import com.tunjid.heron.timeline.utilities.sharedElementPrefix
import com.tunjid.heron.timeline.utilities.timelineHorizontalPadding
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.tabIndex
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.threepane.ThreePane
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
        updatedTimelineStateHolders.size
    }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier,
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
            key = { page ->
                updatedTimelineStateHolders[page]
                    .state
                    .value
                    .timeline
                    .sourceId
            },
            pageContent = { page ->
                val timelineStateHolder = updatedTimelineStateHolders[page]
                HomeTimeline(
                    paneScaffoldState = paneScaffoldState,
                    timelineStateHolder = timelineStateHolder,
                    actions = actions,
                )
            },
        )
        HomeTabs(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .offset {
                    tabsOffsetNestedScrollConnection.offset.round()
                },
            sharedTransitionScope = paneScaffoldState,
            selectedTabIndex = pagerState::tabIndex,
            saveRequestId = state.timelinePreferenceSaveRequestId,
            currentSourceId = state.currentSourceId,
            isSignedIn = state.signedInProfile != null,
            isExpanded = state.timelinePreferencesExpanded,
            timelines = state.timelines,
            sourceIdsToHasUpdates = state.sourceIdsToHasUpdates,
            scrollToPage = {
                scope.launch {
                    pagerState.animateScrollToPage(it)
                }
            },
            onRefreshTabClicked = { page ->
                updatedTimelineStateHolders
                    .getOrNull(page)
                    ?.accept
                    ?.invoke(
                        TimelineState.Action.Tile(
                            tilingAction = TilingState.Action.Refresh,
                        ),
                    )
            },
            onExpansionChanged = { isExpanded ->
                actions(Action.SetPreferencesExpanded(isExpanded = isExpanded))
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

        tabsOffsetNestedScrollConnection.timelinePreferenceExpansionEffect(
            isExpanded = state.timelinePreferencesExpanded,
        )

        LaunchedEffect(Unit) {
            snapshotFlow { pagerState.currentPage }
                .collect { page ->
                    if (page < updatedTimelineStateHolders.size) {
                        actions(
                            Action.SetCurrentTab(
                                updatedTimelineStateHolders[page]
                                    .state
                                    .value
                                    .timeline
                                    .sourceId,
                            ),
                        )
                    }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeTimeline(
    paneScaffoldState: PaneScaffoldState,
    timelineStateHolder: TimelineStateHolder,
    actions: (Action) -> Unit,
) {
    val gridState = rememberLazyStaggeredGridState()
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
                    .align(Alignment.TopCenter),
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
                verticalItemSpacing = 8.dp,
                contentPadding = UiTokens.bottomNavAndInsetPaddingValues(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                        if (linkTarget is LinkTarget.Navigable) {
                                            actions(
                                                Action.Navigate.To(
                                                    pathDestination(
                                                        path = linkTarget.path,
                                                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                                    ),
                                                ),
                                            )
                                        }
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
                                                if (paneScaffoldState.isSignedOut) {
                                                    signInDestination()
                                                } else {
                                                    composePostDestination(
                                                        type = Post.Create.Reply(
                                                            parent = post,
                                                        ),
                                                        sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
                                                    )
                                                },
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
        onRefresh = { animateScrollToItem(index = 0) },
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
