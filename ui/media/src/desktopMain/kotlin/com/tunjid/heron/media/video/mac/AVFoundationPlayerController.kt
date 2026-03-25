/*
 *    Originally from ComposeMediaPlayer by Elie G.
 *    https://github.com/kdroidFilter/ComposeMediaPlayer
 *
 *    Copyright (c) 2025 Elie G.
 *    Licensed under the MIT License.
 *
 *    Adapted for Heron by Adetunji Dahunsi.
 */

package com.tunjid.heron.media.video.mac

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import com.tunjid.heron.media.video.PlayerStatus
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.media.video.VideoPlayerState
import com.tunjid.heron.media.video.VideoPlayerStates
import com.tunjid.heron.media.video.seekPositionOnPlayMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Stable
class AVFoundationPlayerController(
    private val appMainScope: CoroutineScope,
) : VideoPlayerController {

    private val states = VideoPlayerStates(
        onEvicted = AVFoundationPlayerState::dispose,
    )

    override var isMuted: Boolean by states::isMuted

    init {
        snapshotFlow { isMuted }
            .onEach { states.activeState?.applyVolume() }
            .launchIn(appMainScope)
    }

    override fun registerVideo(
        videoUrl: String,
        videoId: String,
        thumbnail: String?,
        isLooping: Boolean,
        autoplay: Boolean,
    ): VideoPlayerState = states.registerOrGet(
        videoId = videoId,
    ) {
        AVFoundationPlayerState(
            videoUrl = videoUrl,
            videoId = videoId,
            thumbnail = thumbnail,
            autoplay = autoplay,
            isLooping = isLooping,
            isMuted = derivedStateOf { isMuted },
            appMainScope = appMainScope,
        )
    }

    override fun play(
        videoId: String?,
        seekToMs: Long?,
    ) {
        val playerIdToPlay = videoId ?: states.activeVideoId
        val stateToPlay = states[playerIdToPlay] ?: return

        setActiveVideo(playerIdToPlay)

        // Open media if not yet called
        if (!stateToPlay.mediaOpenCalled) {
            stateToPlay.openMedia(stateToPlay.videoUrl)
            stateToPlay.playNative()
            return
        }

        val alreadyPlaying = stateToPlay.status is PlayerStatus.Play.Confirmed

        // Resume if paused and same video
        if (stateToPlay.status is PlayerStatus.Pause && seekToMs == null) {
            stateToPlay.playNative()
            return
        }

        // Already playing and not seeking
        if (alreadyPlaying && seekToMs == null) return

        // Seeking in same video
        if (alreadyPlaying && seekToMs != null) {
            stateToPlay.seekToNative(seekToMs)
            return
        }

        // Start playback
        val position = stateToPlay.seekPositionOnPlayMs(seekToMs)
        if (position > 0) {
            stateToPlay.seekToNative(position)
        }
        stateToPlay.playNative()
    }

    override fun pauseActiveVideo() {
        states.activeState?.apply {
            status = PlayerStatus.Pause.Requested
            pauseNative()
        }
    }

    override fun seekTo(position: Long) {
        states.activeState?.seekToNative(position)
    }

    override fun getVideoStateById(videoId: String): VideoPlayerState? = states[videoId]

    override fun retry(videoId: String) {
        val stateToRetry = states[videoId] ?: return
        setActiveVideo(videoId)
        stateToRetry.dispose()
        stateToRetry.reinitialize()
        stateToRetry.openMedia(stateToRetry.videoUrl)
        stateToRetry.playNative()
    }

    private fun setActiveVideo(videoId: String) {
        states[videoId] ?: return
        val previousId = states.activeVideoId
        states.activeVideoId = videoId

        if (previousId == states.activeVideoId) return

        states[previousId]?.let { previousState ->
            previousState.status = PlayerStatus.Pause.Requested
            previousState.pauseNative()
        }
    }
}
