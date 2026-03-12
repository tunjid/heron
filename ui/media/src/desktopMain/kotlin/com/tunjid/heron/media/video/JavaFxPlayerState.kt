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
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.util.Duration

@Stable
internal class JavaFxPlayerState(
    videoId: String,
    videoUrl: String,
    thumbnail: String?,
    isLooping: Boolean,
    autoplay: Boolean,
    isMuted: State<Boolean>,
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

    override var videoStill by mutableStateOf<ImageBitmap?>(
        value = null,
        policy = referentialEqualityPolicy(),
    )

    override val shouldReplay: Boolean
        get() = (totalDuration - lastPositionMs) <= 200 &&
            totalDuration != 0L &&
            !isLooping

    internal var mediaPlayer: MediaPlayer? = null
    internal var mediaView: MediaView? = null

    internal fun updateFromPlayer() {
        val player = mediaPlayer ?: return
        val currentTime = player.currentTime
        if (currentTime != null) {
            lastPositionMs = currentTime.toMillis().toLong()
        }
        val duration = player.totalDuration
        if (duration != null && duration != Duration.UNKNOWN && duration != Duration.INDEFINITE) {
            totalDuration = duration.toMillis().toLong()
        }
        val media = player.media
        if (media != null && media.width > 0 && media.height > 0) {
            videoSize = IntSize(media.width, media.height)
        }
    }
}

internal fun MediaPlayer.bind(state: JavaFxPlayerState) {
    state.mediaPlayer = this
    state.hasRenderedFirstFrame = false
    isMute = state.isMuted

    statusProperty().addListener { _, _, newStatus ->
        when (newStatus) {
            MediaPlayer.Status.PLAYING -> {
                state.status = PlayerStatus.Play.Confirmed
                if (!state.hasRenderedFirstFrame) {
                    state.hasRenderedFirstFrame = true
                }
                state.updateFromPlayer()
            }

            MediaPlayer.Status.PAUSED -> {
                state.status = PlayerStatus.Pause.Confirmed
                state.updateFromPlayer()
            }

            MediaPlayer.Status.STOPPED -> {
                state.status = PlayerStatus.Idle.Initial
                state.updateFromPlayer()
            }

            else -> {}
        }
    }

    currentTimeProperty().addListener { _, _, newTime ->
        if (newTime != null) {
            state.lastPositionMs = newTime.toMillis().toLong()
        }
    }

    totalDurationProperty().addListener { _, _, newDuration ->
        if (newDuration != null && newDuration != Duration.UNKNOWN && newDuration != Duration.INDEFINITE) {
            state.totalDuration = newDuration.toMillis().toLong()
        }
    }

    setOnReady {
        val media = media
        if (media != null && media.width > 0 && media.height > 0) {
            state.videoSize = IntSize(media.width, media.height)
        }
        val dur = totalDuration
        if (dur != null && dur != Duration.UNKNOWN && dur != Duration.INDEFINITE) {
            state.totalDuration = dur.toMillis().toLong()
        }
    }

    setOnEndOfMedia {
        if (state.isLooping) {
            seek(Duration.ZERO)
            play()
        } else {
            state.updateFromPlayer()
        }
    }
}

internal fun MediaPlayer.unbind(state: JavaFxPlayerState) {
    state.status = PlayerStatus.Pause.Requested
    val currentTime = currentTime
    if (currentTime != null) {
        state.lastPositionMs = currentTime.toMillis().toLong()
    }
    state.mediaPlayer = null
}
