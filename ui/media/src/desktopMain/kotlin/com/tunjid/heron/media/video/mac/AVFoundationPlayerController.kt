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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.tunjid.heron.media.video.PlayerStatus
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.media.video.VideoPlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Stable
class AVFoundationPlayerController(
    private val appMainScope: CoroutineScope,
) : VideoPlayerController {

    override var isMuted: Boolean by mutableStateOf(true)

    private val idsToStates = mutableStateMapOf<String, AVFoundationPlayerState>()
    private var activeVideoId: String by mutableStateOf("")

    init {
        snapshotFlow { isMuted }
            .onEach { idsToStates[activeVideoId]?.applyVolume() }
            .launchIn(appMainScope)
    }

    override fun registerVideo(
        videoUrl: String,
        videoId: String,
        thumbnail: String?,
        isLooping: Boolean,
        autoplay: Boolean,
    ): VideoPlayerState {
        idsToStates[videoId]?.let { return it }

        trim()

        val videoPlayerState = AVFoundationPlayerState(
            videoUrl = videoUrl,
            videoId = videoId,
            thumbnail = thumbnail,
            autoplay = autoplay,
            isLooping = isLooping,
            isMuted = derivedStateOf { isMuted },
            appMainScope = appMainScope,
        )

        idsToStates[videoId] = videoPlayerState
        return videoPlayerState
    }

    override fun play(
        videoId: String?,
        seekToMs: Long?,
    ) {
        val playerIdToPlay = videoId ?: activeVideoId
        val stateToPlay = idsToStates[playerIdToPlay] ?: return

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
        idsToStates[activeVideoId]?.apply {
            status = PlayerStatus.Pause.Requested
            pauseNative()
        }
    }

    override fun seekTo(position: Long) {
        idsToStates[activeVideoId]?.seekToNative(position)
    }

    override fun getVideoStateById(videoId: String): VideoPlayerState? = idsToStates[videoId]

    override fun retry(videoId: String) {
        val stateToRetry = idsToStates[videoId] ?: return
        setActiveVideo(videoId)
        stateToRetry.dispose()
        stateToRetry.reinitialize()
        stateToRetry.openMedia(stateToRetry.videoUrl)
        stateToRetry.playNative()
    }

    override fun unregisterAll(retainedVideoIds: Set<String>): Set<String> {
        val toDispose = idsToStates
            .filterNot { retainedVideoIds.contains(it.key) }
        toDispose.keys.forEach { id ->
            idsToStates.remove(id)
        }
        toDispose.values.forEach { it.dispose() }
        return retainedVideoIds - idsToStates.keys
    }

    private fun setActiveVideo(videoId: String) {
        idsToStates[videoId] ?: return
        val previousId = activeVideoId
        activeVideoId = videoId

        if (previousId == activeVideoId) return

        idsToStates[previousId]?.let { previousState ->
            previousState.status = PlayerStatus.Pause.Requested
            previousState.pauseNative()
        }
    }

    private fun trim() {
        val size = idsToStates.size
        if (size < MaxVideoStates) return
        val toDispose = idsToStates.keys
            .filter { idsToStates[it]?.status is PlayerStatus.Idle.Evicted }
            .take(size - MaxVideoStates)
            .mapNotNull { id ->
                idsToStates.remove(id)
            }
        toDispose.forEach { it.dispose() }
    }
}

private fun VideoPlayerState.seekPositionOnPlayMs(seekToMs: Long?): Long {
    return seekToMs ?: if (shouldReplay) 0L else lastPositionMs
}

private const val MaxVideoStates = 30
