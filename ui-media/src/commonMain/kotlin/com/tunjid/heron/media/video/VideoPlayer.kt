package com.tunjid.heron.media.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.tunjid.heron.ui.shapes.RoundedPolygonShape

@Composable
expect fun VideoPlayer(
    modifier: Modifier,
    state: VideoPlayerState,
)

@Composable
fun VideoPlayerController.rememberUpdatedVideoPlayerState(
    videoUrl: String,
    videoId: String = videoUrl,
    thumbnail: String? = null,
    isLooping: Boolean = true,
    isMuted: Boolean = false,
    autoplay: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    shape: RoundedPolygonShape = RoundedPolygonShape.Rectangle,
): VideoPlayerState = registerVideo(
    videoUrl = videoUrl,
    videoId = videoId,
    thumbnail = thumbnail,
    isLooping = isLooping,
    isMuted = isMuted,
    autoplay = autoplay
).also { videoPlayerState ->
    videoPlayerState.thumbnailUrl = thumbnail
    videoPlayerState.contentScale = contentScale
    videoPlayerState.alignment = alignment
    videoPlayerState.shape = shape
}