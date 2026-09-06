package com.tunjid.heron.media

import androidx.collection.IntList
import androidx.collection.intListOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.IntSize

@Stable
class MediaConfig(
    val windowSize: () -> IntSize,
    val autoPlayGifs: () -> Boolean,
    val imageSizeBuckets: () -> IntList = ::DefaultImageSizeBuckets,
)

private val DefaultImageSizeBuckets = intListOf(
    96, 128, 256, 320, 480, 640,
    720, 1024, 1280, 1536, 1920,
)
val LocalMediaConfig = staticCompositionLocalOf<MediaConfig> {
    throw IllegalArgumentException("MediaConfig has not been provided")
}
