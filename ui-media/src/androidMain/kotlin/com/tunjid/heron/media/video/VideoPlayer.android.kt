package com.tunjid.heron.media.video

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import com.tunjid.composables.ui.animate
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.shapes.RoundedPolygonShape

@Composable
actual fun VideoPlayer(
    modifier: Modifier,
    state: VideoPlayerState,
) {
    check(state is ExoPlayerState)

    val graphicsLayer = rememberGraphicsLayer()
    val alignment = state.alignment.animate()
    val contentScale = state.contentScale.animate()

    Box(modifier = modifier) {
        // Note its important the embedded Surface is removed from the composition when it is scrolled
        // off screen
        if (state.canShowVideo) VideoSurface(
            exoPlayer = state.player,
            contentScale = contentScale,
            alignment = alignment,
            videoSize = state.videoSize,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayer)
                }

        )
        if (state.canShowStill) VideoStill(
            lastBitmap = state.videoStill.takeIf {
                state.status != PlayerStatus.Idle.Initial
            },
            url = state.videoUrl,
            modifier = Modifier.fillMaxSize(),
            alignment = alignment,
            contentScale = contentScale,
        )

        // Capture a still frame from the video to use as a stand in when buffering playback
        LaunchedEffect(state.status) {
            if (state.status is PlayerStatus.Pause
                && state.hasRenderedFirstFrame
                && graphicsLayer.size.height != 0
                && graphicsLayer.size.width != 0
            ) {
                state.videoStill = graphicsLayer.toImageBitmap()
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

@Composable
private fun VideoStill(
    lastBitmap: ImageBitmap?,
    url: String?,
    modifier: Modifier,
    alignment: Alignment,
    contentScale: ContentScale,
) {
    when (lastBitmap) {
        null -> AsyncImage(
            modifier = modifier,
            args = remember {
                ImageArgs(
                    url = url,
                    contentDescription = null,
                    alignment = alignment,
                    contentScale = contentScale,
                    shape = RoundedPolygonShape.Rectangle,
                )
            },
        )

        else -> Image(
            modifier = modifier,
            bitmap = lastBitmap,
            contentDescription = null,
            alignment = alignment,
            contentScale = contentScale
        )
    }
}

private val ExoPlayerState.canShowVideo
    get() = when (status) {
        is PlayerStatus.Idle.Initial -> true
        is PlayerStatus.Play -> true
        is PlayerStatus.Pause -> true
        PlayerStatus.Idle.Evicted -> false
    }

private val ExoPlayerState.canShowStill
    get() = videoSize == IntSize.Zero
            || !hasRenderedFirstFrame
            || when (status) {
        is PlayerStatus.Idle -> true
        is PlayerStatus.Pause -> false
        PlayerStatus.Play.Requested -> true
        PlayerStatus.Play.Confirmed -> false
    }