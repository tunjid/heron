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

package com.tunjid.heron.postdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.paneClip
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.TimelineViewType
import com.tunjid.heron.timeline.ui.withQuotingPostIdPrefix
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.timeline.ui.rememberPostActions
import com.tunjid.treenav.compose.threepane.ThreePane
import kotlinx.datetime.Clock
import kotlin.math.floor

@Composable
internal fun PostDetailScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyStaggeredGridState()
    val items by rememberUpdatedState(state.items)

    val videoStates = remember { ThreadedVideoPositionStates() }

    LazyVerticalStaggeredGrid(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxSize()
            .paneClip(),
        state = gridState,
        columns = StaggeredGridCells.Adaptive(340.dp),
        verticalItemSpacing = 4.dp,
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
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
                            state = videoStates.getOrCreateStateFor(item)
                        ),
                    panedSharedElementScope = paneScaffoldState,
                    now = remember { Clock.System.now() },
                    item = item,
                    sharedElementPrefix = state.sharedElementPrefix,
                    viewType = TimelineViewType.Blog,
                    postActions = rememberPostActions(
                        onPostClicked = { post: Post, quotingPostId: Id? ->
                            actions(
                                Action.Navigate.DelegateTo(
                                    NavigationAction.Common.ToPost(
                                        referringRouteOption = NavigationAction.ReferringRouteOption.Parent,
                                        sharedElementPrefix = state.sharedElementPrefix.withQuotingPostIdPrefix(
                                            quotingPostId = quotingPostId,
                                        ),
                                        post = post,
                                    )
                                )
                            )
                        },
                        onProfileClicked = { profile: Profile, post: Post, quotingPostId: Id? ->
                            actions(
                                Action.Navigate.DelegateTo(
                                    NavigationAction.Common.ToProfile(
                                        referringRouteOption = NavigationAction.ReferringRouteOption.Parent,
                                        profile = profile,
                                        avatarSharedElementKey = post.avatarSharedElementKey(
                                            prefix = state.sharedElementPrefix,
                                            quotingPostId = quotingPostId,
                                        ).takeIf { post.author.did == profile.did }
                                    )
                                )
                            )
                        },
                        onPostMediaClicked = { media: Embed.Media, index: Int, post: Post, quotingPostId: Id? ->
                            actions(
                                Action.Navigate.DelegateTo(
                                    NavigationAction.Common.ToMedia(
                                        post = post,
                                        media = media,
                                        startIndex = index,
                                        sharedElementPrefix = state.sharedElementPrefix.withQuotingPostIdPrefix(
                                            quotingPostId = quotingPostId,
                                        ),
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
                                        sharedElementPrefix = state.sharedElementPrefix,
                                    )
                                )
                            )
                        },
                        onPostMetadataClicked = onPostMetadataClicked@{ postMetadata ->
                            actions(
                                Action.Navigate.DelegateTo(
                                    when (postMetadata) {
                                        is Post.Metadata.Likes -> NavigationAction.Common.ToProfiles.Post.Likes(
                                            profileId = postMetadata.profileId,
                                            postId = postMetadata.postId,
                                        )

                                        is Post.Metadata.Quotes -> return@onPostMetadataClicked
                                        is Post.Metadata.Reposts -> NavigationAction.Common.ToProfiles.Post.Repost(
                                            profileId = postMetadata.profileId,
                                            postId = postMetadata.postId,
                                        )
                                    }
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
        // Allow for scrolling to the post selected even if others came before.
        item(
            span = StaggeredGridItemSpan.FullLine
        ) {
            Spacer(Modifier.height(800.dp))
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
                ?.let(videoStates::retrieveStateFor)
                ?.videoIdAt(fraction)
                ?.let(videoPlayerController::play)
                ?: videoPlayerController.pauseActiveVideo()
        }
    }
}

