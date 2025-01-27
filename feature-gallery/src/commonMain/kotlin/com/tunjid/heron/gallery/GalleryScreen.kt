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

package com.tunjid.heron.gallery

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.tunjid.composables.gesturezoom.GestureZoomState.Companion.gestureZoomable
import com.tunjid.composables.gesturezoom.rememberGestureZoomState
import com.tunjid.heron.data.core.models.aspectRatioOrSquare
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
import com.tunjid.heron.timeline.ui.post.sharedElementKey
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.heron.ui.isPrimaryOrPreview
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun GalleryScreen(
    panedSharedElementScope: PanedSharedElementScope,
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
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (val item = updatedItems[page]) {
                        is GalleryItem.Photo -> {
                            val zoomState = rememberGestureZoomState()
                            val coroutineScope = rememberCoroutineScope()
                            GalleryImage(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .gestureZoomable(zoomState)
                                    .combinedClickable(
                                        onClick = { },
                                        onDoubleClick = {
                                            coroutineScope.launch {
                                                zoomState.toggleZoom()
                                            }
                                        }
                                    ),
                                panedSharedElementScope = panedSharedElementScope,
                                item = item,
                                sharedElementPrefix = state.sharedElementPrefix
                            )
                        }

                        is GalleryItem.Video -> GalleryVideo(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .aspectRatio(item.video.aspectRatioOrSquare),
                            panedSharedElementScope = panedSharedElementScope,
                            item = item,
                            sharedElementPrefix = state.sharedElementPrefix
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
    panedSharedElementScope: PanedSharedElementScope,
    item: GalleryItem.Photo,
    sharedElementPrefix: String,
) {
    panedSharedElementScope.updatedMovableSharedElementOf(
        modifier = modifier,
        key = item.image.sharedElementKey(
            prefix = sharedElementPrefix
        ),
        state = remember(item.image) {
            ImageArgs(
                url = item.image.fullsize.uri,
                thumbnailUrl = item.image.thumb.uri,
                contentDescription = item.image.alt,
                contentScale = ContentScale.Fit,
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
    panedSharedElementScope: PanedSharedElementScope,
    item: GalleryItem.Video,
    sharedElementPrefix: String,
) {
    val videoPlayerState = LocalVideoPlayerController.current.rememberUpdatedVideoPlayerState(
        videoUrl = item.video.playlist.uri,
        thumbnail = item.video.thumbnail?.uri,
        shape = RoundedPolygonShape.Rectangle,
    )
    if (!panedSharedElementScope.isPrimaryOrPreview) VideoStill(
        modifier = modifier,
        state = videoPlayerState,
    )
    else panedSharedElementScope.updatedMovableSharedElementOf(
        modifier = modifier,
        key = item.video.sharedElementKey(
            prefix = sharedElementPrefix
        ),
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
