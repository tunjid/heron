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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.tunjid.composables.lazy.pendingScrollOffsetState
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.core.types.PostUri
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
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.post.PostInteractionsSheetState.Companion.rememberUpdatedPostInteractionState
import com.tunjid.heron.timeline.ui.post.PostOptionsSheetState.Companion.rememberUpdatedPostOptionsState
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.timeline.ui.postActions
import com.tunjid.heron.timeline.ui.withQuotingPostUriPrefix
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
import kotlinx.datetime.Clock

@Composable
internal fun PostsScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyStaggeredGridState()
    val density = LocalDensity.current
    val now by remember { mutableStateOf(Clock.System.now()) }
    val videoStates = remember { ThreadedVideoPositionStates(TimelineItem::id) }
    val presentation = remember { Timeline.Presentation.Text.WithEmbed }
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
                        sharedElementPrefix = SharedElementPrefix,
                    ),
                ),
            )
        },
    )

    val postOptionsState = rememberUpdatedPostOptionsState(
        signedInProfileId = state.signedInProfileId,
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

    val pendingScrollOffsetState = gridState.pendingScrollOffsetState()
    val items by rememberUpdatedState(state.tilingData.items)

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
                            presentation = presentation,
                            postActions = remember {
                                postActions(
                                    onLinkTargetClicked = { _, linkTarget ->
                                        if (linkTarget is LinkTarget.Navigable) actions(
                                            Action.Navigate.To(
                                                pathDestination(
                                                    path = linkTarget.path,
                                                    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                ),
                                            ),
                                        )
                                    },
                                    onPostClicked = { post: Post ->
                                        pendingScrollOffsetState.value =
                                            gridState.pendingOffsetFor(item)
                                        actions(
                                            Action.Navigate.To(
                                                recordDestination(
                                                    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                    sharedElementPrefix = SharedElementPrefix,
                                                    record = post,
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
                                                    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                    profile = profile,
                                                    avatarSharedElementKey = post
                                                        .avatarSharedElementKey(
                                                            prefix = SharedElementPrefix,
                                                            quotingPostUri = quotingPostUri,
                                                        )
                                                        .takeIf { post.author.did == profile.did },
                                                ),
                                            ),
                                        )
                                    },
                                    onPostRecordClicked = { record, owningPostUri ->
                                        pendingScrollOffsetState.value =
                                            gridState.pendingOffsetFor(item)
                                        actions(
                                            Action.Navigate.To(
                                                recordDestination(
                                                    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                    sharedElementPrefix = SharedElementPrefix
                                                        .withQuotingPostUriPrefix(owningPostUri),
                                                    record = record,
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
                                                    sharedElementPrefix = SharedElementPrefix
                                                        .withQuotingPostUriPrefix(quotingPostUri),
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
                                                    sharedElementPrefix = SharedElementPrefix,
                                                ),
                                            ),
                                        )
                                    },
                                    onPostInteraction = postInteractionState::onInteraction,
                                    onPostOptionsClicked = postOptionsState::showOptions,
                                )
                            },
                        )
                    },
                )
            }
        }
    }

    // Auto-play videos for visible items
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
