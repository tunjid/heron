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

package com.tunjid.heron.posts

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.tunjid.composables.lazy.rememberLazyScrollableState
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.utilities.asGenericUri
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.composePostDestination
import com.tunjid.heron.scaffold.navigation.conversationDestination
import com.tunjid.heron.scaffold.navigation.galleryDestination
import com.tunjid.heron.scaffold.navigation.pathDestination
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.navigation.recordDestination
import com.tunjid.heron.scaffold.navigation.signInDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.timeline.ui.DismissableRefreshIndicator
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.timeline.ui.PostActions
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.post.PostInteractionsSheetState.Companion.rememberUpdatedPostInteractionsSheetState
import com.tunjid.heron.timeline.ui.post.PostOption
import com.tunjid.heron.timeline.ui.post.PostOptionsSheetState.Companion.rememberUpdatedPostOptionsSheetState
import com.tunjid.heron.timeline.ui.post.ThreadGateSheetState.Companion.rememberUpdatedThreadGateSheetState
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.timeline.ui.profile.ProfileRestrictionDialogState.Companion.rememberProfileRestrictionDialogState
import com.tunjid.heron.timeline.ui.sheets.MutedWordsSheetState.Companion.rememberUpdatedMutedWordsSheetState
import com.tunjid.heron.timeline.ui.withQuotingPostUriPrefix
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.canAutoPlayVideo
import com.tunjid.heron.timeline.utilities.cardSize
import com.tunjid.heron.timeline.utilities.lazyGridHorizontalItemSpacing
import com.tunjid.heron.timeline.utilities.lazyGridVerticalItemSpacing
import com.tunjid.heron.timeline.utilities.pendingOffsetFor
import com.tunjid.heron.timeline.utilities.timelineHorizontalPadding
import com.tunjid.heron.ui.UiTokens
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.threepane.ThreePane
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.time.Clock

@Composable
internal fun PostsScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
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
    val items by rememberUpdatedState(state.tilingData.items)
    val density = LocalDensity.current
    val now by remember { mutableStateOf(Clock.System.now()) }
    val videoStates = remember { ThreadedVideoPositionStates(TimelineItem::id) }
    val presentation = remember { Timeline.Presentation.Text.WithEmbed }
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
                        sharedElementPrefix = SharedElementPrefix,
                    ),
                ),
            )
        },
    )
    val threadGateSheetState = rememberUpdatedThreadGateSheetState(
        recentLists = state.recentLists,
        onRequestRecentLists = {
            actions(Action.UpdateRecentLists)
        },
        onThreadGateUpdated = {
            actions(Action.SendPostInteraction(it))
        },
    )
    val mutedWordsSheetState = rememberUpdatedMutedWordsSheetState(
        mutedWordPreferences = state.preferences.mutedWordPreferences,
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
        signedInProfileId = state.signedInProfileId,
        recentConversations = state.recentConversations,
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
        modifier = modifier
            .padding(
                horizontal = animateDpAsState(
                    presentation.timelineHorizontalPadding,
                ).value,
            )
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
                onDismissRequest = {
                    // Handle refresh dismissal if needed
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
                        val numColumns = floor(it.width / itemWidth).roundToInt()
                        if (numColumns > 0) actions(
                            Action.Tile(TilingState.Action.GridSize(numColumns = numColumns)),
                        )
                    },
                state = gridState,
                columns = StaggeredGridCells.Adaptive(presentation.cardSize),
                verticalItemSpacing = presentation.lazyGridVerticalItemSpacing,
                contentPadding = UiTokens.bottomNavAndInsetPaddingValues(
                    top = UiTokens.statusBarHeight + UiTokens.toolbarHeight,
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
                            now = now,
                            item = item,
                            sharedElementPrefix = SharedElementPrefix,
                            showEngagementMetrics = state.preferences.local.showPostEngagementMetrics,
                            presentation = presentation,
                            postActions = remember {
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
                                            pendingScrollOffset = gridState.pendingOffsetFor(item)
                                            actions(
                                                Action.Navigate.To(
                                                    recordDestination(
                                                        referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                        sharedElementPrefix = SharedElementPrefix,
                                                        otherModels = listOfNotNull(action.warnedAppliedLabels),
                                                        record = action.post,
                                                    ),
                                                ),
                                            )
                                        }

                                        is PostAction.OfProfile -> {
                                            pendingScrollOffset = gridState.pendingOffsetFor(item)
                                            actions(
                                                Action.Navigate.To(
                                                    profileDestination(
                                                        referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                        profile = action.profile,
                                                        avatarSharedElementKey = action.post
                                                            .avatarSharedElementKey(
                                                                prefix = SharedElementPrefix,
                                                                quotingPostUri = action.quotingPostUri,
                                                            )
                                                            .takeIf { action.post.author.did == action.profile.did },
                                                    ),
                                                ),
                                            )
                                        }

                                        is PostAction.OfRecord -> {
                                            pendingScrollOffset = gridState.pendingOffsetFor(item)
                                            actions(
                                                Action.Navigate.To(
                                                    recordDestination(
                                                        referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                        sharedElementPrefix = SharedElementPrefix
                                                            .withQuotingPostUriPrefix(action.owningPostUri),
                                                        record = action.record,
                                                    ),
                                                ),
                                            )
                                        }

                                        is PostAction.OfMedia -> {
                                            pendingScrollOffset = gridState.pendingOffsetFor(item)
                                            actions(
                                                Action.Navigate.To(
                                                    galleryDestination(
                                                        post = action.post,
                                                        media = action.media,
                                                        startIndex = action.index,
                                                        sharedElementPrefix = SharedElementPrefix
                                                            .withQuotingPostUriPrefix(action.quotingPostUri),
                                                    ),
                                                ),
                                            )
                                        }

                                        is PostAction.OfReply -> {
                                            pendingScrollOffset = gridState.pendingOffsetFor(item)
                                            actions(
                                                Action.Navigate.To(
                                                    if (paneScaffoldState.isSignedOut) signInDestination()
                                                    else composePostDestination(
                                                        type = Post.Create.Reply(
                                                            parent = action.post,
                                                        ),
                                                        sharedElementPrefix = SharedElementPrefix,
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

    // Auto-play videos for visible items
    if (paneScaffoldState.paneState.pane == ThreePane.Primary && state.preferences.local.autoPlayTimelineVideos) {
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

private const val SharedElementPrefix = "posts"
