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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.tunjid.composables.lazy.rememberLazyScrollableState
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.core.models.uri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.utilities.asGenericUri
import com.tunjid.heron.data.utilities.path
import com.tunjid.heron.home.ui.RestoreLastViewedTabEffect
import com.tunjid.heron.home.ui.TabsCollapseEffect
import com.tunjid.heron.home.ui.TabsExpansionEffect
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.bookmarksDestination
import com.tunjid.heron.scaffold.navigation.composePostDestination
import com.tunjid.heron.scaffold.navigation.conversationDestination
import com.tunjid.heron.scaffold.navigation.galleryDestination
import com.tunjid.heron.scaffold.navigation.pathDestination
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.navigation.recordDestination
import com.tunjid.heron.scaffold.navigation.settingsDestination
import com.tunjid.heron.scaffold.navigation.signInDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.paneClip
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.isRefreshing
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.state.TimelineStateHolder
import com.tunjid.heron.timeline.ui.DismissableRefreshIndicator
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.timeline.ui.PostActions
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.effects.TimelineRefreshEffect
import com.tunjid.heron.timeline.ui.post.PostInteractionsSheetState.Companion.rememberUpdatedPostInteractionsSheetState
import com.tunjid.heron.timeline.ui.post.PostOption
import com.tunjid.heron.timeline.ui.post.PostOptionsSheetState.Companion.rememberUpdatedPostOptionsSheetState
import com.tunjid.heron.timeline.ui.post.ThreadGateSheetState.Companion.rememberUpdatedThreadGateSheetState
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.timeline.ui.profile.ProfileRestrictionDialogState.Companion.rememberProfileRestrictionDialogState
import com.tunjid.heron.timeline.ui.sheets.MutedWordsSheetState.Companion.rememberUpdatedMutedWordsSheetState
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
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
import kotlin.time.Clock
import kotlinx.coroutines.launch

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
                var pendingScrollOffset by rememberSaveable { mutableIntStateOf(0) }
                val gridState = rememberLazyScrollableState(
                    init = ::LazyStaggeredGridState,
                    firstVisibleItemIndex = LazyStaggeredGridState::firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = LazyStaggeredGridState::firstVisibleItemScrollOffset,
                    restore = { firstVisibleItemIndex, firstVisibleItemScrollOffset ->
                        LazyStaggeredGridState(
                            initialFirstVisibleItemIndex = firstVisibleItemIndex,
                            initialFirstVisibleItemOffset = firstVisibleItemScrollOffset + pendingScrollOffset,
                        )
                    },
                )
                val timelineStateHolder = updatedTimelineStateHolders[page]
                HomeTimeline(
                    gridState = gridState,
                    paneScaffoldState = paneScaffoldState,
                    signedInProfileId = state.signedInProfile?.did,
                    mutedWordsPreferences = state.preferences.mutedWordPreferences,
                    recentConversations = state.recentConversations,
                    timelineStateHolder = timelineStateHolder,
                    tabsOffset = tabsOffsetNestedScrollConnection::offset,
                    updatePendingScrollState = { pendingScrollOffset = it },
                    actions = actions,
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
            onBookmarkIconClick = {
                actions(Action.Navigate.To(bookmarksDestination()))
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

@Composable
private fun HomeTimeline(
    gridState: LazyStaggeredGridState,
    paneScaffoldState: PaneScaffoldState,
    signedInProfileId: ProfileId?,
    mutedWordsPreferences: List<MutedWordPreference>,
    recentConversations: List<Conversation>,
    timelineStateHolder: TimelineStateHolder,
    tabsOffset: () -> Offset,
    updatePendingScrollState: (Int) -> Unit,
    actions: (Action) -> Unit,
) {
    val timelineState by timelineStateHolder.state.collectAsStateWithLifecycle()
    val items by rememberUpdatedState(timelineState.tiledItems)

    val density = LocalDensity.current
    val videoStates = remember { ThreadedVideoPositionStates(TimelineItem::id) }
    val presentation = timelineState.timeline.presentation
    val pullToRefreshState = rememberPullToRefreshState()
    val postInteractionSheetState = rememberUpdatedPostInteractionsSheetState(
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
    val threadGateSheetState = rememberUpdatedThreadGateSheetState(
        onThreadGateUpdated = {
            actions(Action.SendPostInteraction(it))
        },
    )
    val mutedWordsSheetState = rememberUpdatedMutedWordsSheetState(
        mutedWordPreferences = mutedWordsPreferences,
        onSave = {
            actions(Action.UpdateMutedWord(it))
        },
        onShown = {},
    )
    val profileRestrictionDialogState = rememberProfileRestrictionDialogState(
        onProfileRestricted = { profileRestriction ->
            when (profileRestriction) {
                is PostOption.Moderation.BlockAccount ->
                    actions(
                        Action.BlockAccount(
                            signedInProfileId = profileRestriction.signedInProfileId,
                            profileId = profileRestriction.post.author.did,
                        ),
                    )

                is PostOption.Moderation.MuteAccount ->
                    actions(
                        Action.MuteAccount(
                            signedInProfileId = profileRestriction.signedInProfileId,
                            profileId = profileRestriction.post.author.did,
                        ),
                    )
            }
        },
    )
    val postOptionsSheetState = rememberUpdatedPostOptionsSheetState(
        signedInProfileId = signedInProfileId,
        recentConversations = recentConversations,
        onOptionClicked = { option ->
            when (option) {
                is PostOption.ShareInConversation ->
                    actions(
                        Action.Navigate.To(
                            conversationDestination(
                                id = option.conversation.id,
                                members = option.conversation.members,
                                sharedElementPrefix = option.conversation.id.id,
                                sharedUri = option.post.uri.asGenericUri(),
                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                            ),
                        ),
                    )

                is PostOption.ThreadGate ->
                    items.firstOrNull { it.post.uri == option.postUri }
                        ?.let(threadGateSheetState::show)

                is PostOption.Moderation.BlockAccount ->
                    profileRestrictionDialogState.show(option)

                is PostOption.Moderation.MuteAccount ->
                    profileRestrictionDialogState.show(option)

                is PostOption.Moderation.MuteWords -> mutedWordsSheetState.show()
            }
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
            DismissableRefreshIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset {
                        IntOffset(x = 0, y = gridState.layoutInfo.beforeContentPadding)
                    },
                state = pullToRefreshState,
                isRefreshing = timelineState.isRefreshing,
                onDismissRequest = {
                    timelineStateHolder.accept(TimelineState.Action.DismissRefresh)
                },
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
                    isCompact = paneScaffoldState.prefersCompactBottomNav,
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
                                PostActions { action ->
                                    when (action) {
                                        is PostAction.OfLinkTarget -> {
                                            val linkTarget = action.linkTarget
                                            if (linkTarget is LinkTarget.Navigable) actions(
                                                Action.Navigate.To(
                                                    pathDestination(
                                                        path = linkTarget.path,
                                                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                                    ),
                                                ),
                                            )
                                        }

                                        is PostAction.OfPost -> {
                                            updatePendingScrollState(gridState.pendingOffsetFor(item))
                                            actions(
                                                Action.Navigate.To(
                                                    recordDestination(
                                                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                                        sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
                                                        otherModels = listOfNotNull(action.warnedAppliedLabels),
                                                        record = action.post,
                                                    ),
                                                ),
                                            )
                                        }

                                        is PostAction.OfProfile -> {
                                            updatePendingScrollState(gridState.pendingOffsetFor(item))
                                            actions(
                                                Action.Navigate.To(
                                                    profileDestination(
                                                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                                        profile = action.profile,
                                                        avatarSharedElementKey = action.post
                                                            .avatarSharedElementKey(
                                                                prefix = timelineState.timeline.sharedElementPrefix,
                                                                quotingPostUri = action.quotingPostUri,
                                                            )
                                                            .takeIf { action.post.author.did == action.profile.did },
                                                    ),
                                                ),
                                            )
                                        }

                                        is PostAction.OfRecord -> {
                                            updatePendingScrollState(gridState.pendingOffsetFor(item))
                                            actions(
                                                Action.Navigate.To(
                                                    recordDestination(
                                                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                                        sharedElementPrefix = timelineState.timeline.sharedElementPrefix(
                                                            quotingPostUri = action.owningPostUri,
                                                        ),
                                                        record = action.record,
                                                    ),
                                                ),
                                            )
                                        }

                                        is PostAction.OfMedia -> {
                                            updatePendingScrollState(gridState.pendingOffsetFor(item))
                                            actions(
                                                Action.Navigate.To(
                                                    galleryDestination(
                                                        post = action.post,
                                                        media = action.media,
                                                        startIndex = action.index,
                                                        sharedElementPrefix = timelineState.timeline.sharedElementPrefix(
                                                            quotingPostUri = action.quotingPostUri,
                                                        ),
                                                    ),
                                                ),
                                            )
                                        }

                                        is PostAction.OfReply -> {
                                            updatePendingScrollState(gridState.pendingOffsetFor(item))
                                            actions(
                                                Action.Navigate.To(
                                                    if (paneScaffoldState.isSignedOut) signInDestination()
                                                    else composePostDestination(
                                                        type = Post.Create.Reply(
                                                            parent = action.post,
                                                        ),
                                                        sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
                                                    ),
                                                ),
                                            )
                                        }

                                        is PostAction.OfInteraction -> {
                                            postInteractionSheetState.onInteraction(action)
                                        }

                                        is PostAction.OfMore -> {
                                            postOptionsSheetState.showOptions(action.post)
                                        }

                                        else -> Unit
                                    }
                                }
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
