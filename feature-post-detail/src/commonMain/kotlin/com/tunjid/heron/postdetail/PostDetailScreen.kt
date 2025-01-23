/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.postdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.ui.SharedElementScope
import kotlinx.datetime.Clock
import kotlin.math.floor

@Composable
internal fun PostDetailScreen(
    sharedElementScope: SharedElementScope,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyStaggeredGridState()
    val items by rememberUpdatedState(state.items)

    val onPostClicked = remember {
        { post: Post ->
            actions(
                Action.Navigate.DelegateTo(
                    NavigationAction.Common.ToPost(
                        referringRouteOption = NavigationAction.ReferringRouteOption.Parent,
                        sharedElementPrefix = state.sharedElementPrefix,
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
                        referringRouteOption = NavigationAction.ReferringRouteOption.Parent,
                        profile = profile,
                        avatarSharedElementKey = post.avatarSharedElementKey(
                            prefix = state.sharedElementPrefix,
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
                        sharedElementPrefix = state.sharedElementPrefix,
                    )
                )
            )
        }
    }

    val onPostMetadataClicked = remember {
        onPostMetadataClicked@{ postMetadata: Post.Metadata ->
            actions(
                Action.Navigate.DelegateTo(
                    when (postMetadata) {
                        is Post.Metadata.Likes -> NavigationAction.Common.ToProfiles.Post.Likes(
                            postId = postMetadata.postId,
                        )

                        is Post.Metadata.Quotes -> return@onPostMetadataClicked
                        is Post.Metadata.Reposts -> NavigationAction.Common.ToProfiles.Post.Repost(
                            postId = postMetadata.postId,
                        )
                    }
                )
            )
        }
    }

    val videoStates = remember { ThreadedVideoPositionStates() }

    LazyVerticalStaggeredGrid(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxSize()
            .clip(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                )
            ),
        state = gridState,
        columns = StaggeredGridCells.Adaptive(340.dp),
        verticalItemSpacing = 4.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = !sharedElementScope.isTransitionActive,
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
                    sharedElementScope = sharedElementScope,
                    now = remember { Clock.System.now() },
                    item = item,
                    sharedElementPrefix = state.sharedElementPrefix,
                    onPostClicked = onPostClicked,
                    onProfileClicked = onProfileClicked,
                    onPostMediaClicked = onPostMediaClicked,
                    onReplyToPost = {},
                    onPostMetadataClicked = onPostMetadataClicked,
                    onPostInteraction = {
                        actions(
                            Action.SendPostInteraction(it)
                        )
                    },
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

