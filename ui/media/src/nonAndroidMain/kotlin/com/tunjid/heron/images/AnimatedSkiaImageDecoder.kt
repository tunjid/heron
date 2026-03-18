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

import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlin.experimental.and
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8
import okio.use
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data

/**
 * A [Decoder] that uses Skia to decode animated images (GIF, WebP).
 *
 * @param bufferedFramesCount The number of frames to be pre-buffered before the animation
 * starts playing.
 */
class AnimatedSkiaImageDecoder(
    private val source: ImageSource,
    private val bufferedFramesCount: Int,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val bytes = source.source().use { it.readByteArray() }
        val codec = Codec.makeFromData(Data.makeFromBytes(bytes))
        return DecodeResult(
            image = AnimatedSkiaImage(
                codec = codec,
                bufferedFramesCount = bufferedFramesCount,
            ),
            isSampled = false,
        )
    }

    class Factory(
        private val bufferedFramesCount: Int = DEFAULT_BUFFERED_FRAMES_COUNT,
    ) : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result.source.source())) return null
            return AnimatedSkiaImageDecoder(
                source = result.source,
                bufferedFramesCount = bufferedFramesCount,
            )
        }

        private fun isApplicable(source: BufferedSource): Boolean {
            return isGif(source) || isAnimatedWebP(source)
        }

        private fun isGif(source: BufferedSource): Boolean {
            return source.rangeEquals(0, GIF_HEADER_89A) ||
                source.rangeEquals(0, GIF_HEADER_87A)
        }

        private fun isAnimatedWebP(source: BufferedSource): Boolean {
            return source.rangeEquals(0, WEBP_HEADER_RIFF) &&
                source.rangeEquals(8, WEBP_HEADER_WEBP) &&
                source.rangeEquals(12, WEBP_HEADER_VPX8) &&
                source.request(21) &&
                (source.buffer[20] and 0b00000010) > 0
        }
    }
}

private val GIF_HEADER_87A = "GIF87a".encodeUtf8()
private val GIF_HEADER_89A = "GIF89a".encodeUtf8()
private val WEBP_HEADER_RIFF = "RIFF".encodeUtf8()
private val WEBP_HEADER_WEBP = "WEBP".encodeUtf8()
private val WEBP_HEADER_VPX8 = "VP8X".encodeUtf8()
private const val DEFAULT_BUFFERED_FRAMES_COUNT = 5
