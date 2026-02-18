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
import androidx.compose.runtime.staticCompositionLocalOf

/** Controller for videos. */
@Stable
interface VideoPlayerController {

    var isMuted: Boolean

    /**
     * Registers a video for playback. If a unique ID is not specified, it is defaulted to
     * [videoUrl].
     *
     * Registering a video is not enough to play it; it must be explicitly played.
     */
    fun registerVideo(
        videoUrl: String,
        videoId: String = videoUrl,
        thumbnail: String? = null,
        isLooping: Boolean = false,
        autoplay: Boolean = false,
    ): VideoPlayerState

    /**
     * Plays the video with the specified [videoId].
     *
     * @param videoId The unique ID of the player sending this event. See [registerVideo].
     * @param seekToMs position to seek to before playback
     */
    fun play(videoId: String? = null, seekToMs: Long? = null)

    /** Pauses the currently active video. */
    fun pauseActiveVideo()

    /** Seeks to a position specified in milliseconds in the currently active video. */
    fun seekTo(position: Long)

    /**
     * Retrieves the [VideoPlayerState] object for a given [videoId].
     *
     * @return A [VideoPlayerState] object, or null if a video with [videoId] was not registered.
     */
    fun getVideoStateById(videoId: String): VideoPlayerState?

    /**
     * Tries to reload a video, normally used after an error has occurred. If the currently active
     * video is different, sets the active video to the one with the given [videoId].
     */
    fun retry(videoId: String)

    /**
     * Unregisters all videos with IDs that are not present in [retainedVideoIds].
     *
     * @return A set of videoIds that are present in [retainedVideoIds] but not the video list (aka.
     *   IDs of videos that have not been registered)
     */
    fun unregisterAll(retainedVideoIds: Set<String>): Set<String>
}

val LocalVideoPlayerController =
    staticCompositionLocalOf<VideoPlayerController> { StubVideoPlayerController }
