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

package com.tunjid.heron.images

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.skiaCanvas
import coil3.BitmapImage
import coil3.ComponentRegistry
import coil3.PlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal actual fun coil3.Image.renderInto(
    canvas: Canvas,
) {
    when (this) {
        is BitmapImage -> canvas.drawImage(
            image = bitmap.asComposeImageBitmap(),
            topLeftOffset = Offset.Zero,
            paint = DefaultPaint,
        )
        else -> draw(
            canvas = canvas.skiaCanvas,
        )
    }
}

fun imageLoader(): ImageLoader = CoilImageLoader.create(
    context = PlatformContext.INSTANCE,
)

internal actual fun ComponentRegistry.Builder.addPlatformDecoders() {
    add(AnimatedSkiaImageDecoder.Factory())
}

@Composable
internal actual fun coil3.Image.AnimationEffect() {
    val image = this as? AnimatedSkiaImage ?: return
    if (image.frameCount <= 1) return

    LaunchedEffect(image) {
        coroutineScope {
            // Decode remaining frames in the background
            launch(Dispatchers.Default) {
                for (index in image.frames.indices) {
                    if (image.frames[index] == null) {
                        image.frames[index] = image.decodeFrame(index)
                    }
                }
                image.closeTempBitmap()
            }

            // Advance frames using the Compose frame clock
            var startNanos = -1L
            while (true) {
                withFrameNanos { frameTimeNanos ->
                    if (startNanos < 0) startNanos = frameTimeNanos
                    val elapsedMs = (frameTimeNanos - startNanos) / 1_000_000

                    val singleIterationMs = image.singleIterationDurationMs
                    if (singleIterationMs <= 0) return@withFrameNanos

                    val currentIterationElapsedMs = (elapsedMs % singleIterationMs).toInt()
                    val cumulative = image.cumulativeFrameDurationsMs
                    val searchResult = cumulative.binarySearch(currentIterationElapsedMs)

                    // binarySearch returns index if found, or -(insertionPoint + 1) if not.
                    // The frame to show is the one whose cumulative end time exceeds elapsed.
                    val frameIndex = if (searchResult >= 0) {
                        (searchResult + 1).coerceAtMost(cumulative.lastIndex)
                    } else {
                        -(searchResult + 1)
                    }

                    image.currentFrameIndex = frameIndex.coerceIn(0, cumulative.lastIndex)
                }
            }
        }
    }
}

private val DefaultPaint = Paint()
