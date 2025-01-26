package com.tunjid.heron.timeline.ui.post

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.AutoSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.models.aspectRatioOrSquare
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.media.video.PlayerStatus
import com.tunjid.heron.media.video.VideoPlayer
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.media.video.VideoPlayerState
import com.tunjid.heron.media.video.VideoStill
import com.tunjid.heron.media.video.formatVideoDuration
import com.tunjid.heron.media.video.rememberUpdatedVideoPlayerState
import com.tunjid.heron.ui.SharedElementScope
import com.tunjid.heron.ui.shapes.toRoundedPolygonShape
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import com.tunjid.treenav.compose.threepane.ThreePane

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PostVideo(
    video: Video,
    sharedElementPrefix: String,
    sharedElementScope: SharedElementScope,
    onClicked: () -> Unit,
) {
    val videoPlayerController = LocalVideoPlayerController.current
    val videoPlayerState = videoPlayerController.rememberUpdatedVideoPlayerState(
        videoUrl = video.playlist.uri,
        thumbnail = video.thumbnail?.uri,
        shape = remember {
            RoundedCornerShape(16.dp).toRoundedPolygonShape()
        }
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(video.aspectRatioOrSquare)
    ) {
        val videoModifier = Modifier
            .fillMaxSize()
            .clickable {
                videoPlayerController.play(videoId = video.playlist.uri)
                onClicked()
            }
        if (sharedElementScope.paneState.pane != ThreePane.Primary) VideoStill(
            modifier = videoModifier,
            state = videoPlayerState,
        )
        else sharedElementScope.updatedMovableSharedElementOf(
            modifier = videoModifier,
            key = video.sharedElementKey(
                prefix = sharedElementPrefix
            ),
            state = videoPlayerState,
            alternateOutgoingSharedElement = { state, innerModifier ->
                VideoStill(
                    modifier = innerModifier,
                    state = state,
                )
            },
            sharedElement = { state, innerModifier ->
                VideoPlayer(
                    modifier = innerModifier,
                    state = state,
                )
            }
        )
        PlayerInfo(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    horizontal = 8.dp,
                )
                .fillMaxWidth(),
            videoPlayerState = videoPlayerState,
            videoPlayerController = videoPlayerController,
        )

        PlayButton(
            modifier = Modifier
                .align(Alignment.Center),
            videoPlayerState = videoPlayerState,
            videoPlayerController = videoPlayerController,
        )
    }
}

@Composable
private fun PlayerInfo(
    modifier: Modifier = Modifier,
    videoPlayerState: VideoPlayerState,
    videoPlayerController: VideoPlayerController,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = videoPlayerState.status is PlayerStatus.Play.Confirmed
    ) {
        Row {
            PlayerControlBackground {
                BasicText(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    text = videoPlayerState.lastPositionMs.formatVideoDuration(),
                    style = MaterialTheme.typography.bodySmall,
                    autoSize = AutoSize.StepBased(
                        minFontSize = 8.sp,
                        maxFontSize = 10.sp,
                    ),
                    color = Color.Companion::White
                )
            }
            Spacer(Modifier.weight(1f))
            PlayerControlBackground(
                onClicked = {
                    videoPlayerController.isMuted = !videoPlayerController.isMuted
                },
                content = {
                    Icon(
                        modifier = Modifier
                            .padding(2.dp),
                        contentDescription = "",
                        imageVector =
                        if (videoPlayerState.isMuted) Icons.AutoMirrored.Rounded.VolumeUp
                        else Icons.AutoMirrored.Rounded.VolumeOff,
                    )
                }
            )
        }
    }
}

@Composable
private fun PlayButton(
    modifier: Modifier,
    videoPlayerState: VideoPlayerState,
    videoPlayerController: VideoPlayerController,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = videoPlayerState.status !is PlayerStatus.Play
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .clickable { videoPlayerController.play(videoPlayerState.videoId) }
        ) {
            Icon(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp),
                contentDescription = "",
                imageVector = Icons.Rounded.PlayArrow,
            )
        }
    }
}

@Composable
private fun PlayerControlBackground(
    onClicked: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val color = Color.Black.copy(alpha = 0.6f)
    Box(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .background(
                color = color,
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable {
                onClicked()
            }
            .height(16.dp),
    ) {
        Box(
            modifier = Modifier.align(Alignment.Center),
            content = { content() }
        )
    }
}

fun Video.sharedElementKey(
    prefix: String,
) = "$prefix-${playlist.uri}"
