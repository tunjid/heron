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

package com.tunjid.heron.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.tunjid.heron.ui.MorphingLoaderState.Companion.rememberMorphingLoaderState
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A whimsical, indeterminate loader that traces an animated parametric curve while a
 * long-running task is in flight.
 */
@Composable
fun MorphingLoader(
    state: MorphingLoaderState,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val center = Offset(
                x = size.width / 2f,
                y = size.height / 2f,
            )
            val halfExtent = min(size.width, size.height) / 2f

            val brush = Brush.sweepGradient(
                colors = state.colors,
                center = center,
            )
            // Reused across frames so the per-frame draw allocates nothing.
            val curvePath = Path()
            val cometPath = Path()
            val pathMeasure = PathMeasure()

            onDrawBehind {
                // strokeWidth animates, so it is read here (draw phase) rather than in the cache block.
                val strokePx = state.strokeWidth.toPx()
                val baseRadius = halfExtent - strokePx * 2f
                if (baseRadius <= 0f) return@onDrawBehind
                val energyValue = state.energy.coerceIn(0f, 1f)

                // Energy sets the character of the figure.
                val petals = lerp(MinPetals, MaxPetals, energyValue).roundToInt()
                val nextPetals = petals + 1
                val baseDepth = lerp(0.20f, 0.44f, energyValue)
                val ghostAlpha = lerp(0.30f, 0.50f, energyValue)
                val breathAmplitude = lerp(0.04f, 0.10f, energyValue)

                // The clocks keep the shape itself moving, independent of energy.
                val morphAngle = state.morph * TwoPi
                // Blend the petal count between P and P+1, and pulse how deep the petals cut.
                val petalBlend = 0.5f + 0.5f * sin(morphAngle)
                val depth = baseDepth * (0.55f + 0.45f * sin(morphAngle * 2f))

                val spinPhase = state.spin * TwoPi
                val breathScale = 1f + breathAmplitude * sin(spinPhase)
                val radius = baseRadius * breathScale

                curvePath.rewind()
                for (step in 0..CurveSteps) {
                    val t = (step.toFloat() / CurveSteps) * TwoPi
                    val point = loaderPoint(
                        curve = state.curve,
                        t = t,
                        phase = spinPhase,
                        petals = petals,
                        nextPetals = nextPetals,
                        petalBlend = petalBlend,
                        depth = depth,
                    )
                    val x = center.x + point.x * radius
                    val y = center.y + point.y * radius
                    if (step == 0) curvePath.moveTo(x, y)
                    else curvePath.lineTo(x, y)
                }
                curvePath.close()

                // The full figure, drawn faintly through the sweep gradient; fades out when the trace
                // is switched off, leaving only the comet.
                val traceAlpha = ghostAlpha * state.traceAlpha
                if (traceAlpha > 0f) {
                    drawPath(
                        path = curvePath,
                        brush = brush,
                        alpha = traceAlpha,
                        style = Stroke(width = strokePx),
                    )
                }

                // A bright comet head that runs along the figure to signal progress.
                pathMeasure.setPath(curvePath, forceClosed = true)
                val length = pathMeasure.length
                if (length > 0f) {
                    val highlightColor = state.highlightColor
                    val tail = state.cometLength.coerceIn(0f, 1f) * length
                    val head = state.comet * length
                    val start = head - tail
                    cometPath.rewind()
                    if (start < 0f) {
                        pathMeasure.getSegment(
                            startDistance = length + start,
                            stopDistance = length,
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
                    drawPath(
                        path = cometPath,
                        color = highlightColor,
                        alpha = 0.14f,
                        style = Stroke(width = strokePx * 4f, cap = StrokeCap.Round),
                    )
                    drawPath(
                        path = cometPath,
                        color = highlightColor,
                        alpha = 0.30f,
                        style = Stroke(width = strokePx * 2.2f, cap = StrokeCap.Round),
                    )
                    drawPath(
                        path = cometPath,
                        color = highlightColor,
                        style = Stroke(width = strokePx * 1.1f, cap = StrokeCap.Round),
                    )
                }
            }
        },
    )
}

/**
 * The parametric family a [MorphingLoader] traces.
 */
enum class LoaderCurve {
    Rose,
    Lissajous,
}

/**
 * Holds a [MorphingLoader]'s configuration and animation. Create with [rememberMorphingLoaderState]
 * and pass to one or more [MorphingLoader]s; sharing a single state animates them in lockstep.
 */
@Stable
class MorphingLoaderState internal constructor(
    internal val curve: LoaderCurve,
    internal val colors: List<Color>,
    private val energyState: State<Float>,
    private val cometLengthState: State<Float>,
    private val highlightColorState: State<Color>,
    private val strokeWidthState: State<Dp>,
    private val traceAlphaState: State<Float>,
    private val spinState: State<Float>,
    private val morphState: State<Float>,
    private val cometState: State<Float>,
) {
    /**
     *  How lively the figure is, `0f`..`1f`.
     */
    internal val energy: Float get() = energyState.value

    /**
     * The comet's length as a fraction of the outline perimeter, `0f`..`1f`.
     */
    internal val cometLength: Float get() = cometLengthState.value

    /**
     * The comet's color.
     */
    internal val highlightColor: Color get() = highlightColorState.value

    /** The stroke width of the figure and comet.
     */
    internal val strokeWidth: Dp get() = strokeWidthState.value

    /**
     * Opacity multiplier for the full figure; `0f` hides everything but the comet.
     */
    internal val traceAlpha: Float get() = traceAlphaState.value

    internal val spin: Float get() = spinState.value
    internal val morph: Float get() = morphState.value
    internal val comet: Float get() = cometState.value

    companion object {
        /**
         * Remembers a [MorphingLoaderState]. Continuous inputs animate to their targets, so changing
         * them is smooth rather than jerky:
         * - [energy] eases with [energyAnimationSpec],
         * - [cometLength], [highlightColor], [strokeWidth] and [traceVisible] share a
         *   [configEaseMillis] tween.
         *
         * The clocks run on different periods so the figure never quite repeats:
         * - [spinDurationMillis] rotates the whole figure and drives its breathing,
         * - [morphDurationMillis] blends the petal count and pulses the petal depth,
         * - [cometDurationMillis] runs the comet once around the outline.
         *
         * With [traceVisible] `false` only the comet is drawn, so its path can't be anticipated.
         * [curve] and [colors] are structural: changing either rebuilds the state.
         */
        @Composable
        fun rememberMorphingLoaderState(
            energy: Float = 1f,
            cometLength: Float = DefaultCometLength,
            curve: LoaderCurve = LoaderCurve.Rose,
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
            highlightColor: Color = MaterialTheme.colorScheme.primary,
            strokeWidth: Dp = 2.dp,
            traceVisible: Boolean = true,
            energyAnimationSpec: AnimationSpec<Float> = tween(
                durationMillis = DefaultEnergyEaseMillis,
            ),
            configEaseMillis: Int = DefaultConfigEaseMillis,
            spinDurationMillis: Int = 9000,
            morphDurationMillis: Int = 7200,
            cometDurationMillis: Int = 6000,
        ): MorphingLoaderState {
            // Continuous config: animated where the values live so state need not be rebuilt to change
            // them. cometLength/highlightColor/strokeWidth share one tween; energy keeps its own spec.
            val energyState = animateFloatAsState(
                targetValue = energy,
                animationSpec = energyAnimationSpec,
                label = "MorphingLoaderEnergy",
            )
            val cometLengthState = animateFloatAsState(
                targetValue = cometLength,
                animationSpec = tween(durationMillis = configEaseMillis),
                label = "MorphingLoaderCometLength",
            )
            val highlightColorState = animateColorAsState(
                targetValue = highlightColor,
                animationSpec = tween(durationMillis = configEaseMillis),
                label = "MorphingLoaderHighlight",
            )
            val strokeWidthState = animateDpAsState(
                targetValue = strokeWidth,
                animationSpec = tween(durationMillis = configEaseMillis),
                label = "MorphingLoaderStroke",
            )
            val traceAlphaState = animateFloatAsState(
                targetValue = if (traceVisible) 1f else 0f,
                animationSpec = tween(durationMillis = configEaseMillis),
                label = "MorphingLoaderTrace",
            )

            val transition = rememberInfiniteTransition(
                label = "morphingLoader",
            )
            val spinState = transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = spinDurationMillis,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "MorphingLoaderSpin",
            )
            val morphState = transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = morphDurationMillis,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "MorphingLoaderMorph",
            )
            val cometState = transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = cometDurationMillis,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "MorphingLoaderComet",
            )
            return remember(
                curve,
                colors,
                energyState,
                cometLengthState,
                highlightColorState,
                strokeWidthState,
                traceAlphaState,
                spinState,
                morphState,
                cometState,
            ) {
                MorphingLoaderState(
                    curve = curve,
                    colors = colors,
                    energyState = energyState,
                    cometLengthState = cometLengthState,
                    highlightColorState = highlightColorState,
                    strokeWidthState = strokeWidthState,
                    traceAlphaState = traceAlphaState,
                    spinState = spinState,
                    morphState = morphState,
                    cometState = cometState,
                )
            }
        }
    }
}

/**
 * Unit-space point (roughly within `[-1, 1]`) of [curve] at parameter [t], rotated by [phase]. For
 * [LoaderCurve.Rose] the outline blends between a [petals]- and a [nextPetals]-petal flower by
 * [petalBlend], with petals cut [depth] deep.
 */
private fun loaderPoint(
    curve: LoaderCurve,
    t: Float,
    phase: Float,
    petals: Int,
    nextPetals: Int,
    petalBlend: Float,
    depth: Float,
): Offset = when (curve) {
    LoaderCurve.Rose -> {
        // A rose-modulated circle: always a smooth closed loop that never collapses to the origin.
        val angle = t + phase
        val modulation =
            (1f - petalBlend) * cos(petals * angle) + petalBlend * cos(nextPetals * angle)
        val normalizedRadius = (1f + depth * modulation) / (1f + depth)
        Offset(
            x = normalizedRadius * cos(angle),
            y = normalizedRadius * sin(angle),
        )
    }

    LoaderCurve.Lissajous -> Offset(
        x = sin(petals * t + phase),
        y = sin(nextPetals * t),
    )
}

private const val TwoPi = 6.2831855f
private const val CurveSteps = 220
private const val MinPetals = 3f
private const val MaxPetals = 6f
private const val DefaultEnergyEaseMillis = 550
private const val DefaultConfigEaseMillis = 400
private const val DefaultCometLength = 0.22f
