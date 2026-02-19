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
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import java.awt.Component
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer

@Stable
internal class VlcPlayerState(
    private val factory: MediaPlayerFactory,
    videoId: String,
    videoUrl: String,
    thumbnail: String?,
    isLooping: Boolean,
    autoplay: Boolean,
    isMuted: State<Boolean>,
    mediaPlayerState: State<EmbeddedMediaPlayer?>,
) : VideoPlayerState {

    override var thumbnailUrl by mutableStateOf(thumbnail)

    override var alignment by mutableStateOf(Alignment.Center)

    override var contentScale by mutableStateOf(ContentScale.Crop)

    override var shape by mutableStateOf<RoundedPolygonShape>(RoundedPolygonShape.Rectangle)

    override var videoId by mutableStateOf(videoId)
        internal set

    override var videoUrl by mutableStateOf(videoUrl)
        internal set

    override var isLooping by mutableStateOf(isLooping)
        internal set

    override val isMuted by isMuted

    override var autoplay by mutableStateOf(autoplay)
        internal set

    override var hasRenderedFirstFrame by mutableStateOf(false)

    override var videoSize by mutableStateOf(IntSize.Zero)
        internal set

    override var lastPositionMs by mutableLongStateOf(0L)
        internal set

    override var totalDuration by mutableLongStateOf(0L)
        internal set

    override var status by mutableStateOf<PlayerStatus>(PlayerStatus.Idle.Initial)
        internal set

    override var videoStill by
        mutableStateOf<ImageBitmap?>(value = null, policy = referentialEqualityPolicy())

    override val shouldReplay: Boolean
        get() = (totalDuration - lastPositionMs) <= 200 && totalDuration != 0L && !isLooping

    internal val player by mediaPlayerState

    internal val playerListener =
        object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer?) {
                status = PlayerStatus.Play.Confirmed
                updateFromPlayer()
            }

            override fun paused(mediaPlayer: MediaPlayer?) {
                status = PlayerStatus.Pause.Confirmed
                updateFromPlayer()
            }

            override fun stopped(mediaPlayer: MediaPlayer?) {
                status = PlayerStatus.Idle.Initial
                updateFromPlayer()
            }

            override fun finished(mediaPlayer: MediaPlayer?) {
                // Loop is handled by controller usually
                updateFromPlayer()
            }

            override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) {
                lastPositionMs = newTime
            }

            override fun lengthChanged(mediaPlayer: MediaPlayer?, newLength: Long) {
                totalDuration = newLength
            }

            override fun videoOutput(mediaPlayer: MediaPlayer?, newCount: Int) {
                if (newCount > 0) hasRenderedFirstFrame = true
            }
        }

    internal fun updateFromPlayer() {
        val player = player ?: return
        totalDuration = player.status().length()
        lastPositionMs = player.status().time()

        val dim = player.video().videoDimension()
        if (dim != null) {
            videoSize = IntSize(dim.width, dim.height)
        }
    }

    private var activeComponent: Component? = null
    private var lastAttachedPlayer: MediaPlayer? = null

    internal fun setVideoSurface(component: Component) {
        val player = player ?: return
        if (activeComponent == component && lastAttachedPlayer == player) return

        activeComponent = component
        lastAttachedPlayer = player
        val videoSurface = factory.videoSurfaces().newVideoSurface(component)
        player.videoSurface().set(videoSurface)
    }
}

internal fun EmbeddedMediaPlayer.bind(state: VlcPlayerState) {
    events().addMediaPlayerEventListener(state.playerListener)
    state.hasRenderedFirstFrame = false
    audio().setMute(state.isMuted)
}

internal fun EmbeddedMediaPlayer.unbind(state: VlcPlayerState) {
    state.status = PlayerStatus.Pause.Requested
    events().removeMediaPlayerEventListener(state.playerListener)
    state.lastPositionMs = status().time()
    videoSurface().set(null)
}
