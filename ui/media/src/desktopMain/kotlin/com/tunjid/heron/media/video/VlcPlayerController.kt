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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

@Stable
class VlcPlayerController(
    scope: CoroutineScope,
) : VideoPlayerController {

    override var isMuted: Boolean by mutableStateOf(true)

    private val idsToStates = mutableStateMapOf<String, VlcPlayerState>()
    private var activeVideoId: String by mutableStateOf("")

    private val factory = MediaPlayerFactory()
    private val player = factory.mediaPlayers().newEmbeddedMediaPlayer()

    init {
        snapshotFlow { isMuted }
            .onEach { player.audio().setMute(it) }
            .launchIn(scope)

        snapshotFlow { idsToStates[activeVideoId]?.status }
            .map { it == null || it is PlayerStatus.Idle }
            .filter { it }
            .onEach { player.controls().pause() }
            .launchIn(scope)

        player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun finished(mediaPlayer: MediaPlayer?) {
                val activeState = idsToStates[activeVideoId] ?: return
                if (activeState.isLooping) {
                    player.submit {
                        player.controls().play()
                    }
                }
            }
        })
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

        val videoPlayerState = VlcPlayerState(
            factory = factory,
            videoUrl = videoUrl,
            videoId = videoId,
            thumbnail = thumbnail,
            autoplay = autoplay,
            isLooping = isLooping,
            isMuted = derivedStateOf { isMuted },
            mediaPlayerState = derivedStateOf {
                player.takeIf { isCurrentMediaItem(videoId) }
            },
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

        val alreadyPlaying = stateToPlay.status is PlayerStatus.Play.Confirmed

        // Resume if paused and same video
        if (stateToPlay.status is PlayerStatus.Pause && seekToMs == null && isCurrentMediaItem(playerIdToPlay)) {
            player.controls().play()
            return
        }

        // Already playing and not seeking, do nothing
        if (alreadyPlaying && seekToMs == null && isCurrentMediaItem(playerIdToPlay)) return

        // If we are just seeking in the same video
        if (alreadyPlaying && seekToMs != null && isCurrentMediaItem(playerIdToPlay)) {
            player.controls().setTime(seekToMs)
            return
        }

        // Otherwise load and play (or restart)
        player.media().play(stateToPlay.videoUrl)

        if (seekToMs != null) {
            player.controls().setTime(seekToMs)
        } else {
            val pos = stateToPlay.seekPositionOnPlayMs(null)
            if (pos > 0) player.controls().setTime(pos)
        }
    }

    override fun pauseActiveVideo() {
        activeVideoId.let(idsToStates::get)?.apply {
            status = PlayerStatus.Pause.Requested
        }
        player.controls().pause()
    }

    override fun seekTo(position: Long) {
        player.controls().setTime(position)
    }

    override fun getVideoStateById(videoId: String): VideoPlayerState? = idsToStates[videoId]

    override fun retry(videoId: String) {
        setActiveVideo(videoId)
        play(videoId)
    }

    override fun unregisterAll(retainedVideoIds: Set<String>): Set<String> {
        idsToStates
            .filterNot { retainedVideoIds.contains(it.key) }
            .forEach { (id, videoState) ->
                if (activeVideoId == id) {
                    player.controls().stop()
                }
                player.unbind(videoState)
                idsToStates.remove(id)
            }
        return retainedVideoIds - idsToStates.keys
    }

    private fun setActiveVideo(videoId: String) {
        val activeState = idsToStates[videoId] ?: return
        val previousId = activeVideoId
        activeVideoId = videoId

        if (previousId == activeVideoId) return

        idsToStates[previousId]?.let { player.unbind(it) }
        player.bind(activeState)
    }

    private fun isCurrentMediaItem(videoId: String) = activeVideoId == videoId

    internal fun teardown() {
        player.release()
        factory.release()
    }

    private fun trim() {
        val size = idsToStates.size
        if (size >= MaxVideoStates) idsToStates.keys.filter {
            val state = idsToStates[it]
            state?.status is PlayerStatus.Idle.Evicted
        }
            .take(size - MaxVideoStates)
            .forEach(idsToStates::remove)
    }
}

private fun VideoPlayerState.seekPositionOnPlayMs(seekToMs: Long?): Long {
    return seekToMs ?: if (shouldReplay) 0L else lastPositionMs
}

private const val MaxVideoStates = 30
