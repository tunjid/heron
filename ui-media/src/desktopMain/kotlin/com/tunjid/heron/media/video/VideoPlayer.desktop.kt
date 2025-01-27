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