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

import com.sun.jna.Native
import com.sun.jna.Pointer

/**
 * JNA direct mapping to the AVFoundationVideoPlayer library (AVFoundation on macOS).
 */
internal object SharedVideoPlayer {
    init {
        Native.register("AVFoundationVideoPlayer")
    }

    @JvmStatic external fun createVideoPlayer(): Pointer?

    @JvmStatic external fun openUri(context: Pointer?, uri: String?)

    @JvmStatic external fun playVideo(context: Pointer?)

    @JvmStatic external fun pauseVideo(context: Pointer?)

    @JvmStatic external fun setVolume(context: Pointer?, volume: Float)

    @JvmStatic external fun getVolume(context: Pointer?): Float

    @JvmStatic external fun getLatestFrame(context: Pointer?): Pointer?

    @JvmStatic external fun getFrameWidth(context: Pointer?): Int

    @JvmStatic external fun getFrameHeight(context: Pointer?): Int

    @JvmStatic external fun getVideoDuration(context: Pointer?): Double

    @JvmStatic external fun getCurrentTime(context: Pointer?): Double

    @JvmStatic external fun seekTo(context: Pointer?, time: Double)

    @JvmStatic external fun disposeVideoPlayer(context: Pointer?)
}
