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

package com.tunjid.heron.ui.modifiers

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp

/**
 * A blur [Modifier] with lambda arguments to prevent relayouts and recomposition.
 * Lambda arguments that update should be backed with compose state
 */
fun Modifier.blur(
    shape: Shape,
    clip: () -> Boolean = { false },
    radius: () -> Dp,
    progress: () -> Float,
): Modifier = graphicsLayer {
    blurEffect(
        radius = radius,
        progress = progress,
        shape = shape,
        clip = clip,
    )
    return@graphicsLayer
}

fun GraphicsLayerScope.blurEffect(
    radius: () -> Dp,
    progress: () -> Float,
    shape: Shape,
    clip: () -> Boolean,
) {
    val currentRadius = radius()
    val currentProgress = progress()
    if (currentProgress <= 0f) return

    val horizontalBlurPixels = currentRadius.toPx() * currentProgress
    val verticalBlurPixels = currentRadius.toPx() * currentProgress

    // Only non-zero blur radii are valid BlurEffect parameters
    if (horizontalBlurPixels <= 0f || verticalBlurPixels <= 0f) return

    this.renderEffect = BlurEffect(
        radiusX = horizontalBlurPixels,
        radiusY = verticalBlurPixels,
        edgeTreatment = TileMode.Clamp,
    )

    this.shape = shape
    this.clip = clip()
}

fun Modifier.blockClickEvents() =
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial)
            down.consume()
            waitForUpOrCancellation(PointerEventPass.Initial)?.consume()
        }
    }

inline fun Modifier.ifTrue(
    predicate: Boolean,
    block: Modifier.() -> Modifier,
) = if (predicate) block() else this
