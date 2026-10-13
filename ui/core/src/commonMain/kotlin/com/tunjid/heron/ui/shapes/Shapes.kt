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

package com.tunjid.heron.ui.shapes

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.MorphPolygonShape
import androidx.compose.foundation.shape.PolygonShape
import androidx.compose.foundation.shape.PolygonShapeGeometry
import androidx.compose.foundation.shape.PolygonShapeGeometry.Companion.CornerRounding
import androidx.compose.foundation.shape.scaledToFit
import androidx.compose.foundation.shape.transformed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * All PolygonShape construction for the app lives in this file, so foundation API changes only
 * need to be absorbed here.
 *
 * Shapes returned from these helpers should be held in top level or lazy vals, never created
 * in composition. PolygonShape equality is going to become referential as of
 * this [CL](https://android-review.googlesource.com/c/platform/frameworks/support/+/4310336)
 */

val PolygonShape.Companion.Circle: PolygonShape
    get() = StretchedCircle

val PolygonShape.Companion.Rectangle: PolygonShape
    get() = PlainRectangle

@RememberInComposition
fun PolygonShape.Companion.roundedRectangle(
    percent: Float,
): PolygonShape = roundedRectangle(
    bottomEndPercent = percent,
    bottomStartPercent = percent,
    topStartPercent = percent,
    topEndPercent = percent,
)

@RememberInComposition
fun PolygonShape.Companion.roundedRectangle(
    bottomEndPercent: Float,
    bottomStartPercent: Float,
    topStartPercent: Float,
    topEndPercent: Float,
): PolygonShape = PolygonShape(
    geometry = PolygonShapeGeometry(
        // A 2x2 square centered on the origin, ordered bottom end, bottom start, top start, top end.
        vertices = listOf(
            Offset(x = 1f, y = 1f),
            Offset(x = -1f, y = 1f),
            Offset(x = -1f, y = -1f),
            Offset(x = 1f, y = -1f),
        ),
        perVertexRounding = listOf(
            CornerRounding(radius = bottomEndPercent),
            CornerRounding(radius = bottomStartPercent),
            CornerRounding(radius = topStartPercent),
            CornerRounding(radius = topEndPercent),
        ),
        center = Offset.Zero,
    ),
).stretched()

/**
 * A star with [numPoints] points stretched to fill its layout bounds, rounded at each vertex with
 * [roundingFraction] of its radius.
 *
 * @param rotation clockwise rotation in degrees applied before stretching.
 */
@RememberInComposition
fun PolygonShape.Companion.roundedStar(
    numPoints: Int,
    innerRadiusRatio: Float,
    roundingFraction: Float,
    rotation: Float = 0f,
): PolygonShape = star(
    numPoints = numPoints,
    innerRadiusRatio = innerRadiusRatio,
    outerRounding = CornerRounding(percent = (roundingFraction * 100).roundToInt()),
).stretched(rotation = rotation)

/**
 * The material3 pill, stretched to fill its layout bounds. material3 only exposes it as a
 * RoundedPolygon, so its points are ported here: the points of one section are repeated around
 * the center, mirroring every other section.
 */
@RememberInComposition
fun PolygonShape.Companion.materialPill(): PolygonShape {
    val center = Offset(
        x = 0.5f,
        y = 0.5f,
    )
    val points = listOf(
        Offset(x = 0.961f, y = 0.039f) to 0.426f,
        Offset(x = 1.001f, y = 0.428f) to 0f,
        Offset(x = 1.000f, y = 0.609f) to 1f,
    )
    val angles = points.map { (point) ->
        (point - center).let { atan2(it.y, it.x) }
    }
    val sections = 4
    val sectionAngle = 2 * PI.toFloat() / sections
    val repeated = buildList {
        repeat(sections) { section ->
            val mirrored = section % 2 != 0
            points.indices.forEach { index ->
                val i = if (mirrored) points.lastIndex - index else index
                if (i > 0 || !mirrored) {
                    val angle = sectionAngle * section +
                        if (mirrored) sectionAngle - angles[i] + 2 * angles[0]
                        else angles[i]
                    val (point, rounding) = points[i]
                    val distance = (point - center).getDistance()
                    add(
                        Offset(
                            x = cos(angle),
                            y = sin(angle),
                        ) * distance + center to rounding,
                    )
                }
            }
        }
    }
    return PolygonShape(
        geometry = PolygonShapeGeometry(
            vertices = repeated.map { it.first },
            perVertexRounding = repeated.map { CornerRounding(radius = it.second) },
            center = center,
        ),
    ).stretched()
}

/**
 * A regular polygon stretched to fill its layout bounds, with a vertex for each item in
 * [cornerSizePercentAtIndex], rounded with that percent of its radius.
 *
 * @param rotation clockwise rotation in degrees of the first vertex from the positive x axis.
 */
@RememberInComposition
fun PolygonShape.Companion.roundedPolygon(
    cornerSizePercentAtIndex: List<Float>,
    rotation: Float = 0f,
): PolygonShape = PolygonShape(
    geometry = PolygonShapeGeometry(
        vertices = cornerSizePercentAtIndex.indices.map { index ->
            val angle = (rotation / 360f + index.toFloat() / cornerSizePercentAtIndex.size) *
                2 * PI.toFloat()
            Offset(
                x = cos(angle),
                y = sin(angle),
            )
        },
        perVertexRounding = cornerSizePercentAtIndex.map { radius ->
            CornerRounding(radius = radius)
        },
        center = Offset.Zero,
    ),
).stretched()

@Composable
fun PolygonShape.animate(
    animationSpec: FiniteAnimationSpec<Float> = spring(),
): Shape {
    val updatedAnimationSpec by rememberUpdatedState(animationSpec)
    val progress = remember {
        mutableFloatStateOf(1f)
    }
    var previousShape by remember {
        mutableStateOf(this)
    }

    val currentShape = remember {
        mutableStateOf(this)
    }.apply {
        if (value != this@animate) {
            // TODO capture morphs that have not completed
            previousShape = value
            // Reset the progress
            progress.floatValue = 0f
        }
        value = this@animate
    }.value

    LaunchedEffect(currentShape) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = updatedAnimationSpec,
            block = { value, _ ->
                progress.floatValue = value
            },
        )
    }

    return remember(
        previousShape,
        currentShape,
    ) {
        EmptySizeSafeShape(
            delegate = MorphPolygonShape(
                start = previousShape,
                end = currentShape,
                progress = progress::floatValue,
            ),
        )
    }
}

private class EmptySizeSafeShape(
    private val delegate: Shape,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline =
        if (size.isEmpty()) RectangleShape.createOutline(
            size = size,
            layoutDirection = layoutDirection,
            density = density,
        )
        else delegate.createOutline(
            size = size,
            layoutDirection = layoutDirection,
            density = density,
        )
}

/**
 * Rotates this shape clockwise by [rotation] degrees, then stretches its resolved geometry to fill
 * its layout bounds, scaling independently on each axis. A circle in a non square container
 * becomes an ellipse.
 *
 * Every shape goes through this exactly once, so all transforms on a shape are applied in a
 * single step.
 */
private fun PolygonShape.stretched(
    rotation: Float = 0f,
): PolygonShape =
    if (rotation == 0f) scaledToFit(contentScale = ContentScale.FillBounds)
    else transformed(rotation = rotation).scaledToFit(contentScale = ContentScale.FillBounds)

private val StretchedCircle = PolygonShape.circle().stretched()

private val PlainRectangle = PolygonShape.rectangle()
