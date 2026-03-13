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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.tunjid.heron.media.video.javafx.JavaFxPlayerState
import com.tunjid.heron.media.video.mac.AVFoundationPlayerState
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.media.MediaView
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

@Composable
actual fun VideoPlayer(
    modifier: Modifier,
    state: VideoPlayerState,
) {
    when (state) {
        is AVFoundationPlayerState -> AVFoundationVideoPlayer(modifier, state)
        is JavaFxPlayerState -> JavaFxVideoPlayer(modifier, state)
        else -> error("Unsupported VideoPlayerState: ${state::class}")
    }
}

@Composable
private fun AVFoundationVideoPlayer(
    modifier: Modifier,
    state: AVFoundationPlayerState,
) {
    Box(modifier = modifier) {
        if (state.canShowVideo) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                state.currentFrameState.value?.let { frame ->
                    drawScaledImage(
                        image = frame,
                        dstSize = IntSize(
                            width = size.width.toInt(),
                            height = size.height.toInt(),
                        ),
                        contentScale = state.contentScale,
                    )
                }
            }
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

@Composable
private fun JavaFxVideoPlayer(
    modifier: Modifier,
    state: JavaFxPlayerState,
) {
    Box(modifier = modifier) {
        if (state.canShowVideo) {
            JavaFxVideoSurface(
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
private fun JavaFxVideoSurface(
    modifier: Modifier,
    state: JavaFxPlayerState,
) {
    val surfaceColor = MaterialTheme.colorScheme
        .surface
    val surfaceColorHex = surfaceColor
        .toArgb()
        .toHexString()

    SwingPanel(
        background = surfaceColor,
        modifier = modifier,
        factory = {
            JFXPanel().also { jfxPanel ->
                Platform.runLater {
                    jfxPanel.isEnabled = false
                    val mediaView = MediaView().apply {
                        isPreserveRatio = true
                        mediaPlayer = state.mediaPlayer
                    }
                    state.mediaView = mediaView
                    val root = StackPane(mediaView).apply {
                        style = "-fx-background-color: #$surfaceColorHex;"
                    }
                    jfxPanel.scene = Scene(root)
                }
            }
        },
        update = { jfxPanel ->
            Platform.runLater {
                val mediaView = state.mediaView ?: return@runLater
                mediaView.mediaPlayer = state.mediaPlayer
                mediaView.fitWidth = jfxPanel.width.toDouble()
                mediaView.fitHeight = jfxPanel.height.toDouble()
            }
        },
    )
}

// Native player helpers

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

// JavaFx player helpers

private val JavaFxPlayerState.canShowVideo
    get() = when (status) {
        is PlayerStatus.Idle -> false
        is PlayerStatus.Play -> true
        is PlayerStatus.Pause -> true
    }

private val JavaFxPlayerState.canShowStill
    get() = videoSize == IntSize.Zero ||
        !hasRenderedFirstFrame ||
        when (status) {
            is PlayerStatus.Idle -> true
            is PlayerStatus.Pause -> false
            PlayerStatus.Play.Requested -> true
            PlayerStatus.Play.Confirmed -> false
        }

private fun DrawScope.drawScaledImage(
    image: ImageBitmap,
    dstSize: IntSize,
    contentScale: ContentScale,
) {
    if (contentScale == ContentScale.Crop) {
        val frameW = image.width
        val frameH = image.height

        val scale = maxOf(
            dstSize.width / frameW.toFloat(),
            dstSize.height / frameH.toFloat(),
        )

        val srcW = (dstSize.width / scale).toInt()
        val srcH = (dstSize.height / scale).toInt()
        val srcX = ((frameW - srcW) / 2).coerceAtLeast(0)
        val srcY = ((frameH - srcH) / 2).coerceAtLeast(0)

        drawImage(
            image = image,
            srcOffset = IntOffset(srcX, srcY),
            srcSize = IntSize(srcW, srcH),
            dstSize = dstSize,
        )
    } else {
        drawImage(
            image = image,
            dstSize = dstSize,
        )
    }
}
