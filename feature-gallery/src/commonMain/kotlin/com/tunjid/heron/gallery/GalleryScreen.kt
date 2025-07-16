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

package com.tunjid.heron.gallery

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.tunjid.composables.gesturezoom.GestureZoomState.Companion.gestureZoomable
import com.tunjid.composables.gesturezoom.rememberGestureZoomState
import com.tunjid.heron.data.core.models.AspectRatio
import com.tunjid.heron.data.core.models.aspectRatioOrSquare
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.ControlsVisibilityEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.media.video.PlayerControls
import com.tunjid.heron.media.video.PlayerControlsUiState
import com.tunjid.heron.media.video.VideoPlayer
import com.tunjid.heron.media.video.VideoStill
import com.tunjid.heron.media.video.rememberUpdatedVideoPlayerState
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.post.sharedElementKey
import com.tunjid.heron.ui.isPrimaryOrActive
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableStickySharedElementOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun GalleryScreen(
    paneScaffoldState: PaneScaffoldState,
    modifier: Modifier = Modifier,
    state: State,
) {
    val videoPlayerController = LocalVideoPlayerController.current
    val playerControlsUiState = remember(videoPlayerController) {
        PlayerControlsUiState(videoPlayerController)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                onClick = playerControlsUiState::toggleVisibility
            )
    ) {
        val updatedItems by rememberUpdatedState(state.items)
        val pagerState = rememberPagerState(
            initialPage = state.startIndex
        ) {
            updatedItems.size
        }

        HorizontalPager(
            modifier = Modifier,
            state = pagerState,
            key = { page -> updatedItems[page].key },
            pageContent = { page ->
                var windowSize by remember { mutableStateOf(IntSize.Zero) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged {
                            windowSize = it
                        }
                ) {
                    when (val item = updatedItems[page]) {
                        is GalleryItem.Photo -> {
                            val zoomState = rememberGestureZoomState()
                            val coroutineScope = rememberCoroutineScope()
                            GalleryImage(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .aspectRatioFor(
                                        windowSize = windowSize,
                                        aspectRatio = item.image,
                                    )
                                    .gestureZoomable(zoomState)
                                    .combinedClickable(
                                        onClick = { },
                                        onDoubleClick = {
                                            coroutineScope.launch {
                                                zoomState.toggleZoom()
                                            }
                                        }
                                    ),
                                scaffoldState = paneScaffoldState,
                                item = item,
                                sharedElementPrefix = state.sharedElementPrefix,
                                postId = state.postId,
                            )
                        }

                        is GalleryItem.Video -> GalleryVideo(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .aspectRatioFor(
                                    windowSize = windowSize,
                                    aspectRatio = item.video,
                                ),
                            paneMovableElementSharedTransitionScope = paneScaffoldState,
                            item = item,
                            sharedElementPrefix = state.sharedElementPrefix,
                            postId = state.postId,
                        )
                    }
                }
            }
        )

        when (
            val currentItem = updatedItems.getOrNull(pagerState.currentPage)
        ) {
            null -> Unit
            is GalleryItem.Photo -> Unit
            is GalleryItem.Video -> {
                val videoPlayerState = videoPlayerController.getVideoStateById(
                    currentItem.video.playlist.uri
                )
                if (videoPlayerState != null) {
                    PlayerControls(
                        videoPlayerState = videoPlayerState,
                        state = playerControlsUiState,
                    )
                    LaunchedEffect(Unit) {
                        snapshotFlow { videoPlayerState.status }
                            .collectLatest {
                                playerControlsUiState.update(it)
                            }
                    }
                }
            }
        }

        pagerState.interpolatedVisibleIndexEffect(
            denominator = 10,
            itemsAvailable = updatedItems.size,
            onIndex = { index ->
                when (val media = updatedItems.getOrNull(index.roundToInt())) {
                    null -> Unit
                    is GalleryItem.Photo -> Unit
                    is GalleryItem.Video -> videoPlayerController.play(
                        media.video.playlist.uri
                    )
                }
            }
        )

        playerControlsUiState.ControlsVisibilityEffect()
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun GalleryImage(
    modifier: Modifier = Modifier,
    scaffoldState: PaneScaffoldState,
    item: GalleryItem.Photo,
    postId: PostId,
    sharedElementPrefix: String,
) {
    scaffoldState.updatedMovableStickySharedElementOf(
        modifier = modifier,
        sharedContentState = with(scaffoldState) {
            rememberSharedContentState(
                key = item.image.sharedElementKey(
                    prefix = sharedElementPrefix,
                    postId = postId,
                )
            )
        },
        state = remember(item.image) {
            ImageArgs(
                url = item.image.fullsize.uri,
                thumbnailUrl = item.image.thumb.uri,
                contentDescription = item.image.alt,
                contentScale = ContentScale.Crop,
                shape = RoundedPolygonShape.Rectangle,
            )
        },
        sharedElement = { args, innerModifier ->
            AsyncImage(
                modifier = innerModifier,
                args = args,
            )
        }
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun GalleryVideo(
    modifier: Modifier = Modifier,
    paneMovableElementSharedTransitionScope: PaneScaffoldState,
    item: GalleryItem.Video,
    postId: PostId,
    sharedElementPrefix: String,
) {
    val videoPlayerState = LocalVideoPlayerController.current.rememberUpdatedVideoPlayerState(
        videoUrl = item.video.playlist.uri,
        thumbnail = item.video.thumbnail?.uri,
        shape = RoundedPolygonShape.Rectangle,
    )
    if (!paneMovableElementSharedTransitionScope.isPrimaryOrActive) VideoStill(
        modifier = modifier,
        state = videoPlayerState,
    )
    else paneMovableElementSharedTransitionScope.updatedMovableStickySharedElementOf(
        modifier = modifier,
        sharedContentState = with(paneMovableElementSharedTransitionScope) {
            rememberSharedContentState(
                key = item.video.sharedElementKey(
                    prefix = sharedElementPrefix,
                    postId = postId,
                ),
            )
        },
        state = videoPlayerState,
        alternateOutgoingSharedElement = { state, innerModifier ->
            VideoStill(
                modifier = innerModifier,
                state = state,
            )
        },
        sharedElement = { state, innerModifier ->
            VideoPlayer(
                modifier = innerModifier,
                state = state,
            )
        }
    )
}

private fun Modifier.aspectRatioFor(
    windowSize: IntSize,
    aspectRatio: AspectRatio,
): Modifier {
    val screenAspectRatio = windowSize.width.toFloat() / windowSize.height.toFloat()
    val isWiderAspectRatioThanMedia = screenAspectRatio > aspectRatio.aspectRatioOrSquare
    return this
        .fillMaxSize()
        .aspectRatio(
            ratio = aspectRatio.aspectRatioOrSquare,
            matchHeightConstraintsFirst = isWiderAspectRatioThanMedia
        )
}
