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

package com.tunjid.heron.media.video

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.IntSize
import com.tunjid.composables.ui.animate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

@Composable
actual fun VideoPlayer(modifier: Modifier, state: VideoPlayerState) {
    check(state is ExoPlayerState)

    val graphicsLayer = rememberGraphicsLayer()
    val alignment = state.alignment.animate()
    val contentScale = state.contentScale.animate()
    // TODO: Animate this
    val shape = state.shape

    Box(modifier = modifier) {
        // Note its important the embedded Surface is removed from the composition when it is
        // scrolled
        // off screen
        if (state.canShowVideo)
            VideoSurface(
                exoPlayer = state.player,
                contentScale = contentScale,
                alignment = alignment,
                videoSize = state.videoSize,
                shape = shape,
                modifier =
                    Modifier.fillMaxSize().drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(graphicsLayer)
                    },
            )
        if (state.canShowStill) VideoStill(modifier = Modifier.fillMaxSize(), state = state)

        // Capture a still frame from the video to use as a stand in when buffering playback
        LaunchedEffect(state.status) {
            if (
                state.status is PlayerStatus.Pause &&
                    state.hasRenderedFirstFrame &&
                    graphicsLayer.size.height != 0 &&
                    graphicsLayer.size.width != 0
            ) {
                state.videoStill = graphicsLayer.toImageBitmap()
            }
        }
    }
    // Keep player position up to date
    LaunchedEffect(Unit) {
        snapshotFlow { state.player?.let { it to state.status } }
            .filterNotNull()
            .collectLatest { (player, status) ->
                if (status is PlayerStatus.Play)
                    while (true) {
                        state.lastPositionMs = player.currentPosition
                        delay(100)
                    }
            }
    }
    DisposableEffect(graphicsLayer) {
        state.status = PlayerStatus.Idle.Initial
        onDispose {
            state.hasRenderedFirstFrame = false
            state.status = PlayerStatus.Idle.Evicted
        }
    }
}

private val ExoPlayerState.canShowVideo
    get() =
        when (status) {
            is PlayerStatus.Idle.Initial -> true
            is PlayerStatus.Play -> true
            is PlayerStatus.Pause -> true
            PlayerStatus.Idle.Evicted -> false
        }

private val ExoPlayerState.canShowStill
    get() =
        videoSize == IntSize.Zero ||
            !hasRenderedFirstFrame ||
            when (status) {
                is PlayerStatus.Idle -> true
                is PlayerStatus.Pause -> false
                PlayerStatus.Play.Requested -> true
                PlayerStatus.Play.Confirmed -> false
            }
