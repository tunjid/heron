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

import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.toSize

/**
 * A [Painter] that draws the latest image available to it.
 *
 * The Compose snapshot system invalidates on the next tick. For
 * image loads that are async, this often results in images not being
 * drawn on the first frame if it backed by snapshot state.
 *
 * To work around this, an [com.tunjid.heron.images.ImagePainter]
 * can be remembered in composition, while the image driving [currentImage] can be updated
 * async. If the image is updated before the first frame deadline, the image will be drawn
 * in the first frame.
 *
 * The [com.tunjid.heron.images.ImagePainter] should used with the [Image] composable.
 * as the scaling and alignment logic used in the Painter depends on the [Image] composable.
 *
 * @param currentImage a means of reading an image that is updated async.
 * @param contentScale the content scale used to render the image.
 * @param alignment the alignment of the image.
 */
internal class ImagePainter(
    val currentImage: () -> Image?,
    val contentScale: () -> ContentScale,
    val alignment: () -> Alignment,
) : Painter() {

    override fun DrawScope.onDraw() {
        when (val image = currentImage()) {
            null -> Unit
            is CoilImage -> {
                scaleAndAlignTo(
                    srcSize = image.size,
                    destSize = size.roundToIntSize(),
                    contentScale = contentScale(),
                    alignment = alignment(),
                    block = {
                        drawIntoCanvas(image.image::renderInto)
                    },
                )
            }
        }
    }

    override val intrinsicSize: Size
        get() = when (val image = currentImage()) {
            is CoilImage -> image.size.toSize()
            null -> Size.Unspecified
        }
}

private inline fun DrawScope.scaleAndAlignTo(
    srcSize: IntSize,
    destSize: IntSize,
    contentScale: ContentScale,
    alignment: Alignment,
    crossinline block: DrawScope.() -> Unit,
) {
    val scaleFactor = contentScale.computeScaleFactor(
        srcSize = srcSize.toSize(),
        dstSize = destSize.toSize(),
    )

    val alignmentOffset = alignment.align(
        size = srcSize,
        space = destSize,
        layoutDirection = layoutDirection,
    )

    val translationOffset = Offset(
        x = alignmentOffset.x * scaleFactor.scaleX,
        y = alignmentOffset.y * scaleFactor.scaleY,
    )

    translate(
        left = translationOffset.x,
        top = translationOffset.y,
        block = {
            scale(
                scaleX = scaleFactor.scaleX,
                scaleY = scaleFactor.scaleY,
                block = block,
            )
        },
    )
}
