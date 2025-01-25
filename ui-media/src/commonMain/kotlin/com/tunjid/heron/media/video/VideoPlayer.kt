package com.tunjid.heron.media.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import kotlin.time.Duration.Companion.milliseconds

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
    autoplay: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    shape: RoundedPolygonShape = RoundedPolygonShape.Rectangle,
): VideoPlayerState = registerVideo(
    videoUrl = videoUrl,
    videoId = videoId,
    thumbnail = thumbnail,
    isLooping = isLooping,
    autoplay = autoplay
).also { videoPlayerState ->
    videoPlayerState.thumbnailUrl = thumbnail
    videoPlayerState.contentScale = contentScale
    videoPlayerState.alignment = alignment
    videoPlayerState.shape = shape
}

fun Long.formatVideoDuration() =
    milliseconds.toComponents { h, m, s, _ ->
        val paddedSeconds = "0$s".takeLast(2)
        when {
            h > 0 -> "$h:$m:$paddedSeconds"
            else -> "$m:$paddedSeconds"
        }
    }
