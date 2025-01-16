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