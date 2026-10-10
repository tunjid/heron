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

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.blur.BlurStop
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

internal actual fun verticalProgressiveBlurEffect(
    fractions: FloatArray,
    radiiPx: FloatArray,
    count: Int,
    lineStartY: Float,
    lineEndY: Float,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect? = with(density) {
    BlurRadiusSpec.linearGradient(
        start = DpOffset(
            x = 0.dp,
            y = lineStartY.toDp(),
        ),
        end = DpOffset(
            x = 0.dp,
            y = lineEndY.toDp(),
        ),
        // Allocates whenever the stops change.
        stops = List(count) { index ->
            BlurStop(
                fraction = fractions[index],
                radius = radiiPx[index].toDp(),
            )
        },
    )
}
    .createRenderEffect(
        size = size,
        density = density,
        edgeTreatment = edgeTreatment,
    )
    .takeIf(RenderEffect::isSupported)
