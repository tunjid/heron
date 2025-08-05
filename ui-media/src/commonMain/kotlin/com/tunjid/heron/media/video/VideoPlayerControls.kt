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

package com.tunjid.heron.media.video

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward5
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Replay5
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import heron.ui_media.generated.resources.Res
import heron.ui_media.generated.resources.pause
import heron.ui_media.generated.resources.play
import heron.ui_media.generated.resources.rewind_5
import heron.ui_media.generated.resources.skip_5
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import org.jetbrains.compose.resources.stringResource
import kotlin.math.max
import kotlin.math.min

@Composable
private fun PlaybackControls(
    modifier: Modifier = Modifier,
    controlsState: PlayerControlsUiState,
    videoPlayerState: VideoPlayerState,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        controlsState.playerControlStates.forEach { state ->
            Icon(
                modifier = Modifier
                    .size(state.size)
                    .clip(CircleShape)
                    .clickable {
                        controlsState.interactWith(
                            playerControlState = state,
                            videoPlayerState = videoPlayerState,
                        )
                    },
                imageVector = state.icon,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = stringResource(state.stringRes),
            )
        }
    }
}

@Composable
fun PlaybackStatus(
    modifier: Modifier = Modifier,
    controlsState: PlayerControlsUiState,
    videoPlayerState: VideoPlayerState,
) {
    Column(
        modifier = modifier,
    ) {
        var adjustedPosition by remember { mutableFloatStateOf(Float.NaN) }
        Slider(
            modifier = Modifier
                .fillMaxWidth(),
            value = when {
                adjustedPosition.isNaN() -> videoPlayerState.lastPositionMs.toFloat()
                else -> adjustedPosition
            },
            onValueChange = { newValue ->
                controlsState.onInteracted()
                adjustedPosition = newValue
            },
            valueRange = 0f..videoPlayerState.totalDuration.toFloat(),
            interactionSource = controlsState.controlsInteractionSource,
            onValueChangeFinished = {
                if (!adjustedPosition.isNaN()) controlsState.videoPlayerController.play(
                    videoId = videoPlayerState.videoId,
                    seekToMs = adjustedPosition.toLong(),
                )
                adjustedPosition = Float.NaN
                controlsState.onInteracted()
            },
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember { MutableInteractionSource() },
                    colors = SliderDefaults.colors(),
                    enabled = true,
                    thumbSize = DpSize(20.dp, 20.dp)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    modifier = Modifier.height(4.dp),
                    sliderState = sliderState,
                    thumbTrackGapSize = 0.dp,
                )
            },
        )
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = videoPlayerState.lastPositionMs.formatVideoDuration()
            )
            PlaybackControls(
                modifier = Modifier,
                videoPlayerState = videoPlayerState,
                controlsState = controlsState,
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.outline)) {
                        append(videoPlayerState.totalDuration.formatVideoDuration())
                    }
                }
            )
        }
    }
}

@Stable
class PlayerControlsUiState(
    val videoPlayerController: VideoPlayerController,
) {
    var playerControlsVisible by mutableStateOf(false)
    val controlsInteractionSource = MutableInteractionSource()
    var interactionCount by mutableLongStateOf(0)

    internal val playerControlStates = mutableStateListOf(
        PlayerControlState(Icons.Rounded.Replay5, 30.dp),
        PlayerControlState(Icons.Rounded.PlayCircle, 30.dp),
        PlayerControlState(Icons.Rounded.Forward5, 30.dp),
    )

    fun toggleVisibility() {
        playerControlsVisible = !playerControlsVisible
    }

    fun update(
        status: PlayerStatus,
    ) {
        playerControlStates[1].icon = when (status) {
            is PlayerStatus.Play -> Icons.Rounded.PauseCircle
            else -> Icons.Rounded.PlayCircle
        }
    }

    fun onInteracted() {
        ++interactionCount
    }
}

@Composable
fun PlayerControlsUiState.ControlsVisibilityEffect() {
    val sliderDragged = controlsInteractionSource.collectIsDraggedAsState()

    LaunchedEffect(Unit) {
        merge(
            snapshotFlow { interactionCount },
            snapshotFlow { playerControlsVisible || sliderDragged.value }
        )
            .collectLatest {
                playerControlsVisible = playerControlsVisible || sliderDragged.value
                if (!playerControlsVisible) return@collectLatest

                delay(ControlsVisibilityDurationMs)
                playerControlsVisible = false
            }
    }
}

private fun PlayerControlsUiState.interactWith(
    playerControlState: PlayerControlState,
    videoPlayerState: VideoPlayerState,
) {
    when (playerControlState.icon) {
        Icons.Rounded.PlayCircle,
        Icons.Rounded.PauseCircle,
            -> when (videoPlayerState.status) {
            is PlayerStatus.Play -> videoPlayerController.pauseActiveVideo()
            else -> videoPlayerController.play(videoPlayerState.videoId)
        }

        Icons.Rounded.Forward5 -> videoPlayerController.play(
            videoId = videoPlayerState.videoId,
            seekToMs = min(
                a = videoPlayerState.totalDuration,
                b = videoPlayerState.lastPositionMs + SkipDurationMS,
            )
        )

        Icons.Rounded.Replay5 -> videoPlayerController.play(
            videoId = videoPlayerState.videoId,
            seekToMs = max(
                a = 0,
                b = videoPlayerState.lastPositionMs - SkipDurationMS,
            )
        )

        else -> Unit
    }
    onInteracted()
}

@Stable
internal class PlayerControlState(
    icon: ImageVector,
    val size: Dp,
) {
    var icon by mutableStateOf(icon)
}

private val PlayerControlState.stringRes
    get() = when (icon) {
        Icons.Rounded.PlayCircle -> Res.string.play
        Icons.Rounded.PauseCircle -> Res.string.pause
        Icons.Rounded.Forward5 -> Res.string.skip_5
        Icons.Rounded.Replay5 -> Res.string.rewind_5
        else -> Res.string.play
    }

private const val SkipDurationMS = 5000L

private const val ControlsVisibilityDurationMs = 4000L