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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.tunjid.heron.media.video.PlayerStatus
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.media.video.VideoPlayerState
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

    override var isMuted: Boolean by mutableStateOf(true)

    private val idsToStates = mutableStateMapOf<String, JavaFxPlayerState>()
    private var activeVideoId: String by mutableStateOf("")
    private val mediaPlayers = mutableMapOf<String, MediaPlayer>()

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
                    mediaPlayers[activeVideoId]?.isMute = muted
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
    ): VideoPlayerState {
        idsToStates[videoId]?.let { return it }

        trim()

        val videoPlayerState = JavaFxPlayerState(
            videoUrl = videoUrl,
            videoId = videoId,
            thumbnail = thumbnail,
            autoplay = autoplay,
            isLooping = isLooping,
            isMuted = derivedStateOf { isMuted },
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
        activeVideoId.let(idsToStates::get)?.apply {
            status = PlayerStatus.Pause.Requested
        }
        Platform.runLater {
            mediaPlayers[activeVideoId]?.pause()
        }
    }

    override fun seekTo(position: Long) {
        Platform.runLater {
            mediaPlayers[activeVideoId]?.seek(Duration.millis(position.toDouble()))
        }
    }

    override fun getVideoStateById(videoId: String): VideoPlayerState? = idsToStates[videoId]

    override fun retry(videoId: String) {
        // Dispose existing player so it gets recreated
        Platform.runLater {
            mediaPlayers.remove(videoId)?.dispose()
        }
        setActiveVideo(videoId)
        play(videoId)
    }

    override fun unregisterAll(retainedVideoIds: Set<String>): Set<String> {
        idsToStates
            .filterNot { retainedVideoIds.contains(it.key) }
            .forEach { (id, videoState) ->
                Platform.runLater {
                    val player = mediaPlayers.remove(id)
                    player?.unbind(videoState)
                    player?.dispose()
                }
                idsToStates.remove(id)
            }
        return retainedVideoIds - idsToStates.keys
    }

    private fun setActiveVideo(videoId: String) {
        val activeState = idsToStates[videoId] ?: return
        val previousId = activeVideoId
        activeVideoId = videoId

        if (previousId == activeVideoId) return

        idsToStates[previousId]?.let { previousState ->
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

    private fun trim() {
        val size = idsToStates.size
        if (size >= MaxVideoStates) idsToStates.keys.filter {
            val state = idsToStates[it]
            state?.status is PlayerStatus.Idle.Evicted
        }
            .take(size - MaxVideoStates)
            .forEach { id ->
                Platform.runLater {
                    mediaPlayers.remove(id)?.dispose()
                }
                idsToStates.remove(id)
            }
    }
}

private fun VideoPlayerState.seekPositionOnPlayMs(seekToMs: Long?): Long {
    return seekToMs ?: if (shouldReplay) 0L else lastPositionMs
}

private const val MaxVideoStates = 30
