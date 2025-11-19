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

package com.tunjid.heron.timeline.utilities

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tunjid.heron.ui.modifiers.blur

internal fun Modifier.sensitiveContentBlur(
    shape: Shape,
) = drawWithCache {
    val density = Density(density)
    val color = Color.Black.copy(alpha = 0.5f)
    onDrawWithContent {
        drawContent()
        drawOutline(
            outline = shape.createOutline(
                size = size,
                layoutDirection = layoutDirection,
                density = density,
            ),
            color = color,
        )
    }
}
    .blur(
        shape = shape,
        radius = ::SensitiveContentBlurRadius,
        clip = ::SensitiveContentBlurClip,
        progress = { 1f },
    )

private val SensitiveContentBlurRadius = 120.dp
private const val SensitiveContentBlurClip = true
