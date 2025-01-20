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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.composables.gesturezoom.GestureZoomState.Companion.gestureZoomable
import com.tunjid.composables.gesturezoom.rememberGestureZoomState
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.media.video.VideoPlayer
import com.tunjid.heron.media.video.rememberUpdatedVideoPlayerState
import com.tunjid.heron.timeline.ui.post.sharedElementKey
import com.tunjid.heron.ui.SharedElementScope
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.shapes.toRoundedPolygonShape
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun GalleryScreen(
    sharedElementScope: SharedElementScope,
    modifier: Modifier = Modifier,
    state: State,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
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
                            sharedElementScope = sharedElementScope,
                            item = item,
                            sharedElementPrefix = state.sharedElementPrefix
                        )
                    }

                    is GalleryItem.Video -> GalleryVideo(
                        modifier = Modifier
                            .fillMaxSize(),
                        sharedElementScope = sharedElementScope,
                        item = item,
                        sharedElementPrefix = state.sharedElementPrefix
                    )
                }
            }
        )

        val videoPlayerController = LocalVideoPlayerController.current
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
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun GalleryImage(
    modifier: Modifier = Modifier,
    sharedElementScope: SharedElementScope,
    item: GalleryItem.Photo,
    sharedElementPrefix: String,
) {
    sharedElementScope.updatedMovableSharedElementOf(
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
    sharedElementScope: SharedElementScope,
    item: GalleryItem.Video,
    sharedElementPrefix: String,
) {
    val videoPlayerState = LocalVideoPlayerController.current.rememberUpdatedVideoPlayerState(
        videoUrl = item.video.playlist.uri,
        thumbnail = item.video.thumbnail?.uri,
        shape = remember {
            RoundedCornerShape(16.dp).toRoundedPolygonShape()
        }
    )
    sharedElementScope.updatedMovableSharedElementOf(
        modifier = modifier,
        key = item.video.sharedElementKey(
            prefix = sharedElementPrefix
        ),
        state = videoPlayerState,
        sharedElement = { state, innerModifier ->
            VideoPlayer(
                modifier = innerModifier,
                state = state,
            )
        }
    )
}

