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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Draws an animated gradient halo/outline around the composable, following [HaloState.shape]
 * as the path the gradient runs through.
 *
 * @param state the animated halo state; see [HaloState.rememberHaloState].
 */
fun Modifier.halo(
    state: HaloState,
): Modifier = drawWithCache {
    val outlinePath = state.shape.createOutline(
        size = size,
        layoutDirection = layoutDirection,
        density = this,
    ).asPath()

    val center = Offset(
        x = size.width / 2f,
        y = size.height / 2f,
    )
    val brush = Brush.sweepGradient(
        colors = state.colors,
        center = center,
    )
    val strokePx = state.width.toPx()
    val pathMeasure = PathMeasure().apply { setPath(outlinePath, forceClosed = true) }
    val outlineLength = pathMeasure.length
    val cometPath = Path()

    onDrawBehind {
        val alpha = state.alpha
        if (alpha <= 0f) return@onDrawBehind

        when (state.mode) {
            HaloMode.Comet -> {
                if (outlineLength <= 0f) return@onDrawBehind
                val tail = HaloCometFraction * outlineLength
                val head = state.progress * outlineLength
                val start = head - tail
                cometPath.rewind()
                if (start < 0f) {
                    pathMeasure.getSegment(
                        startDistance = outlineLength + start,
                        stopDistance = outlineLength,
                        destination = cometPath,
                        startWithMoveTo = true,
                    )
                    pathMeasure.getSegment(
                        startDistance = 0f,
                        stopDistance = head,
                        destination = cometPath,
                        startWithMoveTo = true,
                    )
                } else {
                    pathMeasure.getSegment(
                        startDistance = start,
                        stopDistance = head,
                        destination = cometPath,
                        startWithMoveTo = true,
                    )
                }
                drawHaloStack(
                    path = cometPath,
                    brush = brush,
                    alpha = alpha,
                    strokePx = strokePx,
                    cap = StrokeCap.Round,
                )
            }

            HaloMode.OutlineSweep -> rotate(
                degrees = state.progress * 360f,
                pivot = center,
            ) {
                drawHaloStack(
                    path = outlinePath,
                    brush = brush,
                    alpha = alpha,
                    strokePx = strokePx,
                    cap = StrokeCap.Butt,
                )
            }
        }
    }
}

enum class HaloMode {
    Comet,
    OutlineSweep,
}

/**
 * Holds a [halo]'s configuration and its animation. Create with [rememberHaloState] and pass to
 * [Modifier.halo]. Dormant until [highlight] is invoked.
 */
@Stable
class HaloState internal constructor(
    internal val shape: Shape,
    internal val colors: List<Color>,
    internal val width: Dp,
    internal val mode: HaloMode,
    private val repeatable: Boolean,
    private val passes: Int,
    private val sweepSpec: AnimationSpec<Float>,
    private val fadeSpec: AnimationSpec<Float>,
    private val scope: CoroutineScope,
) {
    private val progressAnimatable = Animatable(0f)
    private val alphaAnimatable = Animatable(0f)
    private var job: Job? = null

    internal val progress: Float get() = progressAnimatable.value

    internal val alpha: Float get() = alphaAnimatable.value

    /**
     * Fires the halo. In one-shot mode it fades in, runs `passes` sweeps, then fades out; when
     * `repeatable` it loops until [dismiss]. Calling again restarts it from the beginning.
     */
    fun highlight() {
        job?.cancel()
        job = scope.launch {
            alphaAnimatable.animateTo(1f, fadeSpec)
            if (repeatable) {
                while (isActive) {
                    progressAnimatable.snapTo(0f)
                    progressAnimatable.animateTo(1f, sweepSpec)
                }
            } else {
                repeat(passes) {
                    progressAnimatable.snapTo(0f)
                    progressAnimatable.animateTo(1f, sweepSpec)
                }
                alphaAnimatable.animateTo(0f, fadeSpec)
            }
        }
    }

    /**
     * Fades the halo out and stops it, if running.
     */
    fun dismiss() {
        job?.cancel()
        job = scope.launch {
            alphaAnimatable.animateTo(0f, fadeSpec)
        }
    }

    companion object {
        /**
         * Remembers a [HaloState] for [shape]; a change of [shape] (or any configuration) yields a
         * fresh, dormant state.
         *
         * @param repeatable when `true` [highlight] loops until [dismiss]; when `false` it runs
         * [passes] times, then fades out.
         */
        @Composable
        fun rememberHaloState(
            shape: Shape,
            colors: List<Color> = MaterialTheme.colorScheme.let { colorScheme ->
                remember(colorScheme) {
                    listOf(
                        colorScheme.primary,
                        colorScheme.tertiary,
                        colorScheme.secondary,
                        colorScheme.primary,
                    )
                }
            },
            width: Dp = 3.dp,
            mode: HaloMode = HaloMode.Comet,
            repeatable: Boolean = false,
            passes: Int = 2,
            durationMillis: Int = 2000,
        ): HaloState {
            val scope = rememberCoroutineScope()
            return remember(
                shape,
                colors,
                width,
                mode,
                repeatable,
                passes,
                durationMillis,
                scope,
            ) {
                HaloState(
                    shape = shape,
                    colors = colors,
                    width = width,
                    mode = mode,
                    repeatable = repeatable,
                    passes = passes,
                    sweepSpec = tween(durationMillis = durationMillis, easing = LinearEasing),
                    fadeSpec = tween(durationMillis = HaloFadeMillis),
                    scope = scope,
                )
            }
        }
    }
}

private fun DrawScope.drawHaloStack(
    path: Path,
    brush: Brush,
    alpha: Float,
    strokePx: Float,
    cap: StrokeCap,
) {
    drawPath(
        path = path,
        brush = brush,
        alpha = alpha * 0.15f,
        style = Stroke(width = strokePx * 3.5f, cap = cap),
    )
    drawPath(
        path = path,
        brush = brush,
        alpha = alpha * 0.40f,
        style = Stroke(width = strokePx * 2f, cap = cap),
    )
    drawPath(
        path = path,
        brush = brush,
        alpha = alpha,
        style = Stroke(width = strokePx, cap = cap),
    )
}

private fun Outline.asPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}

private const val HaloFadeMillis = 350
private const val HaloCometFraction = 0.22f
