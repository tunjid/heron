package com.tunjid.heron.media.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs

@Composable
actual fun VideoPlayer(
    modifier: Modifier,
    state: VideoPlayerState,
) {
    AsyncImage(
        modifier = modifier,
        args = remember(state.thumbnailUrl, state.contentScale, state.alignment, state.shape) {
            ImageArgs(
                url = state.thumbnailUrl,
                contentDescription = null,
                alignment = state.alignment,
                contentScale = state.contentScale,
                shape = state.shape,
            )
        },
    )
}