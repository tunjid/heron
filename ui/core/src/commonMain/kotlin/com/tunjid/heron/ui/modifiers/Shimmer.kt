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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate

fun Modifier.shimmer(
    state: ShimmerState,
): Modifier = drawWithCache {
    val brush = Brush.linearGradient(
        colors = state.colors,
        start = Offset.Zero,
        end = Offset(size.width, size.height),
        tileMode = TileMode.Clamp,
    )

    val travelDistance = size.width * 2

    onDrawBehind {
        val currentOffset = (travelDistance * state.progress) - size.width
        translate(left = currentOffset) {
            drawRect(
                brush = brush,
                topLeft = Offset(
                    x = -currentOffset,
                    y = 0f,
                ),
                size = size,
            )
        }
    }
}

@Immutable
class ShimmerState internal constructor(
    internal val colors: List<Color>,
    private val progressState: State<Float>,
) {
    internal val progress: Float
        get() = progressState.value

    companion object {
        @Composable
        fun rememberShimmerState(
            colors: List<Color> = MaterialTheme.colorScheme.let { colorScheme ->
                remember(colorScheme) {
                    listOf(
                        colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                        colorScheme.surfaceContainerHighest.copy(alpha = 0.2f),
                        colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    )
                }
            },
            durationMillis: Int = DefaultShimmerDurationMillis,
        ): ShimmerState {
            val transition = rememberInfiniteTransition(label = "shimmer")
            val progressState = transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = durationMillis,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "ShimmerProgress",
            )

            return remember(colors, progressState) {
                ShimmerState(
                    colors = colors,
                    progressState = progressState,
                )
            }
        }
    }
}

private const val DefaultShimmerDurationMillis = 1000
