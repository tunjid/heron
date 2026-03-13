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
    internal var playerPointer: Pointer? = SharedVideoPlayer.createVideoPlayer()
    internal var mediaOpenCalled: Boolean = false

    // Frame rendering state
    internal var currentFrame by mutableStateOf<ImageBitmap?>(null)
    private var skiaBitmapWidth: Int = 0
    private var skiaBitmapHeight: Int = 0
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
        if (isLooping) SharedVideoPlayer.seekTo(
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
        SharedVideoPlayer.openUri(
            context = ptr,
            uri = uri,
        )
        mediaOpenCalled = true
    }

    internal fun updateMetadata() {
        val ptr = playerPointer ?: return
        val width = SharedVideoPlayer.getFrameWidth(ptr)
        val height = SharedVideoPlayer.getFrameHeight(ptr)
        val duration = SharedVideoPlayer.getVideoDuration(ptr)

        if (duration > 0) totalDuration = duration.secondsToMs()
        if (width > 0 && height > 0) videoSize = IntSize(
            width = width,
            height = height,
        )
    }

    /**
     * Registers native callbacks for status, time, frame, and end-of-playback events.
     */
    internal fun registerCallbacks() {
        val ptr = playerPointer ?: return
        SharedVideoPlayer.registerStatusCallback(
            context = ptr,
            callbackCtx = null,
            callback = statusCallback,
        )
        SharedVideoPlayer.registerTimeCallback(
            context = ptr,
            callbackCtx = null,
            callback = timeCallback,
            interval = TIME_OBSERVER_INTERVAL_SECONDS,
        )
        SharedVideoPlayer.registerFrameCallback(
            context = ptr,
            callbackCtx = null,
            callback = frameCallback,
        )
        SharedVideoPlayer.registerEndOfPlaybackCallback(
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
        SharedVideoPlayer.unregisterStatusCallback(ptr)
        SharedVideoPlayer.unregisterTimeCallback(ptr)
        SharedVideoPlayer.unregisterFrameCallback(ptr)
        SharedVideoPlayer.unregisterEndOfPlaybackCallback(ptr)
    }

    internal fun playNative() {
        val ptr = playerPointer ?: return
        SharedVideoPlayer.playVideo(ptr)
    }

    internal fun pauseNative() {
        val ptr = playerPointer ?: return
        SharedVideoPlayer.pauseVideo(ptr)
    }

    internal fun seekToNative(positionMs: Long) {
        val ptr = playerPointer ?: return
        SharedVideoPlayer.seekTo(
            context = ptr,
            time = positionMs.msToSeconds(),
        )
    }

    internal fun applyVolume() {
        val ptr = playerPointer ?: return
        SharedVideoPlayer.setVolume(
            context = ptr,
            volume = if (isMuted) 0f else 1f,
        )
    }

    private suspend fun updateFrameAsync() {
        val ptr = playerPointer ?: return

        val width = SharedVideoPlayer.getFrameWidth(ptr)
        val height = SharedVideoPlayer.getFrameHeight(ptr)
        if (width <= 0 || height <= 0) return

        val framePtr = SharedVideoPlayer.getLatestFrame(ptr) ?: return

        val pixelCount = width * height
        val frameSizeBytes = pixelCount.toLong() * 4L

        withContext(serialDispatcher) {
            val srcBuf = framePtr.getByteBuffer(0, frameSizeBytes)

            if (skiaBitmapA == null || skiaBitmapWidth != width || skiaBitmapHeight != height) {
                skiaBitmapA?.close()
                skiaBitmapB?.close()

                val imageInfo = ImageInfo(
                    width = width,
                    height = height,
                    colorType = ColorType.BGRA_8888,
                    alphaType = ColorAlphaType.OPAQUE,
                )
                skiaBitmapA = Bitmap().apply { allocPixels(imageInfo) }
                skiaBitmapB = Bitmap().apply { allocPixels(imageInfo) }
                skiaBitmapWidth = width
                skiaBitmapHeight = height
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
            val destSizeBytes = destRowBytes.toLong() * height.toLong()
            val destBuf = Pointer(pixelsAddr).getByteBuffer(0, destSizeBytes)
            copyBgraFrame(
                src = srcBuf,
                dst = destBuf,
                width = width,
                height = height,
                dstRowBytes = destRowBytes,
            )

            currentFrame = targetBitmap.asComposeImageBitmap()
        }

        lastFrameUpdateTime = System.currentTimeMillis()

        if (!hasRenderedFirstFrame) {
            hasRenderedFirstFrame = true
        }
    }

    /**
     * Re-creates the native player after [dispose]. Used by retry flows.
     */
    internal fun reinitialize() {
        playerPointer = SharedVideoPlayer.createVideoPlayer()
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
        skiaBitmapWidth = 0
        skiaBitmapHeight = 0
        nextSkiaBitmapA = true

        pointerToDispose?.let(SharedVideoPlayer::disposeVideoPlayer)

        currentFrame = null
    }
}

private const val TIME_OBSERVER_INTERVAL_SECONDS = 0.25

private const val STATUS_CODE_PAUSED = 0
private const val STATUS_CODE_PLAYING = 1
private const val STATUS_CODE_BUFFERING = 2

private fun Double.secondsToMs(): Long = (this * 1000).toLong()

private fun Long.msToSeconds(): Double = this.toDouble() / 1000.0
