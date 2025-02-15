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
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.domain.timeline.TimelineLoadAction
import com.tunjid.heron.domain.timeline.TimelineStateHolder
import com.tunjid.heron.domain.timeline.TimelineStatus
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.paneClip
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.effects.TimelineRefreshEffect
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.timeline.ui.rememberPostActions
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.tiler.compose.PivotedTilingEffect
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
                panedSharedElementScope = paneScaffoldState,
                timelineStateHolder = timelineStateHolder,
                actions = actions,
            )
        }
    }
}

@Composable
private fun FeedTimeline(
    panedSharedElementScope: PanedSharedElementScope,
    timelineStateHolder: TimelineStateHolder,
    actions: (Action) -> Unit,
) {
    val gridState = rememberLazyStaggeredGridState()
    val timelineState by timelineStateHolder.state.collectAsStateWithLifecycle()
    val items by rememberUpdatedState(timelineState.items)

    val postActions = rememberPostActions(
        onPostClicked = { post: Post, _ ->
            actions(
                Action.Navigate.DelegateTo(
                    NavigationAction.Common.ToPost(
                        referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                        sharedElementPrefix = timelineState.timeline.sourceId,
                        post = post,
                    )
                )
            )
        },
        onProfileClicked = { profile: Profile, post: Post, _ ->
            actions(
                Action.Navigate.DelegateTo(
                    NavigationAction.Common.ToProfile(
                        referringRouteOption = NavigationAction.ReferringRouteOption.Parent,
                        profile = profile,
                        avatarSharedElementKey = post.avatarSharedElementKey(
                            prefix = timelineState.timeline.sourceId,
                        ).takeIf { post.author.did == profile.did }
                    )
                )
            )
        },
        onPostMediaClicked = { media: Embed.Media, index: Int, post: Post, _ ->
            actions(
                Action.Navigate.DelegateTo(
                    NavigationAction.Common.ToMedia(
                        post = post,
                        media = media,
                        startIndex = index,
                        sharedElementPrefix = timelineState.timeline.sourceId,
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
                        sharedElementPrefix = timelineState.timeline.sourceId,
                    )
                )
            )
        },
        onPostInteraction = {
            actions(
                Action.SendPostInteraction(it)
            )
        }
    )

    val density = LocalDensity.current
    val videoStates = remember { ThreadedVideoPositionStates() }

    PullToRefreshBox(
        modifier = Modifier
            .pullToRefresh(
                isRefreshing = timelineState.status is TimelineStatus.Refreshing,
                state = rememberPullToRefreshState(),
                onRefresh = { timelineStateHolder.accept(TimelineLoadAction.Refresh) }
            )
            .padding(horizontal = 8.dp)
            .fillMaxSize()
            .paneClip()
            .onSizeChanged {
                timelineStateHolder.accept(
                    TimelineLoadAction.GridSize(
                        floor(it.width / with(density) { CardSize.toPx() }).roundToInt()
                    )
                )
            },
        isRefreshing = timelineState.status is TimelineStatus.Refreshing,
        state = rememberPullToRefreshState(),
        onRefresh = { timelineStateHolder.accept(TimelineLoadAction.Refresh) }
    ) {
        LazyVerticalStaggeredGrid(
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
                        postActions = postActions,
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
}

private val CardSize = 340.dp
