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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import com.sun.jna.Pointer
import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.logging.loggableText
import com.tunjid.heron.media.video.PlayerStatus
import com.tunjid.heron.media.video.VideoPlayerState
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

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

    override var videoStill by mutableStateOf<ImageBitmap?>(
        value = null,
        policy = referentialEqualityPolicy(),
    )

    override val shouldReplay: Boolean
        get() = (totalDuration - lastPositionMs) <= 200 &&
            totalDuration != 0L &&
            !isLooping

    // Native player pointer — initialized eagerly since JNA calls are synchronous
    internal var playerPointer: Pointer? = AVFoundationVideoPlayer.createVideoPlayer()
    internal var mediaOpenCalled: Boolean = false

    // Frame rendering state
    internal var currentFrame by mutableStateOf<ImageBitmap?>(null)
    private var currentFrameSize = IntSize.Zero
    private var skiaBitmapA: Bitmap? = null
    private var skiaBitmapB: Bitmap? = null
    private var nextSkiaBitmapA: Boolean = true
    private var lastFrameUpdateTime: Long = 0
    private val serialDispatcher = Dispatchers.Default.limitedParallelism(
        parallelism = 1,
    )

    // Callback properties — held as fields to prevent GC.
    // Compose mutableStateOf writes are thread-safe via the snapshot system.
    private val statusCallback = StatusCallback { _, statusCode ->
        when (statusCode) {
            STATUS_CODE_PAUSED -> status = PlayerStatus.Pause.Confirmed
            STATUS_CODE_PLAYING -> {
                status = PlayerStatus.Play.Confirmed
                updateMetadata()
                if (!hasRenderedFirstFrame) hasRenderedFirstFrame = true
            }
            STATUS_CODE_BUFFERING -> Unit // keep current status
        }
    }

    private val timeCallback = TimeCallback { _, currentTime, duration ->
        if (currentTime >= 0) lastPositionMs = currentTime.secondsToMs()
        if (duration > 0) totalDuration = duration.secondsToMs()
    }

    private val frameCallback = FrameCallback { _ ->
        appMainScope.launch {
            updateFrameAsync()
        }
    }

    private val endOfPlaybackCallback = EndOfPlaybackCallback { _ ->
        if (isLooping) AVFoundationVideoPlayer.seekTo(
            context = playerPointer ?: return@EndOfPlaybackCallback,
            time = 0.0,
        )
        else status = PlayerStatus.Pause.Confirmed
    }

    init {
        applyVolume()
        registerCallbacks()
    }

    internal fun openMedia(uri: String) {
        val ptr = playerPointer ?: return
        AVFoundationVideoPlayer.openUri(
            context = ptr,
            uri = uri,
        )
        mediaOpenCalled = true
    }

    internal fun updateMetadata() {
        val ptr = playerPointer ?: return
        val displayWidth = AVFoundationVideoPlayer.getDisplayWidth(ptr)
        val displayHeight = AVFoundationVideoPlayer.getDisplayHeight(ptr)
        val duration = AVFoundationVideoPlayer.getVideoDuration(ptr)

        if (duration > 0) totalDuration = duration.secondsToMs()
        if (displayWidth > 0 && displayHeight > 0) videoSize = IntSize(
            width = displayWidth,
            height = displayHeight,
        )
    }

    /**
     * Registers native callbacks for status, time, frame, and end-of-playback events.
     */
    internal fun registerCallbacks() {
        val ptr = playerPointer ?: return
        AVFoundationVideoPlayer.registerStatusCallback(
            context = ptr,
            callbackCtx = null,
            callback = statusCallback,
        )
        AVFoundationVideoPlayer.registerTimeCallback(
            context = ptr,
            callbackCtx = null,
            callback = timeCallback,
            interval = TIME_OBSERVER_INTERVAL_SECONDS,
        )
        AVFoundationVideoPlayer.registerFrameCallback(
            context = ptr,
            callbackCtx = null,
            callback = frameCallback,
        )
        AVFoundationVideoPlayer.registerEndOfPlaybackCallback(
            context = ptr,
            callbackCtx = null,
            callback = endOfPlaybackCallback,
        )
    }

    /**
     * Unregisters all native callbacks.
     */
    internal fun unregisterCallbacks() {
        val ptr = playerPointer ?: return
        AVFoundationVideoPlayer.unregisterStatusCallback(ptr)
        AVFoundationVideoPlayer.unregisterTimeCallback(ptr)
        AVFoundationVideoPlayer.unregisterFrameCallback(ptr)
        AVFoundationVideoPlayer.unregisterEndOfPlaybackCallback(ptr)
    }

    internal fun playNative() {
        val ptr = playerPointer ?: return
        AVFoundationVideoPlayer.playVideo(ptr)
    }

    internal fun pauseNative() {
        val ptr = playerPointer ?: return
        AVFoundationVideoPlayer.pauseVideo(ptr)
    }

    internal fun seekToNative(positionMs: Long) {
        val ptr = playerPointer ?: return
        AVFoundationVideoPlayer.seekTo(
            context = ptr,
            time = positionMs.msToSeconds(),
        )
    }

    internal fun applyVolume() {
        val ptr = playerPointer ?: return
        AVFoundationVideoPlayer.setVolume(
            context = ptr,
            volume = if (isMuted) 0f else 1f,
        )
    }

    private suspend fun updateFrameAsync() {
        val ptr = playerPointer ?: return

        try {
            val latestFrameSize = IntSize(
                width = AVFoundationVideoPlayer.getFrameWidth(ptr),
                height = AVFoundationVideoPlayer.getFrameHeight(ptr),
            )
            if (latestFrameSize.width <= 0 || latestFrameSize.height <= 0) return

            val framePtr = AVFoundationVideoPlayer.getLatestFrame(ptr) ?: return

            val pixelCount = latestFrameSize.width * latestFrameSize.height
            val frameSizeBytes = pixelCount.toLong() * 4L

            withContext(serialDispatcher) {
                val srcBuf = framePtr.getByteBuffer(0, frameSizeBytes)

                if (skiaBitmapA == null || currentFrameSize != latestFrameSize) {
                    skiaBitmapA?.close()
                    skiaBitmapB?.close()

                    val imageInfo = ImageInfo(
                        width = latestFrameSize.width,
                        height = latestFrameSize.height,
                        colorType = ColorType.BGRA_8888,
                        alphaType = ColorAlphaType.OPAQUE,
                    )
                    skiaBitmapA = Bitmap().apply { allocPixels(imageInfo) }
                    skiaBitmapB = Bitmap().apply { allocPixels(imageInfo) }
                    currentFrameSize = latestFrameSize
                    nextSkiaBitmapA = true
                }

                val targetBitmap =
                    if (nextSkiaBitmapA) requireNotNull(skiaBitmapA)
                    else requireNotNull(skiaBitmapB)

                nextSkiaBitmapA = !nextSkiaBitmapA

                val pixmap = targetBitmap.peekPixels() ?: return@withContext
                val pixelsAddr = pixmap.addr
                if (pixelsAddr == 0L) return@withContext

                srcBuf.rewind()
                val destRowBytes = pixmap.rowBytes
                val destSizeBytes = destRowBytes.toLong() * latestFrameSize.height.toLong()
                val destBuf = Pointer(pixelsAddr).getByteBuffer(0, destSizeBytes)
                copyBgraFrame(
                    src = srcBuf,
                    dst = destBuf,
                    width = latestFrameSize.width,
                    height = latestFrameSize.height,
                    dstRowBytes = destRowBytes,
                )

                currentFrame = targetBitmap.asComposeImageBitmap()
            }

            lastFrameUpdateTime = System.currentTimeMillis()

            if (!hasRenderedFirstFrame) {
                hasRenderedFirstFrame = true
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) {
                "Video frame extraction failed: ${e.loggableText()}"
            }
        }
    }

    /**
     * Re-creates the native player after [dispose]. Used by retry flows.
     */
    internal fun reinitialize() {
        playerPointer = AVFoundationVideoPlayer.createVideoPlayer()
        mediaOpenCalled = false
        applyVolume()
        registerCallbacks()
    }

    internal fun dispose() {
        unregisterCallbacks()

        val pointerToDispose = playerPointer
        playerPointer = null
        mediaOpenCalled = false

        skiaBitmapA?.close()
        skiaBitmapB?.close()
        skiaBitmapA = null
        skiaBitmapB = null
        currentFrameSize = IntSize.Zero
        nextSkiaBitmapA = true

        pointerToDispose?.let(AVFoundationVideoPlayer::disposeVideoPlayer)

        currentFrame = null
    }
}

private const val TIME_OBSERVER_INTERVAL_SECONDS = 0.25

private const val STATUS_CODE_PAUSED = 0
private const val STATUS_CODE_PLAYING = 1
private const val STATUS_CODE_BUFFERING = 2

private fun Double.secondsToMs(): Long = (this * 1000).toLong()

private fun Long.msToSeconds(): Double = this.toDouble() / 1000.0
