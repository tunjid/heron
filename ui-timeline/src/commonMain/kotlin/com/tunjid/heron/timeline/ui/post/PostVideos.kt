package com.tunjid.heron.timeline.ui.post

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.models.aspectRatioOrSquare
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.media.video.VideoPlayer
import com.tunjid.heron.media.video.rememberUpdatedVideoPlayerState
import com.tunjid.heron.ui.SharedElementScope
import com.tunjid.heron.ui.shapes.toRoundedPolygonShape
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PostVideo(
    video: Video,
    sharedElementPrefix: String,
    sharedElementScope: SharedElementScope,
    onClicked: () -> Unit,
) {
    val videoPlayerState = LocalVideoPlayerController.current.rememberUpdatedVideoPlayerState(
        videoUrl = video.playlist.uri,
        thumbnail = video.thumbnail?.uri,
        shape = remember {
            RoundedCornerShape(16.dp).toRoundedPolygonShape()
        }
    )
    sharedElementScope.updatedMovableSharedElementOf(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(video.aspectRatioOrSquare)
            .clickable { onClicked() },
        key = video.sharedElementKey(
            prefix = sharedElementPrefix
        ),
        state = videoPlayerState,
        sharedElement = { state, innerModifier ->
            VideoPlayer(
                modifier = innerModifier,
                state = state
            )
        }
    )
}

fun Video.sharedElementKey(
    prefix: String,
) = "$prefix-${playlist.uri}"
