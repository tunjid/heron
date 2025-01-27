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

import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.tunjid.heron.ui.shapes.RoundedPolygonShape

@Stable
sealed class PlayerStatus {
    sealed class Idle : PlayerStatus() {
        data object Initial : Idle()

        data object Evicted : Idle()
    }

    sealed class Play : PlayerStatus() {
        data object Requested : Play()

        data object Confirmed : Play()
    }

    sealed class Pause : PlayerStatus() {
        data object Requested : Pause()

        data object Confirmed : Pause()
    }
}

/**
 * Thin wrapper of essential video information.
 */
@Stable
interface VideoPlayerState {

    // UI logic attributes

    var alignment: Alignment

    var contentScale: ContentScale

    var shape: RoundedPolygonShape

    // Business logic attributes

    val videoId: String

    val autoplay: Boolean

    val videoUrl: String

    var thumbnailUrl: String?

    val isLooping: Boolean

    val isMuted: Boolean

    val shouldReplay: Boolean

    // Player managed attributes

    val lastPositionMs: Long

    val totalDuration: Long

    val status: PlayerStatus

    val hasRenderedFirstFrame: Boolean

    val videoStill: ImageBitmap?
}