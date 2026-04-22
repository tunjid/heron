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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.logging.loggableText
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive

@Stable
class AVFoundationPlayerController(
    private val appMainScope: CoroutineScope,
) : VideoPlayerController {

    private val states = VideoPlayerStates(
        onEvicted = AVFoundationPlayerState::dispose,
    )

    @OptIn(ExperimentalForeignApi::class)
    private val audioSession by lazy {
        AVAudioSession.sharedInstance().apply {
            try {
                setCategory(AVAudioSessionCategoryPlayback, null)
                setActive(true, null)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "configureAudioSession failed: ${e.loggableText()}" }
            }
        }
    }

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
        // Read audio session to lazily initialize if necessary
        audioSession
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
