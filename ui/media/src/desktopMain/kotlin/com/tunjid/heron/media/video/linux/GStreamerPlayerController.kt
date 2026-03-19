package com.tunjid.heron.media.video.linux

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.tunjid.heron.media.video.PlayerStatus
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.media.video.VideoPlayerState
import java.util.EnumSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.freedesktop.gstreamer.Format
import org.freedesktop.gstreamer.event.SeekFlags

@Stable
class GStreamerPlayerController(
    private val appMainScope: CoroutineScope,
) : VideoPlayerController {

    override var isMuted: Boolean by mutableStateOf(true)

    private val idsToStates = mutableStateMapOf<String, GStreamerPlayerState>()
    private var activeVideoId: String by mutableStateOf("")

    init {
        GStreamer.ensureInitialized()

        snapshotFlow { isMuted }
            .onEach { idsToStates[activeVideoId]?.applyMute() }
            .launchIn(appMainScope)
    }

    override fun registerVideo(
        videoUrl: String,
        videoId: String,
        thumbnail: String?,
        isLooping: Boolean,
        autoplay: Boolean,
    ): VideoPlayerState {
        idsToStates[videoId]?.let { return it }

        trim()

        val state = GStreamerPlayerState(
            videoUrl = videoUrl,
            videoId = videoId,
            thumbnail = thumbnail,
            autoplay = autoplay,
            isLooping = isLooping,
            isMuted = derivedStateOf { isMuted },
            appMainScope = appMainScope,
        )
        idsToStates[videoId] = state
        return state
    }

    override fun play(videoId: String?, seekToMs: Long?) {
        val playerIdToPlay = videoId ?: activeVideoId
        val stateToPlay = idsToStates[playerIdToPlay] ?: return

        setActiveVideo(playerIdToPlay)

        if (!stateToPlay.isPipelineInitialized) {
            stateToPlay.createPipeline(stateToPlay.videoUrl)
            stateToPlay.playBin?.play()
            return
        }

        val alreadyPlaying = stateToPlay.status is PlayerStatus.Play.Confirmed

        if (stateToPlay.status is PlayerStatus.Pause && seekToMs == null) {
            stateToPlay.playBin?.play()
            return
        }

        if (alreadyPlaying && seekToMs == null) return

        if (alreadyPlaying && seekToMs != null) {
            stateToPlay.seekNative(seekToMs)
            return
        }

        val position = stateToPlay.seekPositionOnPlayMs(seekToMs)
        stateToPlay.playBin?.play()
        if (position > 0) stateToPlay.seekNative(position)
    }

    override fun pauseActiveVideo() {
        idsToStates[activeVideoId]?.apply {
            status = PlayerStatus.Pause.Requested
            playBin?.pause()
        }
    }

    override fun seekTo(position: Long) {
        idsToStates[activeVideoId]?.seekNative(position)
    }

    override fun getVideoStateById(videoId: String): VideoPlayerState? = idsToStates[videoId]

    override fun retry(videoId: String) {
        val stateToRetry = idsToStates[videoId] ?: return
        setActiveVideo(videoId)
        stateToRetry.dispose()
        stateToRetry.reinitialize()
        stateToRetry.createPipeline(stateToRetry.videoUrl)
        stateToRetry.playBin?.play()
    }

    override fun unregisterAll(retainedVideoIds: Set<String>): Set<String> {
        val toDispose = idsToStates.filterNot { retainedVideoIds.contains(it.key) }
        toDispose.keys.forEach { idsToStates.remove(it) }
        toDispose.values.forEach { it.dispose() }
        return retainedVideoIds - idsToStates.keys
    }

    private fun setActiveVideo(videoId: String) {
        idsToStates[videoId] ?: return
        val previousId = activeVideoId
        activeVideoId = videoId
        if (previousId == activeVideoId) return

        idsToStates[previousId]?.apply {
            status = PlayerStatus.Pause.Requested
            playBin?.pause()
        }
    }

    private fun trim() {
        val size = idsToStates.size
        if (size < MaxVideoStates) return
        idsToStates.keys
            .filter { idsToStates[it]?.status is PlayerStatus.Idle.Evicted }
            .take(size - MaxVideoStates)
            .mapNotNull { idsToStates.remove(it) }
            .forEach { it.dispose() }
    }
}

private fun GStreamerPlayerState.seekNative(positionMs: Long) {
    playBin?.seekSimple(
        Format.TIME,
        EnumSet.of(SeekFlags.FLUSH),
        positionMs * 1_000_000L,
    )
}

private fun VideoPlayerState.seekPositionOnPlayMs(seekToMs: Long?): Long =
    seekToMs ?: if (shouldReplay) 0L else lastPositionMs

private const val MaxVideoStates = 30
