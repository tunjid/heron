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

import com.sun.jna.Callback
import com.sun.jna.Native
import com.sun.jna.Pointer

/**
 * JNA callback interfaces for native AVFoundation player events.
 */
internal fun interface StatusCallback : Callback {
    fun invoke(context: Pointer?, status: Int)
}

internal fun interface TimeCallback : Callback {
    fun invoke(context: Pointer?, currentTime: Double, duration: Double)
}

internal fun interface FrameCallback : Callback {
    fun invoke(context: Pointer?)
}

internal fun interface EndOfPlaybackCallback : Callback {
    fun invoke(context: Pointer?)
}

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

    // Callback registration

    @JvmStatic external fun registerStatusCallback(
        context: Pointer?, callbackCtx: Pointer?, callback: StatusCallback?
    )

    @JvmStatic external fun unregisterStatusCallback(context: Pointer?)

    @JvmStatic external fun registerTimeCallback(
        context: Pointer?, callbackCtx: Pointer?, callback: TimeCallback?, interval: Double
    )

    @JvmStatic external fun unregisterTimeCallback(context: Pointer?)

    @JvmStatic external fun registerFrameCallback(
        context: Pointer?, callbackCtx: Pointer?, callback: FrameCallback?
    )

    @JvmStatic external fun unregisterFrameCallback(context: Pointer?)

    @JvmStatic external fun registerEndOfPlaybackCallback(
        context: Pointer?, callbackCtx: Pointer?, callback: EndOfPlaybackCallback?
    )

    @JvmStatic external fun unregisterEndOfPlaybackCallback(context: Pointer?)
}
