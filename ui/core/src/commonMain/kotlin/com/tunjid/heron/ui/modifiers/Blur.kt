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

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

fun Modifier.blur(
    shape: Shape,
    radius: () -> Dp,
    progress: () -> Float,
): Modifier = graphicsLayer {
    val currentRadius = radius()
    val currentProgress = progress()
    if (currentProgress <= 0f) return@graphicsLayer

    val horizontalBlurPixels = currentRadius.toPx() * currentProgress
    val verticalBlurPixels = currentRadius.toPx() * currentProgress

    // Only non-zero blur radii are valid BlurEffect parameters
    if (horizontalBlurPixels <= 0f || verticalBlurPixels <= 0f) return@graphicsLayer

    this.renderEffect = BlurEffect(
        radiusX = horizontalBlurPixels,
        radiusY = verticalBlurPixels,
        edgeTreatment = TileMode.Clamp,
    )

    this.shape = shape
    this.clip = true
}
