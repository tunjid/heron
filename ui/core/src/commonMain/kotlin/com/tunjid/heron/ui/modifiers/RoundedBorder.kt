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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

fun Modifier.roundedBorder(
    isStroked: Boolean,
    borderColor: () -> Color,
    cornerRadius: () -> Dp,
    strokeWidth: () -> Dp,
) = drawWithCache {
    val style = Stroke(
        width = strokeWidth().toPx(),
        pathEffect =
        if (isStroked) PathEffect.dashPathEffect(
            intervals = floatArrayOf(10f, 10f), // Dash length and gap length
            phase = 0f, // Optional: offset for the dash pattern
        )
        else null,
    )
    onDrawBehind {
        val radius = cornerRadius()
        drawRoundRect(
            cornerRadius = CornerRadius(
                x = radius.toPx(),
                y = radius.toPx(),
            ),
            color = borderColor(),
            style = style,
        )
    }
}
