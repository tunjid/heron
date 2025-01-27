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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
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
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.scaffold.StatusBarHeight
import com.tunjid.heron.scaffold.scaffold.ToolbarHeight
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.effects.TimelineRefreshEffect
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.threepane.ThreePane
import kotlinx.datetime.Clock
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
internal fun FeedScreen(
    panedSharedElementScope: PanedSharedElementScope,
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
                panedSharedElementScope = panedSharedElementScope,
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

    val onPostClicked = remember {
        { post: Post ->
            actions(
                Action.Navigate.DelegateTo(
                    NavigationAction.Common.ToPost(
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                        sharedElementPrefix = timelineState.timeline.sourceId,
                        post = post,
                    )
                )
            )
        }
    }
    val onProfileClicked = remember {
        { post: Post, profile: Profile ->
            actions(
                Action.Navigate.DelegateTo(
                    NavigationAction.Common.ToProfile(
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                        profile = profile,
                        avatarSharedElementKey = post.avatarSharedElementKey(
                            prefix = timelineState.timeline.sourceId,
                        ).takeIf { post.author.did == profile.did }
                    )
                )
            )
        }
    }
    val onPostMediaClicked = remember {
        { post: Post, media: Embed.Media, index: Int ->
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
        }
    }
    val onReplyToPost = remember {
        { item: TimelineItem ->
            actions(
                Action.Navigate.DelegateTo(
                    NavigationAction.Common.ComposePost(
                        type = Post.Create.Reply(
                            parent = item.post,
                            root = when (item) {
                                is TimelineItem.Pinned -> item.post
                                is TimelineItem.Repost -> item.post
                                is TimelineItem.Single -> item.post
                                is TimelineItem.Thread -> item.posts.first()
                            }
                        ),
                        sharedElementPrefix = timelineState.timeline.sourceId,
                    )
                )
            )
        }
    }

    val density = LocalDensity.current
    val videoStates = remember { ThreadedVideoPositionStates() }

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
        contentPadding = PaddingValues(
            top = StatusBarHeight + ToolbarHeight,
            start = 8.dp,
            end = 8.dp,
        ),
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
                    onPostClicked = onPostClicked,
                    onProfileClicked = onProfileClicked,
                    onPostMediaClicked = onPostMediaClicked,
                    onReplyToPost = {
                        onReplyToPost(item)
                    },
                    onPostInteraction = {
                        actions(
                            Action.SendPostInteraction(it)
                        )
                    },
                )
            }
        )
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
