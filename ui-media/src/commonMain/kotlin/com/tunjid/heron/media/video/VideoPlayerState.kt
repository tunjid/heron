package com.tunjid.heron.media.video

import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale

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

    val alignment: Alignment

    val contentScale: ContentScale

    // Business logic attributes

    val videoId: String

    val autoplay: Boolean

    val videoUrl: String

    val isLooping: Boolean

    val isMuted: Boolean

    val shouldReplay: Boolean

    // Player managed attributes

    val lastPositionMs: Long

    val totalDuration: Long

    val status: PlayerStatus

    val hasRenderedFirstFrame: Boolean
}