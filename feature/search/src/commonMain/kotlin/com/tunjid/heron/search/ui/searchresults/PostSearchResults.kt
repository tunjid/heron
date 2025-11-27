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

package com.tunjid.heron.search.ui.searchresults

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.search.SearchResult
import com.tunjid.heron.search.SearchState
import com.tunjid.heron.search.canAutoPlayVideo
import com.tunjid.heron.search.id
import com.tunjid.heron.search.sharedElementPrefix
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.ui.PostActions
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.timeline.ui.postActions
import com.tunjid.heron.timeline.ui.withQuotingPostUriPrefix
import com.tunjid.heron.timeline.utilities.cardSize
import com.tunjid.heron.timeline.utilities.lazyGridHorizontalItemSpacing
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.UiTokens.bottomNavAndInsetPaddingValues
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import com.tunjid.treenav.compose.threepane.ThreePane
import kotlin.math.floor
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Composable
internal fun PostSearchResults(
    state: SearchState.OfPosts,
    gridState: LazyStaggeredGridState,
    modifier: Modifier,
    videoStates: ThreadedVideoPositionStates<SearchResult.OfPost>,
    paneScaffoldState: PaneScaffoldState,
    onLinkTargetClicked: (LinkTarget) -> Unit,
    onPostSearchResultProfileClicked: (profile: Profile, post: Post, sharedElementPrefix: String) -> Unit,
    onPostSearchResultClicked: (post: Post, sharedElementPrefix: String) -> Unit,
    onReplyToPost: (post: Post, sharedElementPrefix: String) -> Unit,
    onPostRecordClicked: (record: Record, sharedElementPrefix: String) -> Unit,
    onMediaClicked: (media: Embed.Media, index: Int, post: Post, sharedElementPrefix: String) -> Unit,
    onPostInteraction: (Post.Interaction) -> Unit,
    onPostOptionsClicked: (Post) -> Unit,
    searchResultActions: (SearchState.Tile) -> Unit,
) {
    val now = remember { Clock.System.now() }
    val results by rememberUpdatedState(state.tiledItems)
    val sharedElementPrefix = state.sharedElementPrefix
    val postActions = remember(
        sharedElementPrefix,
        onLinkTargetClicked,
        onPostSearchResultClicked,
        onPostSearchResultProfileClicked,
        onPostRecordClicked,
        onMediaClicked,
        onReplyToPost,
        onPostInteraction,
        onPostOptionsClicked,
    ) {
        postActions(
            onLinkTargetClicked = { _, linkTarget ->
                onLinkTargetClicked(linkTarget)
            },
            onPostClicked = { post ->
                onPostSearchResultClicked(post, sharedElementPrefix)
            },
            onProfileClicked = { profile, post, quotingPostUri ->
                onPostSearchResultProfileClicked(
                    profile,
                    post,
                    sharedElementPrefix.withQuotingPostUriPrefix(quotingPostUri),
                )
            },
            onPostRecordClicked = { record, owningPostUri ->
                onPostRecordClicked(
                    record,
                    sharedElementPrefix.withQuotingPostUriPrefix(owningPostUri),
                )
            },
            onPostMediaClicked = { media, index, post, quotingPostUri ->
                onMediaClicked(
                    media,
                    index,
                    post,
                    sharedElementPrefix.withQuotingPostUriPrefix(quotingPostUri),
                )
            },
            onReplyToPost = { post ->
                onReplyToPost(post, sharedElementPrefix)
            },
            onPostInteraction = onPostInteraction,
            onPostOptionsClicked = onPostOptionsClicked,
        )
    }
    LazyVerticalStaggeredGrid(
        modifier = modifier,
        state = gridState,
        columns = StaggeredGridCells.Adaptive(
            Timeline.Presentation.Text.WithEmbed.cardSize,
        ),
        verticalItemSpacing = 16.dp,
        contentPadding = bottomNavAndInsetPaddingValues(
            top = UiTokens.statusBarHeight + UiTokens.toolbarHeight + UiTokens.tabsHeight,
        ),
        horizontalArrangement = Arrangement.spacedBy(
            Timeline.Presentation.Text.WithEmbed.lazyGridHorizontalItemSpacing,
        ),
    ) {
        items(
            items = results,
            key = { it.id },
            itemContent = { result ->
                PostSearchResult(
                    modifier = Modifier
                        .threadedVideoPosition(
                            state = videoStates.getOrCreateStateFor(result),
                        )
                        .animateItem(),
                    paneMovableElementSharedTransitionScope = paneScaffoldState,
                    now = now,
                    result = result,
                    sharedElementPrefix = state.sharedElementPrefix,
                    postActions = postActions,
                )
            },
        )
    }
    if (paneScaffoldState.paneState.pane == ThreePane.Primary) {
        val videoPlayerController = LocalVideoPlayerController.current
        gridState.interpolatedVisibleIndexEffect(
            denominator = 10,
            itemsAvailable = results.size,
        ) { interpolatedIndex ->
            val flooredIndex = floor(interpolatedIndex).toInt()
            val fraction = interpolatedIndex - flooredIndex
            results.getOrNull(flooredIndex)
                ?.takeIf(SearchResult.OfPost::canAutoPlayVideo)
                ?.let(videoStates::retrieveStateFor)
                ?.videoIdAt(fraction)
                ?.let(videoPlayerController::play)
                ?: videoPlayerController.pauseActiveVideo()
        }
    }
    gridState.PivotedTilingEffect(
        items = results,
        onQueryChanged = { query ->
            searchResultActions(
                SearchState.Tile(
                    tilingAction = TilingState.Action.LoadAround(
                        query ?: state.tilingData.currentQuery,
                    ),
                ),
            )
        },
    )
}

@Composable
private fun PostSearchResult(
    modifier: Modifier = Modifier,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    now: Instant,
    result: SearchResult.OfPost,
    sharedElementPrefix: String,
    postActions: PostActions,
) {
    ElevatedCard(
        modifier = modifier,
        onClick = {
            postActions.onPostClicked(result.timelineItem.post)
        },
        content = {
            TimelineItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 16.dp,
                        bottom = 8.dp,
                    ),
                paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                presentationLookaheadScope = paneMovableElementSharedTransitionScope,
                now = now,
                item = result.timelineItem,
                sharedElementPrefix = sharedElementPrefix,
                presentation = Timeline.Presentation.Text.WithEmbed,
                postActions = postActions,
            )
        },
    )
}
