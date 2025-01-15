package com.tunjid.heron.media.video

import androidx.annotation.OptIn
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer


@Stable
internal class ExoPlayerState internal constructor(
    videoId: String,
    videoUrl: String,
    isLooping: Boolean,
    isMuted: Boolean,
    autoplay: Boolean,
    exoPlayerState: State<ExoPlayer?>,
) : VideoPlayerState {

    // UI logic fields

    override var alignment by mutableStateOf(Alignment.Center)

    override var contentScale by mutableStateOf(ContentScale.Crop)

    // Business logic fields

    override var videoId by mutableStateOf(videoId)
        internal set

    override var videoUrl by mutableStateOf(videoUrl)
        internal set

    override var isLooping by mutableStateOf(isLooping)
        internal set

    override var isMuted by mutableStateOf(isMuted)
        internal set

    override var autoplay by mutableStateOf(autoplay)
        internal set

    override var hasRenderedFirstFrame by mutableStateOf(false)

    internal var isLoading by mutableStateOf(true)
        private set

    var videoSize by mutableStateOf(IntSize.Zero)
        internal set

    override var lastPositionMs by mutableLongStateOf(0L)
        internal set

    override var totalDuration by mutableLongStateOf(0L)
        internal set

    override var status by mutableStateOf<PlayerStatus>(PlayerStatus.Idle.Initial)
        internal set

    internal var videoStill by mutableStateOf<ImageBitmap?>(
        value = null,
        policy = referentialEqualityPolicy()
    )

    internal val player by exoPlayerState

    internal val playerListener = object : Player.Listener {

        override fun onVideoSizeChanged(size: VideoSize) {
            updateVideoSize(size)
        }

        override fun onPlayWhenReadyChanged(
            playWhenReady: Boolean,
            reason: Int,
        ) {
            // Paused at the end of a media item in the playlist that is not looping, record end of playback
            if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                onVideoEnded()
            } else {
                status = when {
                    playWhenReady && player?.playbackState == Player.STATE_READY && autoplay -> PlayerStatus.Play.Confirmed
                    playWhenReady -> PlayerStatus.Play.Requested
                    status == PlayerStatus.Idle.Initial -> status
                    else -> PlayerStatus.Pause.Confirmed
                }
            }
            player?.videoSize?.let(::updateVideoSize)
            updateFromPlayer()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            isLoading = playbackState != Player.STATE_READY
            status = when {
                playbackState == Player.STATE_READY && player?.playWhenReady == true -> PlayerStatus.Play.Confirmed
                player?.playWhenReady == true -> PlayerStatus.Play.Requested
                else -> status
            }
            player?.videoSize?.let(::updateVideoSize)
            updateFromPlayer()
            when (playbackState) {
                Player.STATE_READY -> Unit

                Player.STATE_ENDED -> {
                    onVideoEnded()
                }

                Player.STATE_BUFFERING -> Unit

                Player.STATE_IDLE -> {
                    status = PlayerStatus.Idle.Initial
                }
            }
        }

        override fun onRenderedFirstFrame() {
            this@ExoPlayerState.hasRenderedFirstFrame = true
            player?.videoSize?.let(::updateVideoSize)
            updateFromPlayer()
        }

        override fun onVolumeChanged(volume: Float) {
            updateFromPlayer()
        }

        override fun onEvents(
            player: Player,
            events: Player.Events,
        ) {
            updateFromPlayer()
        }
    }

    private fun updateVideoSize(size: VideoSize) {
        videoSize = when (val intSize = size.toIntSize()) {
            IntSize.Zero -> videoSize
            else -> intSize
        }
    }

    internal fun updateFromPlayer() {
        val player = player ?: return
        val duration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0

        this.isMuted = player.isMuted
        this.lastPositionMs = player.currentPosition.takeIf { it != C.TIME_UNSET } ?: 0
        totalDuration = duration
    }

    /** Whether the video should reset to the beginning. */
    override val shouldReplay: Boolean
        get() = (totalDuration - lastPositionMs) <= END_OF_VIDEO_RESOLUTION_THRESHOLD_MS &&
                totalDuration != 0L &&
                !isLooping

    private fun onVideoEnded() = Unit

}

private fun VideoSize.toIntSize() = IntSize(width, height)

internal fun VideoPlayerState.seekPositionOnPlayMs(seekToMs: Long?): Long {
    return seekToMs ?: if (shouldReplay) 0L else lastPositionMs
}

internal fun ExoPlayer.unbind(state: ExoPlayerState) {
    state.status = PlayerStatus.Pause.Requested
    removeListener(state.playerListener)
    setVideoSurface(null)
    state.lastPositionMs = currentPosition
}

@OptIn(UnstableApi::class)
internal fun ExoPlayer.bind(state: ExoPlayerState) {
    addListener(state.playerListener)
    state.hasRenderedFirstFrame = false
    isMuted = state.isMuted

    repeatMode = if (state.isLooping) REPEAT_MODE_ONE else REPEAT_MODE_OFF
    // Stop ExoPlayer from automatically advancing to the next item in the playlist if we're not looping.
    pauseAtEndOfMediaItems = !state.isLooping
}

private var ExoPlayer.isMuted: Boolean
    get() = volume < VIDEO_PLAYER_MUTE_THRESHOLD
    set(value) {
        volume = if (value) 0f else 1f
        setAudioAttributes(audioAttributes, !value)
    }

private const val VIDEO_PLAYER_MUTE_THRESHOLD = 0.001f
private const val END_OF_VIDEO_RESOLUTION_THRESHOLD_MS = 200
