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
import androidx.compose.foundation.shape.CornerRounding
import androidx.compose.foundation.shape.MorphPolygonShape
import androidx.compose.foundation.shape.PolygonShape
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
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/*
 * All PolygonShape construction for the app lives in this file, so foundation API changes only
 * need to be absorbed here.
 *
 * Shapes returned from these helpers should be held in top level or lazy vals, never created
 * in composition. PolygonShape equality is referential, and [animate] restarts whenever it sees a
 * shape instance other than the previous one.
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
): PolygonShape {
    val perVertexRounding = listOf(
        bottomEndPercent,
        bottomStartPercent,
        topStartPercent,
        topEndPercent,
    ).map { radius ->
        roundingFraction(
            radius = radius,
            reference = 2f,
        )
    }
    return PolygonShape {
        polygon(
            vertices = UnitSquareVertices,
            perVertexRounding = perVertexRounding,
            center = Offset.Zero,
        )
    }.stretched()
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
): PolygonShape {
    // A regular polygon's fraction resolves against its radius, which is 1 here.
    val perVertexRounding = cornerSizePercentAtIndex.map { radius ->
        roundingFraction(
            radius = radius,
            reference = 1f,
        )
    }
    return PolygonShape {
        polygon(
            numVertices = perVertexRounding.size,
            perVertexRounding = perVertexRounding,
            radius = 1f,
            center = Offset.Zero,
        )
    }.stretched(rotation = rotation)
}

class PebbleVertex(
    val angle: Float,
    val radius: Float,
    val cornerRadius: Float,
)

/**
 * An organic, stone like shape stretched to fill its layout bounds.
 */
@RememberInComposition
fun PolygonShape.Companion.pebble(
    vertices: List<PebbleVertex>,
    smoothing: Float = 1f,
    widthRatio: Float = 1f,
): PolygonShape {
    val offsets = vertices.map { vertex ->
        val angle = vertex.angle / 180f * PI.toFloat()
        Offset(
            x = cos(angle) * vertex.radius * widthRatio,
            y = sin(angle) * vertex.radius,
        )
    }
    // A vertex list's fraction resolves against the smaller dimension of its bounds.
    val reference = min(
        offsets.maxOf(Offset::x) - offsets.minOf(Offset::x),
        offsets.maxOf(Offset::y) - offsets.minOf(Offset::y),
    )
    val perVertexRounding = vertices.map { vertex ->
        roundingFraction(
            radius = vertex.cornerRadius,
            reference = reference,
            smoothing = smoothing,
        )
    }
    return PolygonShape {
        polygon(
            vertices = offsets,
            perVertexRounding = perVertexRounding,
            center = Offset.Zero,
        )
    }.stretched()
}

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
            // Copies give this morph its own outline caches. The shared instances are resolved at
            // every size they are used at, and would otherwise invalidate the Morph each frame.
            delegate = MorphPolygonShape(
                start = previousShape.copy(),
                end = currentShape.copy(),
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
 * its layout bounds, scaling independently on each axis.
 *
 * [PolygonShape.transform] replaces any previous transform, and mutates the shape in place, so
 * transform bust be applied once, and only to a newly created shape.
 */
private fun PolygonShape.stretched(
    rotation: Float = 0f,
): PolygonShape = apply {
    transform {
        if (rotation != 0f) rotate(degrees = rotation)
        scaleToFit(contentScale = ContentScale.FillBounds)
    }
}

/**
 * A [CornerRounding.fraction] of [radius] relative to [reference], clamped to the range it allows.
 */
private fun roundingFraction(
    radius: Float,
    reference: Float,
    smoothing: Float = 0f,
): CornerRounding = CornerRounding.fraction(
    fraction = (radius / reference).coerceIn(0f, 1f),
    smoothing = smoothing,
)

// A 2x2 square centered on the origin, ordered bottom end, bottom start, top start, top end.
private val UnitSquareVertices = listOf(
    Offset(x = 1f, y = 1f),
    Offset(x = -1f, y = 1f),
    Offset(x = -1f, y = -1f),
    Offset(x = 1f, y = -1f),
)

private val StretchedCircle = PolygonShape.circle().stretched()

private val PlainRectangle = PolygonShape.rectangle()
