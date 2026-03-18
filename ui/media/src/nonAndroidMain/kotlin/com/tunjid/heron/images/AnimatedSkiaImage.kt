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

import androidx.collection.MutableIntList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import coil3.Canvas
import coil3.Image
import org.jetbrains.skia.AnimationFrameInfo
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec

internal class AnimatedSkiaImage(
    internal val codec: Codec,
    bufferedFramesCount: Int,
) : Image {

    override val size: Long
        get() {
            var size = codec.imageInfo.computeMinByteSize().toLong()
            if (size <= 0L) {
                size = 4L * codec.width * codec.height
            }
            return size.coerceAtLeast(0)
        }

    override val width: Int
        get() = codec.width

    override val height: Int
        get() = codec.height

    override val shareable: Boolean
        get() = false

    internal val frameDurationsMs: List<Int> by lazy {
        codec.framesInfo.map { it.safeFrameDuration }
    }

    internal val singleIterationDurationMs: Int by lazy {
        frameDurationsMs.sum()
    }

    internal val cumulativeFrameDurationsMs by lazy {
        MutableIntList(frameDurationsMs.size).apply {
            var sum = 0
            for (duration in frameDurationsMs) {
                sum += duration
                add(sum)
            }
        }
    }

    internal val frameCount: Int
        get() = codec.frameCount

    private val tempBitmap = Bitmap().apply { allocPixels(codec.imageInfo) }

    internal val frames = Array(codec.frameCount) { index ->
        if (index in 0..<bufferedFramesCount.coerceAtMost(codec.frameCount)) decodeFrame(index)
        else null
    }

    internal var currentFrameIndex by mutableIntStateOf(0)

    override fun draw(canvas: Canvas) {
        if (frameCount == 0) return

        val frameIndex = currentFrameIndex.coerceIn(0, frames.size - 1)
        frames[frameIndex]?.let { image ->
            canvas.drawImage(image, 0f, 0f)
        }
    }

    internal fun decodeFrame(frameIndex: Int): org.jetbrains.skia.Image {
        check(!tempBitmap.isClosed) { "Cannot decode frame: the bitmap is closed." }
        codec.readPixels(tempBitmap, frameIndex)
        return org.jetbrains.skia.Image.makeFromBitmap(
            tempBitmap,
        )
    }

    internal fun closeTempBitmap() {
        if (!tempBitmap.isClosed) tempBitmap.close()
    }
}

private val AnimationFrameInfo.safeFrameDuration: Int
    get() = duration.let { if (it <= 0) DEFAULT_FRAME_DURATION else it }

private const val DEFAULT_FRAME_DURATION = 100
