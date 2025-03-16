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

package com.tunjid.heron.data.core.models

import kotlinx.serialization.Serializable

sealed interface Embed {
    @Serializable
    sealed interface Media : Embed, ByteSerializable
}

@Serializable
sealed class MediaFile : ByteSerializable {
    abstract val data: ByteArray
    abstract val width: Long
    abstract val height: Long


    @Serializable
    class Photo(
        override val data: ByteArray,
        override val width: Long,
        override val height: Long,
        val altText: String = "",
    ) : MediaFile()

    @Serializable
    class Video(
        override val data: ByteArray,
        override val width: Long,
        override val height: Long,
        val altText: String? = null,
    ) : MediaFile()
}

/**
 * Provides the width and height of an item if available.
 *
 */
sealed interface AspectRatio {
    val width: Long?
    val height: Long?
}

val AspectRatio.aspectRatioOrSquare: Float
    get() = withWidthAndHeight(1f) { width, height ->
        width.toFloat() / height
    }

val AspectRatio.isLandscape
    get() = withWidthAndHeight(false) { width, height ->
        width > height
    }

private inline fun <T> AspectRatio.withWidthAndHeight(
    default: T,
    crossinline block: (Long, Long) -> T,
): T {
    val currentWidth = width ?: return default
    val currentHeight = height ?: return default
    return block(currentWidth, currentHeight)
}

@Serializable
data object UnknownEmbed : Embed