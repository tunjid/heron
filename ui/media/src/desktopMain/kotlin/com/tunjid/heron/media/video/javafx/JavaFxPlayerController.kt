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

package com.tunjid.heron.media.video.javafx

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import com.tunjid.heron.media.video.PlayerStatus
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.media.video.VideoPlayerState
import com.tunjid.heron.media.video.VideoPlayerStates
import com.tunjid.heron.media.video.seekPositionOnPlayMs
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import javax.swing.SwingUtilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Stable
class JavaFxPlayerController(
    appMainScope: CoroutineScope,
) : VideoPlayerController {

    private val mediaPlayers = mutableMapOf<String, MediaPlayer>()
    private val states = VideoPlayerStates<JavaFxPlayerState>(
        onEvicted = { state ->
            Platform.runLater {
                mediaPlayers.remove(state.videoId)?.dispose()
            }
        },
    )

    override var isMuted: Boolean by states::isMuted

    init {
        System.setProperty("compose.interop.blending", "true")

        // Initialize JavaFX toolkit and prevent implicit exit
        SwingUtilities.invokeLater {
            JFXPanel() // Force toolkit init
            Platform.setImplicitExit(false)
        }

        snapshotFlow { isMuted }
            .onEach { muted ->
                Platform.runLater {
                    mediaPlayers[states.activeVideoId]?.isMute = muted
                }
            }
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
        JavaFxPlayerState(
            videoUrl = videoUrl,
            videoId = videoId,
            thumbnail = thumbnail,
            autoplay = autoplay,
            isLooping = isLooping,
            isMuted = derivedStateOf { isMuted },
        )
    }

    override fun play(
        videoId: String?,
        seekToMs: Long?,
    ) {
        val playerIdToPlay = videoId ?: states.activeVideoId
        val stateToPlay = states[playerIdToPlay] ?: return

        setActiveVideo(playerIdToPlay)

        Platform.runLater {
            val player = getOrCreatePlayer(stateToPlay)
            val alreadyPlaying = stateToPlay.status is PlayerStatus.Play.Confirmed

            // Resume if paused and same video
            if (stateToPlay.status is PlayerStatus.Pause && seekToMs == null) {
                player.play()
                return@runLater
            }

            // Already playing and not seeking
            if (alreadyPlaying && seekToMs == null) return@runLater

            // Seeking in same video
            if (alreadyPlaying && seekToMs != null) {
                player.seek(Duration.millis(seekToMs.toDouble()))
                return@runLater
            }

            // Start playback
            val position = stateToPlay.seekPositionOnPlayMs(seekToMs)
            if (position > 0) {
                player.seek(Duration.millis(position.toDouble()))
            }
            player.play()
        }
    }

    override fun pauseActiveVideo() {
        states.activeState?.apply {
            status = PlayerStatus.Pause.Requested
        }
        Platform.runLater {
            mediaPlayers[states.activeVideoId]?.pause()
        }
    }

    override fun seekTo(position: Long) {
        Platform.runLater {
            mediaPlayers[states.activeVideoId]?.seek(Duration.millis(position.toDouble()))
        }
    }

    override fun getVideoStateById(videoId: String): VideoPlayerState? = states[videoId]

    override fun retry(videoId: String) {
        // Dispose existing player so it gets recreated
        Platform.runLater {
            mediaPlayers.remove(videoId)?.dispose()
        }
        setActiveVideo(videoId)
        play(videoId)
    }

    private fun setActiveVideo(videoId: String) {
        val activeState = states[videoId] ?: return
        val previousId = states.activeVideoId
        states.activeVideoId = videoId

        if (previousId == states.activeVideoId) return

        states[previousId]?.let { previousState ->
            Platform.runLater {
                mediaPlayers[previousId]?.let { player ->
                    player.pause()
                    player.unbind(previousState)
                }
            }
        }

        Platform.runLater {
            mediaPlayers[videoId]?.bind(activeState)
        }
    }

    private fun getOrCreatePlayer(state: JavaFxPlayerState): MediaPlayer {
        mediaPlayers[state.videoId]?.let { existing ->
            if (existing != state.mediaPlayer) {
                existing.bind(state)
            }
            return existing
        }

        val media = Media(state.videoUrl)
        val player = MediaPlayer(media).apply {
            isMute = this@JavaFxPlayerController.isMuted
        }
        mediaPlayers[state.videoId] = player
        player.bind(state)
        return player
    }

    internal fun teardown() {
        Platform.runLater {
            mediaPlayers.values.forEach { it.dispose() }
            mediaPlayers.clear()
        }
    }
}
