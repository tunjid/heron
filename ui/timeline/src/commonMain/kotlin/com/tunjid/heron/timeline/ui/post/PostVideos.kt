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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.models.aspectRatioOrSquare
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.media.video.PlayerStatus
import com.tunjid.heron.media.video.VideoPlayer
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.media.video.VideoPlayerState
import com.tunjid.heron.media.video.VideoStill
import com.tunjid.heron.media.video.formatVideoDuration
import com.tunjid.heron.media.video.rememberUpdatedVideoPlayerState
import com.tunjid.heron.timeline.utilities.sensitiveContentBlur
import com.tunjid.heron.ui.isPrimaryOrActive
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import com.tunjid.treenav.compose.UpdatedMovableStickySharedElementOf
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.mute_video
import heron.ui.timeline.generated.resources.pause_video
import heron.ui.timeline.generated.resources.play_video
import heron.ui.timeline.generated.resources.unmute_video
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PostVideo(
    modifier: Modifier = Modifier,
    video: Video,
    postUri: PostUri,
    sharedElementPrefix: String,
    isBlurred: Boolean,
    matchHeightConstraintsFirst: Boolean,
    presentation: Timeline.Presentation,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    onClicked: () -> Unit,
) {
    val videoPlayerController = LocalVideoPlayerController.current
    val videoPlayerState = videoPlayerController.rememberUpdatedVideoPlayerState(
        videoUrl = video.playlist.uri,
        thumbnail = video.thumbnail?.uri,
        shape = presentation.videoShape,
    )
    Box(
        modifier = modifier
            .aspectRatio(
                ratio = when (presentation) {
                    Timeline.Presentation.Media.Condensed,
                    Timeline.Presentation.Media.Expanded,
                    Timeline.Presentation.Text.WithEmbed,
                    -> video.aspectRatioOrSquare
                    Timeline.Presentation.Media.Grid -> 1f
                },
                matchHeightConstraintsFirst = matchHeightConstraintsFirst,
            ),
    ) {
        val videoModifier = when {
            isBlurred -> Modifier.sensitiveContentBlur(videoPlayerState.shape)
            else -> Modifier
        }
            .fillMaxSize()
            .clickable {
                videoPlayerController.play(videoId = video.playlist.uri)
                onClicked()
            }
        if (!paneMovableElementSharedTransitionScope.isPrimaryOrActive) VideoStill(
            modifier = videoModifier,
            state = videoPlayerState,
        )
        else paneMovableElementSharedTransitionScope.UpdatedMovableStickySharedElementOf(
            modifier = videoModifier,
            sharedContentState = with(paneMovableElementSharedTransitionScope) {
                rememberSharedContentState(
                    key = video.sharedElementKey(
                        prefix = sharedElementPrefix,
                        postUri = postUri,
                    ),
                )
            },
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
            },
        )
        PlayerInfo(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
            videoPlayerState = videoPlayerState,
            videoPlayerController = videoPlayerController,
        )

        PlayButton(
            modifier = when {
                isBlurred -> Modifier.blur(
                    radius = 2.dp,
                    edgeTreatment = BlurredEdgeTreatment(CircleShape),
                )
                else -> Modifier
            }
                .align(
                    when (presentation) {
                        Timeline.Presentation.Media.Condensed,
                        Timeline.Presentation.Media.Grid,
                        -> Alignment.TopEnd
                        Timeline.Presentation.Media.Expanded,
                        Timeline.Presentation.Text.WithEmbed,
                        -> Alignment.Center
                    },
                ),
            presentation = presentation,
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
        visible = videoPlayerState.status is PlayerStatus.Play.Confirmed,
    ) {
        Row {
            PlayerControlBackground(
                onClicked = {
                    videoPlayerController.pauseActiveVideo()
                },
                content = {
                    Icon(
                        modifier = Modifier
                            .padding(4.dp),
                        contentDescription = stringResource(Res.string.pause_video),
                        imageVector = Icons.Rounded.Pause,
                    )
                },
            )
            PlayerControlBackground {
                BasicText(
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    text = videoPlayerState.lastPositionMs.formatVideoDuration(),
                    style = MaterialTheme.typography.bodySmall,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 8.sp,
                        maxFontSize = 10.sp,
                    ),
                    color = Color.Companion::White,
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
                            .padding(4.dp),
                        contentDescription = stringResource(
                            if (videoPlayerController.isMuted) Res.string.mute_video
                            else Res.string.unmute_video,
                        ),
                        imageVector =
                        if (videoPlayerState.isMuted) Icons.AutoMirrored.Rounded.VolumeOff
                        else Icons.AutoMirrored.Rounded.VolumeUp,
                    )
                },
            )
        }
    }
}

@Composable
private fun PlayButton(
    modifier: Modifier,
    presentation: Timeline.Presentation,
    videoPlayerState: VideoPlayerState,
    videoPlayerController: VideoPlayerController,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = videoPlayerState.status !is PlayerStatus.Play,
    ) {
        Box(
            modifier = Modifier
                .size(presentation.playButtonBackgroundSize)
                .background(
                    color = presentation.playButtonBackgroundColor,
                    shape = CircleShape,
                )
                .clip(CircleShape)
                .clickable { videoPlayerController.play(videoPlayerState.videoId) },
        ) {
            Icon(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(presentation.playButtonIconSize),
                contentDescription = stringResource(Res.string.play_video),
                imageVector = presentation.playButtonIcon,
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
            .clip(CircleShape)
            .clickable {
                onClicked()
            }
            .padding(all = 8.dp)
            .background(
                color = color,
                shape = CircleShape,
            )
            .height(24.dp),
    ) {
        Box(
            modifier = Modifier.align(Alignment.Center),
            content = { content() },
        )
    }
}

private val Timeline.Presentation.videoShape
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed -> TextWithEmbedShape
        Timeline.Presentation.Media.Condensed -> CondensedShape
        Timeline.Presentation.Media.Expanded -> ExpandedShape
        Timeline.Presentation.Media.Grid -> GridShape
    }

private val Timeline.Presentation.playButtonBackgroundSize
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed,
        Timeline.Presentation.Media.Expanded,
        -> 56.dp

        Timeline.Presentation.Media.Condensed,
        Timeline.Presentation.Media.Grid,
        -> 36.dp
    }

private val Timeline.Presentation.playButtonIconSize
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed,
        Timeline.Presentation.Media.Expanded,
        -> 36.dp

        Timeline.Presentation.Media.Condensed,
        Timeline.Presentation.Media.Grid,
        -> 24.dp
    }

private val Timeline.Presentation.playButtonBackgroundColor
    @Composable get() = when (this) {
        Timeline.Presentation.Text.WithEmbed,
        Timeline.Presentation.Media.Expanded,
        -> MaterialTheme.colorScheme.secondaryContainer

        Timeline.Presentation.Media.Condensed,
        Timeline.Presentation.Media.Grid,
        -> Color.Transparent
    }

private val Timeline.Presentation.playButtonIcon
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed,
        Timeline.Presentation.Media.Expanded,
        -> Icons.Rounded.PlayArrow

        Timeline.Presentation.Media.Condensed,
        Timeline.Presentation.Media.Grid,
        -> Icons.Rounded.Movie
    }

fun Video.sharedElementKey(
    prefix: String,
    postUri: PostUri,
) = "$prefix-$postUri-${playlist.uri}"

private val TextWithEmbedShape = RoundedPolygonShape.RoundedRectangle(percent = 0.05f)
private val CondensedShape = RoundedPolygonShape.RoundedRectangle(percent = 0.0001f)
private val ExpandedShape = RoundedPolygonShape.RoundedRectangle(percent = 0f)
private val GridShape = RoundedPolygonShape.RoundedRectangle(percent = 0f)
