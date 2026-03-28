package com.tunjid.heron.media.video.linux

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State as ComposeState
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
import coil3.Bitmap
import com.sun.jna.Pointer
import com.tunjid.heron.media.video.PlayerStatus
import com.tunjid.heron.media.video.VideoPlayerState
import com.tunjid.heron.media.video.mac.copyBgraFrame
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import java.nio.ByteBuffer
import java.util.EnumSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.freedesktop.gstreamer.Bus
import org.freedesktop.gstreamer.Caps
import org.freedesktop.gstreamer.FlowReturn
import org.freedesktop.gstreamer.Format
import org.freedesktop.gstreamer.State
import org.freedesktop.gstreamer.elements.AppSink
import org.freedesktop.gstreamer.elements.PlayBin
import org.freedesktop.gstreamer.event.SeekFlags
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

@Stable
internal class GStreamerPlayerState(
    videoId: String,
    videoUrl: String,
    thumbnail: String?,
    isLooping: Boolean,
    autoplay: Boolean,
    isMuted: ComposeState<Boolean>,
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
        get() = (totalDuration - lastPositionMs) <= ReplayThresholdMs &&
            totalDuration != 0L &&
            !isLooping

    internal var currentFrame by mutableStateOf<ImageBitmap?>(
        value = null,
        policy = referentialEqualityPolicy(),
    )

    internal var playBin: PlayBin? = null
    private var appSink: AppSink? = null
    internal var isPipelineInitialized = false

    private var skiaBitmapWidth = 0
    private var skiaBitmapHeight = 0
    private var skiaBitmapA: Bitmap? = null
    private var skiaBitmapB: Bitmap? = null
    private var nextSkiaBitmapA = true

    private val serialDispatcher = Dispatchers.Default.limitedParallelism(1)
    private var positionPollingJob: Job? = null
    private var stateChangedConnection: Bus.STATE_CHANGED? = null
    private var eosConnection: Bus.EOS? = null
    private var errorConnection: Bus.ERROR? = null
    private var newSampleConnection: AppSink.NEW_SAMPLE? = null

    internal fun createPipeline(url: String) {
        val sink = buildAppSink()
        appSink = sink

        val pb = PlayBin("playbin-$videoId").apply {
            setURI(java.net.URI.create(url))
            setVideoSink(sink)
            set("mute", isMuted)
            connectBus(this)
        }
        playBin = pb
        isPipelineInitialized = true
    }

    private fun buildAppSink(): AppSink = AppSink("videosink").apply {
        set("emit-signals", true)
        caps = Caps.fromString("video/x-raw,format=BGRA")
        newSampleConnection = AppSink.NEW_SAMPLE { sink -> onNewSample(sink) }
            .also { connect(it) }
    }

    private fun connectBus(pb: PlayBin) {
        stateChangedConnection = Bus.STATE_CHANGED { source, old, current, _ ->
            if (source === pb) onStateChanged(pb, old, current)
        }.also { pb.bus.connect(it) }

        eosConnection = Bus.EOS { _ ->
            onEndOfStream(pb)
        }.also { pb.bus.connect(it) }

        errorConnection = Bus.ERROR { _, _, _ ->
            stopPositionPolling()
            status = PlayerStatus.Idle.Initial
        }.also { pb.bus.connect(it) }
    }

    private fun disconnectBus(pb: PlayBin) {
        stateChangedConnection?.let { pb.bus.disconnect(it) }
        eosConnection?.let { pb.bus.disconnect(it) }
        errorConnection?.let { pb.bus.disconnect(it) }
        stateChangedConnection = null
        eosConnection = null
        errorConnection = null
    }

    private fun onStateChanged(pb: PlayBin, old: State, current: State) {
        when (current) {
            State.PLAYING -> {
                status = PlayerStatus.Play.Confirmed
                if (!hasRenderedFirstFrame) hasRenderedFirstFrame = true
                syncMetadata(pb)
                startPositionPolling(pb)
            }
            State.PAUSED -> {
                status = PlayerStatus.Pause.Confirmed
                syncPosition(pb)
                stopPositionPolling()
            }
            State.READY,
            State.NULL,
            -> {
                if (old == State.PLAYING || old == State.PAUSED) syncPosition(pb)
                stopPositionPolling()
            }
            else -> Unit
        }
    }

    private fun onEndOfStream(pb: PlayBin) {
        stopPositionPolling()
        if (isLooping) {
            pb.seekSimple(Format.TIME, EnumSet.of(SeekFlags.FLUSH), 0L)
            pb.play()
        } else {
            lastPositionMs = totalDuration
            status = PlayerStatus.Pause.Confirmed
        }
    }

    private fun onNewSample(sink: AppSink): FlowReturn {
        val sample = sink.pullSample() ?: return FlowReturn.ERROR
        try {
            val structure = sample.caps?.getStructure(0) ?: return FlowReturn.OK
            val width = structure.getInteger("width")
            val height = structure.getInteger("height")
            if (width <= 0 || height <= 0) return FlowReturn.OK

            val rawBuffer: ByteBuffer = sample.buffer?.map(false) ?: return FlowReturn.OK
            val bytes = ByteArray(rawBuffer.remaining()).also(rawBuffer::get)

            appMainScope.launch { updateFrameAsync(bytes, width, height) }
        } finally {
            sample.dispose()
        }
        return FlowReturn.OK
    }

    private suspend fun updateFrameAsync(bytes: ByteArray, width: Int, height: Int) {
        withContext(serialDispatcher) {
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

            val target = if (nextSkiaBitmapA) requireNotNull(skiaBitmapA)
            else requireNotNull(skiaBitmapB)
            nextSkiaBitmapA = !nextSkiaBitmapA

            val pixmap = target.peekPixels() ?: return@withContext
            if (pixmap.addr == 0L) return@withContext

            val dstRowBytes = pixmap.rowBytes
            val dst = Pointer(pixmap.addr)
                .getByteBuffer(0, dstRowBytes.toLong() * height)

            copyBgraFrame(
                src = ByteBuffer.wrap(bytes),
                dst = dst,
                width = width,
                height = height,
                dstRowBytes = dstRowBytes,
            )

            currentFrame = target.asComposeImageBitmap()
            if (!hasRenderedFirstFrame) hasRenderedFirstFrame = true
            if (videoSize == IntSize.Zero) videoSize = IntSize(width, height)
        }
    }

    internal fun startPositionPolling(pb: PlayBin) {
        if (positionPollingJob?.isActive == true) return
        positionPollingJob = appMainScope.launch {
            while (isActive) {
                syncPosition(pb)
                delay(PositionPollIntervalMs)
            }
        }
    }

    internal fun stopPositionPolling() {
        positionPollingJob?.cancel()
        positionPollingJob = null
    }

    internal fun syncPosition(pb: PlayBin) {
        val posNs = pb.queryPosition(Format.TIME)
        if (posNs >= 0) lastPositionMs = posNs / NsToMs
    }

    internal fun syncMetadata(pb: PlayBin) {
        val durNs = pb.queryDuration(Format.TIME)
        if (durNs > 0) totalDuration = durNs / NsToMs
    }

    internal fun applyMute() {
        playBin?.set("mute", isMuted)
    }

    internal fun dispose() {
        stopPositionPolling()

        val pb = playBin
        playBin = null
        appSink = null
        isPipelineInitialized = false

        if (pb != null) {
            disconnectBus(pb)
            pb.stop()
            pb.dispose()
        }

        newSampleConnection?.let { appSink?.disconnect(it) }
        newSampleConnection = null

        skiaBitmapA?.close()
        skiaBitmapB?.close()
        skiaBitmapA = null
        skiaBitmapB = null
        skiaBitmapWidth = 0
        skiaBitmapHeight = 0
        nextSkiaBitmapA = true

        currentFrame = null
        hasRenderedFirstFrame = false
        status = PlayerStatus.Idle.Initial
    }

    internal fun reinitialize() {
        isPipelineInitialized = false
        hasRenderedFirstFrame = false
        status = PlayerStatus.Idle.Initial
    }
}

private const val PositionPollIntervalMs = 250L
private const val NsToMs = 1_000_000L
private const val ReplayThresholdMs = 200L
