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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVLayerVideoGravityResize
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVPlayerLayer
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIView

@Composable
actual fun VideoPlayer(
    modifier: Modifier,
    state: VideoPlayerState,
) {
    when (state) {
        is AVFoundationPlayerState -> AVFoundationVideoPlayer(modifier, state)
        else -> error("Unsupported VideoPlayerState: ${state::class}")
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun AVFoundationVideoPlayer(
    modifier: Modifier,
    state: AVFoundationPlayerState,
) {
    Box(modifier = modifier) {
        if (state.canShowVideo) {
            UIKitView(
                factory = {
                    VideoPlayerView().apply {
                        playerLayer.player = state.player
                        playerLayer.videoGravity = state.contentScale.toVideoGravity()
                    }
                },
                modifier = Modifier
                    .fillMaxSize(),
                update = { view ->
                    view.playerLayer.player = state.player
                    view.playerLayer.videoGravity = state.contentScale.toVideoGravity()
                },
                onRelease = {
                    state.hasRenderedFirstFrame = false
                    state.status = PlayerStatus.Idle.Evicted
                },
                // Handle UI interactivity in Compose
                properties = UIKitInteropProperties(
                    isInteractive = false,
                    isNativeAccessibilityEnabled = false,
                ),
            )
        }
        if (state.canShowStill) {
            VideoStill(
                modifier = Modifier.fillMaxSize(),
                state = state,
            )
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

@OptIn(ExperimentalForeignApi::class)
private class VideoPlayerView : UIView(frame = CGRectZero.readValue()) {
    val playerLayer = AVPlayerLayer()

    init {
        layer.addSublayer(playerLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        playerLayer.frame = bounds
    }
}

private fun ContentScale.toVideoGravity(): String {
    val gravity = when (this) {
        ContentScale.Crop -> AVLayerVideoGravityResizeAspectFill
        ContentScale.Fit,
        ContentScale.Inside,
        -> AVLayerVideoGravityResizeAspect
        ContentScale.FillBounds,
        ContentScale.FillWidth,
        ContentScale.FillHeight,
        -> AVLayerVideoGravityResize
        else -> AVLayerVideoGravityResizeAspectFill
    }
    return gravity ?: "AVLayerVideoGravityResizeAspectFill"
}

private val AVFoundationPlayerState.canShowVideo
    get() = when (status) {
        is PlayerStatus.Idle -> false
        is PlayerStatus.Play -> true
        is PlayerStatus.Pause -> true
    }

private val AVFoundationPlayerState.canShowStill
    get() = videoSize == IntSize.Zero ||
        !hasRenderedFirstFrame ||
        when (status) {
            is PlayerStatus.Idle -> true
            is PlayerStatus.Pause -> false
            PlayerStatus.Play.Requested -> true
            PlayerStatus.Play.Confirmed -> false
        }
