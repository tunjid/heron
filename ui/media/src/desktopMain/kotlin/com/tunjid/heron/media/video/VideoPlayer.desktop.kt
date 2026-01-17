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
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import java.awt.Canvas
import java.awt.Color as AwtColor
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

@Composable
actual fun VideoPlayer(
    modifier: Modifier,
    state: VideoPlayerState,
) {
    check(state is VlcPlayerState)

    Box(modifier = modifier) {
        if (state.canShowVideo) {
            VideoSurface(
                modifier = Modifier.fillMaxSize(),
                state = state,
            )
        }
        if (state.canShowStill) {
            VideoStill(
                modifier = Modifier.fillMaxSize(),
                state = state,
            )
        }
    }

    // Keep player position up to date
    LaunchedEffect(state) {
        while (true) {
            if (state.status is PlayerStatus.Play) {
                state.updateFromPlayer()
            }
            delay(1.seconds)
        }
    }

    DisposableEffect(state) {
        state.status = PlayerStatus.Idle.Initial
        onDispose {
            state.hasRenderedFirstFrame = false
            state.status = PlayerStatus.Idle.Evicted
        }
    }
}

@Composable
private fun VideoSurface(
    modifier: Modifier,
    state: VlcPlayerState,
) {
    SwingPanel(
        background = Color.Black,
        modifier = modifier,
        factory = {
            Canvas().apply {
                background = AwtColor.BLACK
            }
        },
        update = { canvas ->
            state.setVideoSurface(canvas)
        },
    )
}

private val VlcPlayerState.canShowVideo
    get() = when (status) {
        is PlayerStatus.Idle -> false
        is PlayerStatus.Play -> true
        is PlayerStatus.Pause -> true
    }

private val VlcPlayerState.canShowStill
    get() = videoSize == IntSize.Zero ||
        !hasRenderedFirstFrame ||
        when (status) {
            is PlayerStatus.Idle -> true
            is PlayerStatus.Pause -> false
            PlayerStatus.Play.Requested -> true
            PlayerStatus.Play.Confirmed -> false
        }
