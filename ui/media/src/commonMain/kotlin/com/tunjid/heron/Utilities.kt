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

package com.tunjid.heron

import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize

internal inline fun DrawScope.scaleAndAlignTo(
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
