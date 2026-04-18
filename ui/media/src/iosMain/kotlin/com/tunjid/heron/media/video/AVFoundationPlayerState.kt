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

@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.tunjid.heron.media.video

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.logging.loggableText
import com.tunjid.heron.media.video.cinterop.NSKeyValueObservingProtocol
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemStatus
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.AVPlayerTimeControlStatus
import platform.AVFoundation.AVPlayerTimeControlStatusPaused
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.presentationSize
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.setVolume
import platform.AVFoundation.timeControlStatus
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.darwin.NSObject
import platform.darwin.NSObjectProtocol

@Stable
internal class AVFoundationPlayerState(
    videoId: String,
    videoUrl: String,
    thumbnail: String?,
    isLooping: Boolean,
    autoplay: Boolean,
    isMuted: State<Boolean>,
    private val appMainScope: CoroutineScope,
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

    override var videoStill: ImageBitmap? by mutableStateOf(null)

    override val shouldReplay: Boolean
        get() = (totalDuration - lastPositionMs) <= 200 &&
            totalDuration != 0L &&
            !isLooping

    internal var player by mutableStateOf(AVPlayer())
        private set

    internal var mediaOpenCalled: Boolean = false
        private set

    private var playerScope = playerScope()

    // Observer references for cleanup
    private var timeObserver: Any? = null
    private var endOfPlaybackObserver: NSObjectProtocol? = null
    private var hasRegisteredPlayerKVO = false
    private var observedPlayerItem: AVPlayerItem? = null
    private val kvoObserver = PlayerKVOObserver(this)

    init {
        applyVolume()
        registerObservers()
    }

    internal fun openMedia(uri: String) {
        try {
            val url = NSURL.URLWithString(uri) ?: return
            val asset = AVURLAsset.URLAssetWithURL(url, options = null)
            val item = AVPlayerItem.playerItemWithAsset(asset)
            observePlayerItem(item)
            player.replaceCurrentItemWithPlayerItem(item)
            mediaOpenCalled = true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "openMedia failed: ${e.loggableText()}" }
        }
    }

    internal fun playNative() {
        try {
            player.play()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "playNative failed: ${e.loggableText()}" }
        }
    }

    internal fun pauseNative() {
        try {
            player.pause()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "pauseNative failed: ${e.loggableText()}" }
        }
    }

    internal fun seekToNative(positionMs: Long) {
        try {
            val time = CMTimeMakeWithSeconds(
                seconds = positionMs.toDouble() / 1000.0,
                preferredTimescale = 600,
            )
            player.seekToTime(time)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "seekToNative failed: ${e.loggableText()}" }
        }
    }

    internal fun applyVolume() {
        try {
            player.setVolume(if (isMuted) 0f else 1f)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "applyVolume failed: ${e.loggableText()}" }
        }
    }

    internal fun reinitialize() {
        dispose()

        player = AVPlayer()
        playerScope = playerScope()
        mediaOpenCalled = false
        applyVolume()
        registerObservers()
    }

    internal fun dispose() {
        unregisterObservers()
        playerScope.cancel()

        player.replaceCurrentItemWithPlayerItem(null)
        mediaOpenCalled = false
    }

    private fun registerObservers() {
        // Periodic time observer (0.25s interval)
        val interval = CMTimeMakeWithSeconds(
            seconds = TIME_OBSERVER_INTERVAL_SECONDS,
            preferredTimescale = 600,
        )
        timeObserver = player.addPeriodicTimeObserverForInterval(
            interval = interval,
            queue = null,
            usingBlock = { time: CValue<CMTime>? ->
                if (time == null) return@addPeriodicTimeObserverForInterval
                val currentSeconds = CMTimeGetSeconds(time)
                if (currentSeconds >= 0) {
                    lastPositionMs = (currentSeconds * 1000).toLong()
                }
                player.currentItem?.let { item ->
                    val durationSeconds = CMTimeGetSeconds(item.duration)
                    if (durationSeconds > 0 && !durationSeconds.isNaN() && !durationSeconds.isInfinite()) {
                        totalDuration = (durationSeconds * 1000).toLong()
                    }
                }
            },
        )

        // KVO on player.timeControlStatus
        player.addObserver(
            observer = kvoObserver,
            forKeyPath = "timeControlStatus",
            options = NSKeyValueObservingOptionNew,
            context = null,
        )
        hasRegisteredPlayerKVO = true
    }

    private fun observePlayerItem(item: AVPlayerItem) {
        // Remove previous item observers
        removePlayerItemObservers()

        // KVO on item.status
        item.addObserver(
            observer = kvoObserver,
            forKeyPath = "status",
            options = NSKeyValueObservingOptionNew,
            context = null,
        )
        observedPlayerItem = item

        // End of playback notification
        endOfPlaybackObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = item,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            if (isLooping) {
                val startTime = CMTimeMakeWithSeconds(
                    seconds = 0.0,
                    preferredTimescale = 600,
                )
                player.seekToTime(startTime)
                player.play()
            } else {
                status = PlayerStatus.Pause.Confirmed
            }
        }
    }

    private fun removePlayerItemObservers() {
        observedPlayerItem?.let { item ->
            try {
                item.removeObserver(kvoObserver, forKeyPath = "status")
            } catch (e: Exception) {
                // Observer may not be registered
                logcat(LogPriority.ERROR) {
                    "remove status kvo failed: ${e.loggableText()}"
                }
            }
        }
        observedPlayerItem = null

        endOfPlaybackObserver?.let {
            NSNotificationCenter.defaultCenter.removeObserver(it)
        }
        endOfPlaybackObserver = null
    }

    private fun unregisterObservers() {
        // Remove time observer
        timeObserver?.let { observer -> player.removeTimeObserver(observer) }
        timeObserver = null

        // Remove KVO on player
        if (hasRegisteredPlayerKVO) {
            try {
                player.removeObserver(kvoObserver, forKeyPath = "timeControlStatus")
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) {
                    "remove timeControlStatus kvo failed: ${e.loggableText()}"
                }
            }
            hasRegisteredPlayerKVO = false
        }

        removePlayerItemObservers()
    }

    internal fun onTimeControlStatusChanged(newStatus: AVPlayerTimeControlStatus) {
        when (newStatus) {
            AVPlayerTimeControlStatusPaused -> {
                status = PlayerStatus.Pause.Confirmed
            }

            AVPlayerTimeControlStatusPlaying -> {
                status = PlayerStatus.Play.Confirmed
                if (!hasRenderedFirstFrame) hasRenderedFirstFrame = true
            }

            AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate -> {
                // Keep current status while buffering
            }
        }
    }

    internal fun onPlayerItemStatusChanged(itemStatus: AVPlayerItemStatus) {
        when (itemStatus) {
            AVPlayerItemStatusReadyToPlay -> {
                updateMetadata()
                if (!hasRenderedFirstFrame) hasRenderedFirstFrame = true
            }

            AVPlayerItemStatusFailed -> {
                logcat(LogPriority.ERROR) {
                    "Player item failed: ${player.currentItem?.error?.localizedDescription}"
                }
            }

            else -> Unit
        }
    }

    private fun updateMetadata() {
        player.currentItem?.let { item ->
            val durationSeconds = CMTimeGetSeconds(item.duration)
            if (durationSeconds > 0 && !durationSeconds.isNaN() && !durationSeconds.isInfinite()) {
                totalDuration = (durationSeconds * 1000).toLong()
            }
            item.presentationSize.useContents {
                val w = width.toInt()
                val h = height.toInt()
                if (w > 0 && h > 0) {
                    videoSize = IntSize(width = w, height = h)
                }
            }
        }
    }

    private fun playerScope(): CoroutineScope =
        CoroutineScope(
            appMainScope.coroutineContext + Job(
                parent = appMainScope.coroutineContext[Job],
            ),
        )

    private fun CoroutineScope.cancel() {
        coroutineContext[Job]?.cancel()
    }
}

/**
 * KVO observer for AVPlayer and AVPlayerItem property changes.
 *
 * Implements [NSKeyValueObservingProtocol] which is declared via a cinterop
 * `.def` file because `NSKeyValueObserving` is an informal protocol (ObjC
 * category on NSObject). K/N imports informal protocol methods as extension
 * functions which cannot be overridden. By declaring a formal `@protocol` in
 * the `.def` file, we get a Kotlin interface with an overridable method.
 */
private class PlayerKVOObserver(
    private val state: AVFoundationPlayerState,
) : NSObject(),
    NSKeyValueObservingProtocol {

    override fun observeValueForKeyPath(
        keyPath: String?,
        ofObject: Any?,
        change: Map<Any?, *>?,
        context: COpaquePointer?,
    ) {
        when (keyPath) {
            "timeControlStatus" -> {
                state.onTimeControlStatusChanged(state.player.timeControlStatus)
            }

            "status" -> {
                val item = ofObject as? AVPlayerItem ?: return
                state.onPlayerItemStatusChanged(item.status)
            }
        }
    }
}

private const val TIME_OBSERVER_INTERVAL_SECONDS = 0.25
