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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A Compose state holder for managing [VideoPlayerState] instances across
 * platform-specific [VideoPlayerController] implementations.
 *
 * Encapsulates the common state management pattern shared by all controllers:
 * the map of registered states, the active video ID, and eviction of old states
 * when the maximum capacity is reached.
 */
@Stable
class VideoPlayerStates<S : VideoPlayerState>(
    private val onEvicted: (S) -> Unit = {},
) {
    private val idsToStates = mutableStateMapOf<String, S>()

    var isMuted: Boolean by mutableStateOf(true)

    var activeVideoId: String by mutableStateOf("")

    val activeState: S? get() = idsToStates[activeVideoId]

    operator fun get(videoId: String): S? = idsToStates[videoId]

    /**
     * Returns existing state for [videoId] if registered, otherwise calls [create]
     * after trimming evicted states. The [onEvicted] callback passed at construction
     * is invoked for each state removed during trim, allowing platforms to dispose
     * native resources.
     */
    fun registerOrGet(
        videoId: String,
        create: () -> S,
    ): S {
        idsToStates[videoId]?.let { return it }
        trim()
        return create().also { idsToStates[videoId] = it }
    }

    private fun trim() {
        val size = idsToStates.size
        if (size <= MaxVideoStates) return
        idsToStates.keys
            .toList()
            .filter { idsToStates[it]?.status is PlayerStatus.Idle.Evicted }
            .take(size - MaxVideoStates)
            .mapNotNull { idsToStates.remove(it) }
            .forEach(onEvicted)
    }
}

internal fun VideoPlayerState.seekPositionOnPlayMs(seekToMs: Long?): Long =
    seekToMs ?: if (shouldReplay) 0L else lastPositionMs

private const val MaxVideoStates = 30
