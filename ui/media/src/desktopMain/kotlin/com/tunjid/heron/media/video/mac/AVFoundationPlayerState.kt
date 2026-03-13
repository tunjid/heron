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

import androidx.compose.runtime.MutableState
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    // Native player pointer
    private val playerMutex = Mutex()
    private val frameMutex = Mutex()
    internal var playerPtr: Pointer? = null

    // Frame rendering state
    private val currentFrameFlow = MutableStateFlow<ImageBitmap?>(null)
    internal val currentFrameState: MutableState<ImageBitmap?> = mutableStateOf(null)
    private var skiaBitmapWidth: Int = 0
    private var skiaBitmapHeight: Int = 0
    private var skiaBitmapA: Bitmap? = null
    private var skiaBitmapB: Bitmap? = null
    private var nextSkiaBitmapA: Boolean = true
    private var lastFrameHash: Int = Int.MIN_VALUE
    private var lastFrameUpdateTime: Long = 0

    // Jobs
    private var frameUpdateJob: Job? = null

    @OptIn(FlowPreview::class)
    internal fun startUIUpdateJob(scope: CoroutineScope): Job = scope.launch {
        currentFrameFlow.debounce(1).collect { newFrame ->
            ensureActive()
            withContext(Dispatchers.Main) {
                currentFrameState.value = newFrame
            }
        }
    }

    internal suspend fun initNativePlayer() {
        val ptr = SharedVideoPlayer.createVideoPlayer()
        if (ptr != null) {
            playerMutex.withLock { playerPtr = ptr }
            applyVolume()
        }
    }

    internal suspend fun openMedia(uri: String) {
        val ptr = playerMutex.withLock { playerPtr } ?: return
        SharedVideoPlayer.openUri(ptr, uri)
        pollDimensionsUntilReady(ptr)
        updateMetadata()
    }

    private suspend fun pollDimensionsUntilReady(ptr: Pointer, maxAttempts: Int = 20) {
        for (attempt in 1..maxAttempts) {
            val width = SharedVideoPlayer.getFrameWidth(ptr)
            val height = SharedVideoPlayer.getFrameHeight(ptr)
            if (width > 0 && height > 0) return
            delay(250)
        }
    }

    internal suspend fun updateMetadata() {
        val ptr = playerMutex.withLock { playerPtr } ?: return
        try {
            val width = SharedVideoPlayer.getFrameWidth(ptr)
            val height = SharedVideoPlayer.getFrameHeight(ptr)
            val duration = SharedVideoPlayer.getVideoDuration(ptr)

            withContext(Dispatchers.Main) {
                if (width > 0 && height > 0) {
                    videoSize = IntSize(width, height)
                }
                if (duration > 0) {
                    totalDuration = (duration * 1000).toLong()
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    internal suspend fun playNative(scope: CoroutineScope) {
        val ptr = playerMutex.withLock { playerPtr } ?: return
        try {
            SharedVideoPlayer.playVideo(ptr)
            withContext(Dispatchers.Main) {
                status = PlayerStatus.Play.Confirmed
                if (!hasRenderedFirstFrame) {
                    hasRenderedFirstFrame = true
                }
            }
            startFrameUpdates(scope)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    internal suspend fun pauseNative() {
        val ptr = playerMutex.withLock { playerPtr } ?: return
        try {
            SharedVideoPlayer.pauseVideo(ptr)
            withContext(Dispatchers.Main) {
                status = PlayerStatus.Pause.Confirmed
            }
            stopFrameUpdates()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    internal suspend fun seekToNative(positionMs: Long) {
        val ptr = playerMutex.withLock { playerPtr } ?: return
        try {
            SharedVideoPlayer.seekTo(ptr, positionMs.toDouble() / 1000.0)
            delay(10)
            updateFrameAsync()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    internal suspend fun applyVolume() {
        playerMutex.withLock {
            playerPtr?.let { ptr ->
                try {
                    SharedVideoPlayer.setVolume(ptr, if (isMuted) 0f else 1f)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
            }
        }
    }

    internal fun startFrameUpdates(scope: CoroutineScope) {
        stopFrameUpdates()
        frameUpdateJob = scope.launch {
            while (isActive) {
                ensureActive()
                updateFrameAsync()
                updatePositionAsync()
                delay(33) // ~30fps
            }
        }
    }

    internal fun stopFrameUpdates() {
        frameUpdateJob?.cancel()
        frameUpdateJob = null
    }

    private suspend fun updateFrameAsync() {
        frameMutex.withLock {
            try {
                val ptr = playerMutex.withLock { playerPtr } ?: return

                val width = SharedVideoPlayer.getFrameWidth(ptr)
                val height = SharedVideoPlayer.getFrameHeight(ptr)
                if (width <= 0 || height <= 0) return

                val framePtr = SharedVideoPlayer.getLatestFrame(ptr) ?: return

                val pixelCount = width * height
                val frameSizeBytes = pixelCount.toLong() * 4L

                withContext(Dispatchers.Default) {
                    val srcBuf = framePtr.getByteBuffer(0, frameSizeBytes)

                    val newHash = calculateFrameHash(srcBuf, pixelCount)
                    if (newHash == lastFrameHash) return@withContext
                    lastFrameHash = newHash

                    if (skiaBitmapA == null || skiaBitmapWidth != width || skiaBitmapHeight != height) {
                        skiaBitmapA?.close()
                        skiaBitmapB?.close()

                        val imageInfo = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE)
                        skiaBitmapA = Bitmap().apply { allocPixels(imageInfo) }
                        skiaBitmapB = Bitmap().apply { allocPixels(imageInfo) }
                        skiaBitmapWidth = width
                        skiaBitmapHeight = height
                        nextSkiaBitmapA = true
                    }

                    val targetBitmap = if (nextSkiaBitmapA) skiaBitmapA!! else skiaBitmapB!!
                    nextSkiaBitmapA = !nextSkiaBitmapA

                    val pixmap = targetBitmap.peekPixels() ?: return@withContext
                    val pixelsAddr = pixmap.addr
                    if (pixelsAddr == 0L) return@withContext

                    srcBuf.rewind()
                    val destRowBytes = pixmap.rowBytes.toInt()
                    val destSizeBytes = destRowBytes.toLong() * height.toLong()
                    val destBuf = Pointer(pixelsAddr).getByteBuffer(0, destSizeBytes)
                    copyBgraFrame(srcBuf, destBuf, width, height, destRowBytes)

                    currentFrameFlow.value = targetBitmap.asComposeImageBitmap()
                }

                lastFrameUpdateTime = System.currentTimeMillis()

                if (!hasRenderedFirstFrame) {
                    withContext(Dispatchers.Main) {
                        hasRenderedFirstFrame = true
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    private suspend fun updatePositionAsync() {
        val ptr = playerMutex.withLock { playerPtr } ?: return
        try {
            val current = SharedVideoPlayer.getCurrentTime(ptr)
            val duration = SharedVideoPlayer.getVideoDuration(ptr)

            withContext(Dispatchers.Main) {
                if (current >= 0) {
                    lastPositionMs = (current * 1000).toLong()
                }
                if (duration > 0) {
                    totalDuration = (duration * 1000).toLong()
                }
            }

            // Check for looping
            if (duration > 0 && current >= duration - 0.5) {
                if (isLooping) {
                    SharedVideoPlayer.seekTo(ptr, 0.0)
                } else {
                    withContext(Dispatchers.Main) {
                        status = PlayerStatus.Pause.Confirmed
                    }
                    SharedVideoPlayer.pauseVideo(ptr)
                    stopFrameUpdates()
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    internal suspend fun dispose() {
        stopFrameUpdates()

        val ptrToDispose = frameMutex.withLock {
            val ptr = playerMutex.withLock {
                val p = playerPtr
                playerPtr = null
                p
            }

            skiaBitmapA?.close()
            skiaBitmapB?.close()
            skiaBitmapA = null
            skiaBitmapB = null
            skiaBitmapWidth = 0
            skiaBitmapHeight = 0
            nextSkiaBitmapA = true
            lastFrameHash = Int.MIN_VALUE

            ptr
        }

        ptrToDispose?.let {
            try {
                SharedVideoPlayer.disposeVideoPlayer(it)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }

        currentFrameFlow.value = null
    }
}
