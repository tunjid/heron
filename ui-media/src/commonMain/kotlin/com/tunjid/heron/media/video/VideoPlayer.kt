package com.tunjid.heron.media.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

@Composable
expect fun VideoPlayer(
    modifier: Modifier,
    state: VideoPlayerState,
)

@Composable
fun VideoPlayerController.rememberUpdatedVideoPlayerState(
    videoUrl: String,
    videoId: String = videoUrl,
    isLooping: Boolean = true,
    isMuted: Boolean = false,
    autoplay: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
): VideoPlayerState = registerVideo(
    videoUrl = videoUrl,
    videoId = videoId,
    isLooping = isLooping,
    isMuted = isMuted,
    autoplay = autoplay
).also { videoPlayerState ->
    videoPlayerState.contentScale = contentScale
    videoPlayerState.alignment = alignment
}