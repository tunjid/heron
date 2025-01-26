package com.tunjid.heron.media.video

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.tunjid.heron.ui.shapes.RoundedPolygonShape


object StubVideoPlayerController : VideoPlayerController {

    @Suppress("UNUSED_PARAMETER")
    override var isMuted: Boolean
        get() = true
        set(value) {}

    private val idsToStates = mutableStateMapOf<String, NoOpVideoPlayerState>()

    override fun registerVideo(
        videoUrl: String,
        videoId: String,
        thumbnail: String?,
        isLooping: Boolean,
        autoplay: Boolean,
    ): VideoPlayerState = idsToStates.getOrPut(videoId) {
        NoOpVideoPlayerState(
            videoId = videoId,
            thumbnailUrl = thumbnail,
            autoplay = autoplay,
            videoUrl = videoUrl,
            isLooping = isLooping,
            isMuted = isMuted,
        )
    }

    override fun play(
        videoId: String?,
        seekToMs: Long?,
    ) = Unit

    override fun pauseActiveVideo() = Unit

    override fun seekTo(position: Long) = Unit

    override fun getVideoStateById(videoId: String): VideoPlayerState? = null

    override fun retry(videoId: String) = Unit

    override fun unregisterAll(
        retainedVideoIds: Set<String>,
    ): Set<String> = retainedVideoIds
}

private data class NoOpVideoPlayerState(
    override val videoId: String,
    override val autoplay: Boolean,
    override val videoUrl: String,
    override val isLooping: Boolean,
    override val isMuted: Boolean,
    override val lastPositionMs: Long = 0L,
    override val totalDuration: Long = 0L,
    override val hasRenderedFirstFrame: Boolean = false,
    override var thumbnailUrl: String? = null,
    override var alignment: Alignment = Alignment.Center,
    override var contentScale: ContentScale = ContentScale.Crop,
    override var shape: RoundedPolygonShape = RoundedPolygonShape.Rectangle,
    override val status: PlayerStatus = PlayerStatus.Idle.Initial,
    override val shouldReplay: Boolean = false,
    override val videoStill: ImageBitmap? = null,
) : VideoPlayerState