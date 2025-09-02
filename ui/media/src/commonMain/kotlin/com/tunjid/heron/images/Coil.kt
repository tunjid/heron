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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.IntSize
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.request.ImageRequest as CoilImageRequest
import coil3.size.Size as CoilSize

@Immutable
internal class CoilImage(
    private val image: coil3.Image,
) : Image {

    override val size: IntSize
        get() = IntSize(
            width = image.width,
            height = image.height,
        )

    override fun drawWith(
        scope: DrawScope,
    ) {
        scope.drawIntoCanvas(image::renderInto)
    }
}

internal class CoilImageLoader(
    private val platformContext: PlatformContext,
) : ImageLoader {

    override suspend fun fetchImage(
        request: ImageRequest,
        size: IntSize,
    ): Image? {
        val coilImageLoader = SingletonImageLoader.get(platformContext)
        val coilRequest = CoilImageRequest.Builder(platformContext).apply {
            when (request) {
                is ImageRequest.Local -> {
                    data(request.file)
                }
                is ImageRequest.Network -> {
                    data(request.url)
                    request.thumbnailUrl
                        ?.let(MemoryCache::Key)
                        ?.let { cacheKey ->
                            placeholder {
                                coilImageLoader
                                    .memoryCache
                                    ?.get(cacheKey)
                                    ?.image
                            }
                        }
                    // TODO: This is only done for network images for now. This is because
                    // Local images need to be loaded as is to obtain the proper dimensions
                    size(
                        CoilSize(
                            width = size.width,
                            height = size.height,
                        ),
                    )
                }
            }
        }
            .build()

        return coilImageLoader.execute(coilRequest)
            .image
            ?.let(::CoilImage)
    }
}

internal expect fun coil3.Image.renderInto(canvas: Canvas)

val LocalImageLoader = staticCompositionLocalOf<ImageLoader> {
    throw IllegalArgumentException("Image Fetcher has not been provided")
}
