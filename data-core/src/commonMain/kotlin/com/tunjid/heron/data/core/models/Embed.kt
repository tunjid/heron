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
    sealed interface Media: Embed, ByteSerializable
}

sealed interface AspectRatio {
    val width: Long?
    val height: Long?
}

val AspectRatio.aspectRatioOrSquare: Float
    get() {
        val currentWidth = width
        val currentHeight = height
        return if (currentWidth != null && currentHeight != null) currentWidth.toFloat() / currentHeight
        else 1f
    }

@Serializable
data object UnknownEmbed : Embed