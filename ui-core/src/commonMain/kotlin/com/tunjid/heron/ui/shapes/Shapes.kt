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
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import kotlin.math.abs
import kotlin.math.max

@Stable
sealed class RoundedPolygonShape : Shape {

    private var path = Path()
    private var matrix: Matrix = Matrix()
    private var lastSize = Size.Unspecified
    private var lastDensity = Density(1f)
    internal var lastBounds: Rect = Rect.Zero
    private var lastPolygon: RoundedPolygon? = null

    private val bounds = FloatArray(4)

    internal abstract fun createPolygon(
        size: Size,
        density: Density,
    ): RoundedPolygon

    internal fun ensurePolygon(
        size: Size,
        density: Density,
    ): RoundedPolygon = lastPolygon
        ?.takeIf {
            size.canReuse(lastSize) && lastDensity == density
        }
        ?: createPolygon(size, density).also { polygon ->
            lastPolygon = polygon
            lastSize = size
            lastDensity = density
            lastBounds = polygon.calculateBounds(
                bounds = bounds,
                approximate = this !is Polygon,
            ).let { Rect(it[0], it[1], it[2], it[3]) }
        }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val polygon = ensurePolygon(size, density)

        path.rewind()
        path = polygon.toPath(path)
        matrix.reset()

        matrix.scale(
            x = size.width / lastBounds.width,
            y = size.height / lastBounds.height,
        )

        matrix.translate(
            x = -lastBounds.left,
            y = -lastBounds.top,
        )
        path.transform(matrix)

        return Outline.Generic(path)
    }

    @Stable
    data object Circle : RoundedPolygonShape() {
        private val polygon = RoundedPolygon.circle(numVertices = 8)
        override fun createPolygon(
            size: Size,
            density: Density,
        ): RoundedPolygon = polygon
    }

    @Stable
    data object Rectangle : RoundedPolygonShape() {
        private val polygon = RoundedPolygon.rectangle()
        override fun createPolygon(
            size: Size,
            density: Density,
        ): RoundedPolygon = polygon
    }

    @Stable
    data class RoundedRectangle(
        val roundedCornerShape: RoundedCornerShape,
    ) : RoundedPolygonShape() {
        override fun createPolygon(
            size: Size,
            density: Density,
        ): RoundedPolygon = RoundedPolygon.rectangle(
            perVertexRounding = listOf(
                roundedCornerShape.bottomEnd.normalize(
                    size = size,
                    density = density,
                ),
                roundedCornerShape.bottomStart.normalize(
                    size = size,
                    density = density,
                ),
                roundedCornerShape.topStart.normalize(
                    size = size,
                    density = density,
                ),
                roundedCornerShape.topEnd.normalize(
                    size = size,
                    density = density,
                ),
            )
        )

        private fun CornerSize.normalize(
            size: Size,
            density: Density,
        ): CornerRounding {
            val maxDimension = max(size.width, size.height)
            val absoluteCornerSize = toPx(
                shapeSize = size,
                density = density,
            )
            val radius = (absoluteCornerSize / maxDimension) * 2f

            return CornerRounding(radius = radius)
        }
    }

    @Stable
    data class Polygon(
        val cornerSizeAtIndex: List<Dp>,
    ) : RoundedPolygonShape() {

        override fun createPolygon(
            size: Size,
            density: Density,
        ): RoundedPolygon = RoundedPolygon(
            numVertices = cornerSizeAtIndex.size,
            perVertexRounding = cornerSizeAtIndex.map {
                val maxDimension = max(size.width, size.height)
                val absoluteCornerSize = with(density) { it.toPx() }
                val radius = (absoluteCornerSize / maxDimension) * 2f
                CornerRounding(radius = radius)
            },
        )
    }

    @Stable
    data class Star(
        val cornerSizeAtIndex: List<Dp>,
        val roundingRadius: Float,
    ) : RoundedPolygonShape() {

        override fun createPolygon(
            size: Size,
            density: Density,
        ): RoundedPolygon = RoundedPolygon.star(
            numVerticesPerRadius = cornerSizeAtIndex.size / 2,
            rounding = CornerRounding(roundingRadius),
            perVertexRounding = cornerSizeAtIndex.map {
                val maxDimension = max(size.width, size.height)
                val absoluteCornerSize = with(density) { it.toPx() }
                val radius = (absoluteCornerSize / maxDimension) * 2f
                CornerRounding(radius = radius)
            },
        )
    }
}

fun RoundedCornerShape.toRoundedPolygonShape() = RoundedPolygonShape.RoundedRectangle(
    roundedCornerShape = this
)

@Composable
fun RoundedPolygonShape.animate(
    animationSpec: FiniteAnimationSpec<Float> = spring(),
): Shape {
    val updatedAnimationSpec by rememberUpdatedState(animationSpec)
    var interpolation by remember {
        mutableFloatStateOf(1f)
    }
    var previousShape by remember {
        mutableStateOf(this)
    }

    val currentShape by remember {
        mutableStateOf(this)
    }.apply {
        if (value != this@animate) {
            // TODO capture morphs that have not completed
            previousShape = value
            // Reset the interpolation
            interpolation = 0f
        }
        // Set the current value, this will also stop any call to lerp above from recomposing
        value = this@animate
    }

    val morphingShape = remember {
        MorphingShape(
            previousShape = previousShape,
            currentShape = currentShape,
            interpolation = interpolation
        )
    }

    LaunchedEffect(Unit) {
        snapshotFlow { currentShape }.collect {
            androidx.compose.animation.core.animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = updatedAnimationSpec,
                block = { progress, _ ->
                    interpolation = progress
                    morphingShape.previousShape = previousShape
                    morphingShape.currentShape = currentShape
                    morphingShape.interpolation = progress
                },
            )
        }
    }

    return morphingShape
}

@Stable
private class MorphingShape(
    var previousShape: RoundedPolygonShape,
    var currentShape: RoundedPolygonShape,
    interpolation: Float,
) : Shape {
    private val path = Path()
    private val matrix = Matrix()

    private var previousPolygon: RoundedPolygon? = null
    private var currentPolygon: RoundedPolygon? = null
    private var morph: Morph? = null
    var interpolation by mutableFloatStateOf(interpolation)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        if (size == Size.Zero) return RectangleShape.createOutline(
            size = size,
            layoutDirection = layoutDirection,
            density = density,
        )

        val startPolygon = previousShape.ensurePolygon(size, density)
        val endPolygon = currentShape.ensurePolygon(size, density)

        if (
            morph == null
            || startPolygon != previousPolygon
            || endPolygon != currentPolygon
        ) {
            previousPolygon = startPolygon
            currentPolygon = endPolygon
            morph = Morph(start = startPolygon, end = endPolygon)
        }

        path.rewind()
        requireNotNull(morph).toPath(
            progress = interpolation,
            path = path,
        )
        matrix.reset()

        matrix.scale(
            x = size.width / currentShape.lastBounds.width,
            y = size.height / currentShape.lastBounds.height,
        )

        matrix.translate(
            x = -currentShape.lastBounds.left,
            y = -currentShape.lastBounds.top,
        )
        path.transform(matrix)

        return Outline.Generic(path)
    }
}

/**
 * Gets a [Path] representation for a [RoundedPolygon] shape. Note that there is some rounding
 * happening (to the nearest thousandth), to work around rendering artifacts introduced by some
 * points being just slightly off from each other (far less than a pixel). This also allows for a
 * more optimal path, as redundant curves (usually a single point) can be detected and not added to
 * the resulting path.
 *
 * @param path an optional [Path] object which, if supplied, will avoid the function having to
 *   create a new [Path] object
 */
private fun RoundedPolygon.toPath(path: Path = Path()): Path {
    pathFromCubics(path, cubics)
    return path
}

private fun Morph.toPath(progress: Float, path: Path = Path()): Path {
    pathFromCubics(path, asCubics(progress))
    return path
}

private fun pathFromCubics(
    path: Path,
    cubics: List<Cubic>,
) {
    var first = true
    path.rewind()
    for (element in cubics) {
        if (first) {
            path.moveTo(element.anchor0X, element.anchor0Y)
            first = false
        }
        path.cubicTo(
            element.control0X,
            element.control0Y,
            element.control1X,
            element.control1Y,
            element.anchor1X,
            element.anchor1Y,
        )
    }
    path.close()
}

private fun Size.canReuse(other: Size?): Boolean {
    if (other == null) return false

    val aspectRatio = width / height
    val otherAspectRatio = other.width / other.height

    return aspectRatio / otherAspectRatio in (0.9f..1.1f)
}