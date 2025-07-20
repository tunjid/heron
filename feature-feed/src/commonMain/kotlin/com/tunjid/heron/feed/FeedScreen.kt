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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.composables.lazy.pendingScrollOffsetState
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavigationAction
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
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.timeline.ui.rememberPostActions
import com.tunjid.heron.timeline.utilities.cardSize
import com.tunjid.heron.timeline.utilities.pendingOffsetFor
import com.tunjid.heron.timeline.utilities.sharedElementPrefix
import com.tunjid.heron.timeline.utilities.timelineHorizontalPadding
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import com.tunjid.treenav.compose.threepane.ThreePane
import kotlinx.datetime.Clock
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
internal fun FeedScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        when (val timelineStateHolder = state.timelineStateHolder) {
            null -> Unit
            else -> FeedTimeline(
                paneMovableElementSharedTransitionScope = paneScaffoldState,
                timelineStateHolder = timelineStateHolder,
                actions = actions,
            )
        }
    }
}

@Composable
private fun FeedTimeline(
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    timelineStateHolder: TimelineStateHolder,
    actions: (Action) -> Unit,
) {
    val gridState = rememberLazyStaggeredGridState()
    val timelineState by timelineStateHolder.state.collectAsStateWithLifecycle()
    val items by rememberUpdatedState(timelineState.tiledItems)
    val pendingScrollOffsetState = gridState.pendingScrollOffsetState()

    val density = LocalDensity.current
    val videoStates = remember { ThreadedVideoPositionStates() }
    val presentation = timelineState.timeline.presentation

    PullToRefreshBox(
        modifier = Modifier
            .padding(
                horizontal = animateDpAsState(
                    presentation.timelineHorizontalPadding
                ).value
            )
            .fillMaxSize()
            .paneClip()
            .onSizeChanged {
                val itemWidth = with(density) {
                    presentation.cardSize.toPx()
                }
                timelineStateHolder.accept(
                    TimelineState.Action.Tile(
                        tilingAction = TilingState.Action.GridSize(
                            numColumns = floor(it.width / itemWidth).roundToInt()
                        )
                    )
                )
            },
        isRefreshing = timelineState.isRefreshing,
        state = rememberPullToRefreshState(),
        onRefresh = {
            timelineStateHolder.accept(
                TimelineState.Action.Tile(
                    tilingAction = TilingState.Action.Refresh
                )
            )
        }
    ) {
        LookaheadScope {
            LazyVerticalStaggeredGrid(
                state = gridState,
                columns = StaggeredGridCells.Adaptive(presentation.cardSize),
                verticalItemSpacing = 8.dp,
                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = !paneMovableElementSharedTransitionScope.isTransitionActive,
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
                            paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                            presentationLookaheadScope = this@LookaheadScope,
                            now = remember { Clock.System.now() },
                            item = item,
                            sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
                            presentation = presentation,
                            postActions = rememberPostActions(
                                onPostClicked = { post: Post, quotingPostId: PostId? ->
                                    pendingScrollOffsetState.value =
                                        gridState.pendingOffsetFor(item)
                                    actions(
                                        Action.Navigate.DelegateTo(
                                            NavigationAction.Common.ToPost(
                                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                sharedElementPrefix = timelineState.timeline.sharedElementPrefix(
                                                    quotingPostId = quotingPostId,
                                                ),
                                                post = post,
                                            )
                                        )
                                    )
                                },
                                onProfileClicked = { profile: Profile, post: Post, quotingPostId: PostId? ->
                                    pendingScrollOffsetState.value =
                                        gridState.pendingOffsetFor(item)
                                    actions(
                                        Action.Navigate.DelegateTo(
                                            NavigationAction.Common.ToProfile(
                                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
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
                                onPostMediaClicked = { media: Embed.Media, index: Int, post: Post, quotingPostId: PostId? ->
                                    pendingScrollOffsetState.value =
                                        gridState.pendingOffsetFor(item)
                                    actions(
                                        Action.Navigate.DelegateTo(
                                            NavigationAction.Common.ToMedia(
                                                post = post,
                                                media = media,
                                                startIndex = index,
                                                sharedElementPrefix = timelineState.timeline.sharedElementPrefix(
                                                    quotingPostId = quotingPostId,
                                                ),
                                            )
                                        )
                                    )
                                },
                                onReplyToPost = { post: Post ->
                                    pendingScrollOffsetState.value =
                                        gridState.pendingOffsetFor(item)
                                    actions(
                                        Action.Navigate.DelegateTo(
                                            NavigationAction.Common.ComposePost(
                                                type = Post.Create.Reply(
                                                    parent = post,
                                                ),
                                                sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
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
    }
    if (paneMovableElementSharedTransitionScope.paneState.pane == ThreePane.Primary) {
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
                TimelineState.Action.Tile(
                    tilingAction = TilingState.Action.LoadAround(
                        query = query ?: timelineState.tilingData.currentQuery
                    )
                )
            )
        }
    )

    gridState.TimelineRefreshEffect(
        timelineState = timelineState,
        onRefresh = { animateScrollToItem(index = 0) }
    )
}
