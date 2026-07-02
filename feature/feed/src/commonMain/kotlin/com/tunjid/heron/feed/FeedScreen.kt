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

package com.tunjid.heron.feed

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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.core.models.sourceId
import com.tunjid.heron.data.utilities.asGenericUri
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.sheets.postoptions.PostOption
import com.tunjid.heron.sheets.profile.ProfileRestrictionDialogState.Companion.rememberProfileRestrictionDialogState
import com.tunjid.heron.sheets.rememberMutedWordsSheetState
import com.tunjid.heron.sheets.rememberPostInteractionsSheetState
import com.tunjid.heron.sheets.rememberPostOptionsSheetState
import com.tunjid.heron.sheets.rememberTimelineThreadGateSheetState
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
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.canAutoPlayVideo
import com.tunjid.heron.timeline.utilities.contentType
import com.tunjid.heron.timeline.utilities.rememberTimelineDisplayState
import com.tunjid.heron.timeline.utilities.sharedElementPrefix
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.modifiers.gridColumnCount
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.composePostDestination
import com.tunjid.heron.ui.scaffold.navigation.conversationDestination
import com.tunjid.heron.ui.scaffold.navigation.galleryDestination
import com.tunjid.heron.ui.scaffold.navigation.pathDestination
import com.tunjid.heron.ui.scaffold.navigation.profileDestination
import com.tunjid.heron.ui.scaffold.navigation.recordDestination
import com.tunjid.heron.ui.scaffold.navigation.signInDestination
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.paneClip
import com.tunjid.mutator.compose.produceStateWithLifecycle
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.threepane.ThreePane
import kotlin.math.floor
import kotlin.time.Clock
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

@Composable
internal fun FeedScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        when (val timelineStateHolder = state.timelineStateHolder) {
            null -> Unit
            else -> FeedTimeline(
                scrollToTopRequestId = state.scrollToTopRequestId,
                paneScaffoldState = paneScaffoldState,
                timelineStateHolder = timelineStateHolder,
                actions = actions,
                autoPlayTimelineVideos = state.preferences.local.autoPlayTimelineVideos,
                showEngagementMetrics = state.preferences.local.showPostEngagementMetrics,
            )
        }
    }
}

@Composable
private fun FeedTimeline(
    scrollToTopRequestId: String?,
    paneScaffoldState: PaneScaffoldState,
    timelineStateHolder: TimelineStateHolder,
    actions: (Action) -> Unit,
    autoPlayTimelineVideos: Boolean,
    showEngagementMetrics: Boolean,
) {
    val gridState = rememberLazyStaggeredGridState()
    val timelineState = timelineStateHolder.produceStateWithLifecycle()
    val items = timelineState.tiledItems

    val now = remember(timelineState.timeline.lastRefreshed) { Clock.System.now() }
    val density = LocalDensity.current
    val videoStates = remember { ThreadedVideoPositionStates(TimelineItem::id) }
    val presentation = timelineState.timeline.presentation
    val displayState = rememberTimelineDisplayState()
    val pullToRefreshState = rememberPullToRefreshState()
    val postInteractionSheetState = paneScaffoldState.rememberPostInteractionsSheetState(
        sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
    )
    val threadGateSheetState = paneScaffoldState.rememberTimelineThreadGateSheetState()
    val mutedWordsSheetState = paneScaffoldState.rememberMutedWordsSheetState()
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
    val postOptionsSheetState = paneScaffoldState.rememberPostOptionsSheetState(
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
                is PostOption.Delete -> actions(Action.DeleteRecord(option.postUri))
            }
        },
    )

    PullToRefreshBox(
        modifier = Modifier
            .padding(
                horizontal = animateDpAsState(
                    displayState.horizontalPadding(presentation),
                ).value,
            )
            .fillMaxSize()
            .paneClip()
            .gridColumnCount(
                density = density,
                maxColumnWidth = displayState.cardSize(presentation),
            ) { numColumns ->
                timelineStateHolder.accept(
                    TimelineState.Action.Tile(
                        tilingAction = TilingState.Action.GridSize(
                            numColumns = numColumns,
                        ),
                    ),
                )
            },
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
                state = gridState,
                columns = StaggeredGridCells.Adaptive(displayState.cardSize(presentation)),
                verticalItemSpacing = displayState.verticalItemSpacing(presentation),
                contentPadding = UiTokens.bottomNavAndInsetPaddingValues(
                    top = UiTokens.statusBarHeight + UiTokens.toolbarHeight,
                    isCompact = paneScaffoldState.prefersCompactBottomNav,
                ),
                horizontalArrangement = Arrangement.spacedBy(
                    displayState.horizontalItemSpacing(presentation),
                ),
                userScrollEnabled = !paneScaffoldState.isTransitionActive,
            ) {
                items(
                    items = items,
                    key = TimelineItem::id,
                    contentType = TimelineItem::contentType,
                    itemContent = { item ->
                        TimelineItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .threadedVideoPosition(
                                    state = videoStates.getOrCreateStateFor(item),
                                ),
                            paneTransitionScope = paneScaffoldState,
                            presentationLookaheadScope = this@LookaheadScope,
                            now = now,
                            item = item,
                            sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
                            showEngagementMetrics = showEngagementMetrics,
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
                                                        referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                    ),
                                                ),
                                            )
                                        }

                                        is PostAction.OfPost -> {
                                            actions(
                                                Action.Navigate.To(
                                                    recordDestination(
                                                        referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                        sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
                                                        otherModels = buildList {
                                                            action.warnedAppliedLabels?.let(::add)
                                                            if (action.isMainPost) {
                                                                add(timelineState.timeline.source)
                                                                add(timelineState.tilingData.currentQuery.data)
                                                            }
                                                        },
                                                        record = action.post,
                                                    ),
                                                ),
                                            )
                                        }

                                        is PostAction.OfProfile -> {
                                            actions(
                                                Action.Navigate.To(
                                                    profileDestination(
                                                        referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                        profile = action.profile,
                                                        avatarSharedElementKey = action.post
                                                            .avatarSharedElementKey(
                                                                prefix = timelineState.timeline.sourceId,
                                                                quotingPostUri = action.quotingPostUri,
                                                            )
                                                            .takeIf { action.post.author.did == action.profile.did },
                                                    ),
                                                ),
                                            )
                                        }

                                        is PostAction.OfRecord -> {
                                            actions(
                                                Action.Navigate.To(
                                                    recordDestination(
                                                        referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                        sharedElementPrefix = timelineState.timeline.sharedElementPrefix(
                                                            quotingPostUri = action.owningPostUri,
                                                        ),
                                                        record = action.record,
                                                    ),
                                                ),
                                            )
                                        }

                                        is PostAction.OfMedia -> {
                                            actions(
                                                Action.Navigate.To(
                                                    galleryDestination(
                                                        post = action.post,
                                                        media = action.media,
                                                        startIndex = action.index,
                                                        sharedElementPrefix = timelineState.timeline.sharedElementPrefix(
                                                            quotingPostUri = action.quotingPostUri,
                                                        ),
                                                        otherModels = when {
                                                            action.isMainPost -> listOf(
                                                                timelineState.timeline.source,
                                                                timelineState.tilingData.currentQuery.data,
                                                            )
                                                            else -> emptyList()
                                                        },
                                                    ),
                                                ),
                                            )
                                        }

                                        is PostAction.OfReply -> {
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

                                        is PostAction.OfPublicationSubscription ->
                                            actions(Action.TogglePublicationSubscription(action.publication))

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

    if (paneScaffoldState.paneState.pane == ThreePane.Primary && autoPlayTimelineVideos) {
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

    val currentScrollToTopRequestId = rememberUpdatedState(scrollToTopRequestId)
    LaunchedEffect(Unit) {
        snapshotFlow {
            currentScrollToTopRequestId.value
        }
            .drop(1)
            .collectLatest { requestId ->
                if (requestId != null) gridState.animateScrollToItem(0)
            }
    }
}
