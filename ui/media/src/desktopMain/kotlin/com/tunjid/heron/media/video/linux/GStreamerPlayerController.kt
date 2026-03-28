package com.tunjid.heron.media.video.linux

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import com.tunjid.heron.media.video.PlayerStatus
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.media.video.VideoPlayerState
import com.tunjid.heron.media.video.VideoPlayerStates
import com.tunjid.heron.media.video.seekPositionOnPlayMs
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

    private val states = VideoPlayerStates(
        onEvicted = GStreamerPlayerState::dispose,
    )

    override var isMuted: Boolean by states::isMuted

    init {
        GStreamer.initialized

        snapshotFlow { isMuted }
            .onEach { states.activeState?.applyMute() }
            .launchIn(appMainScope)
    }

    override fun registerVideo(
        videoUrl: String,
        videoId: String,
        thumbnail: String?,
        isLooping: Boolean,
        autoplay: Boolean,
    ): VideoPlayerState = states.registerOrGet(
        videoId = videoId,
    ) {
        GStreamerPlayerState(
            videoUrl = videoUrl,
            videoId = videoId,
            thumbnail = thumbnail,
            autoplay = autoplay,
            isLooping = isLooping,
            isMuted = derivedStateOf { isMuted },
            appMainScope = appMainScope,
        )
    }

    override fun play(
        videoId: String?,
        seekToMs: Long?,
    ) {
        val playerIdToPlay = videoId ?: states.activeVideoId
        val stateToPlay = states[playerIdToPlay] ?: return

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
        if (position > 0) {
            stateToPlay.playBin?.pause()
            stateToPlay.seekNative(position)
        }
        stateToPlay.playBin?.play()
    }

    override fun pauseActiveVideo() {
        states.activeState?.apply {
            status = PlayerStatus.Pause.Requested
            playBin?.pause()
        }
    }

    override fun seekTo(position: Long) {
        states.activeState?.seekNative(position)
    }

    override fun getVideoStateById(videoId: String): VideoPlayerState? = states[videoId]

    override fun retry(videoId: String) {
        val stateToRetry = states[videoId] ?: return
        setActiveVideo(videoId)
        stateToRetry.dispose()
        stateToRetry.reinitialize()
        stateToRetry.createPipeline(stateToRetry.videoUrl)
        stateToRetry.playBin?.play()
    }

    private fun setActiveVideo(videoId: String) {
        states[videoId] ?: return
        val previousId = states.activeVideoId
        states.activeVideoId = videoId
        if (previousId == states.activeVideoId) return

        states[previousId]?.apply {
            status = PlayerStatus.Pause.Requested
            playBin?.pause()
        }
    }
}

private fun GStreamerPlayerState.seekNative(positionMs: Long) {
    playBin?.seekSimple(
        Format.TIME,
        EnumSet.of(SeekFlags.FLUSH),
        positionMs * 1_000_000L,
    )
}
