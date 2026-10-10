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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

fun Modifier.blur(
    shape: Shape,
    clip: () -> Boolean = { false },
    radius: () -> Dp,
    progress: () -> Float,
): Modifier = graphicsLayer {
    blurEffect(
        radius = radius,
        progress = progress,
        shape = shape,
        clip = clip,
    )
    return@graphicsLayer
}

fun Modifier.verticalProgressiveBlur(
    edgeTreatment: TileMode = TileMode.Clamp,
    startRadius: Density.() -> Dp,
    endRadius: Density.() -> Dp,
): Modifier = this then ProgressiveBlurElement(
    edgeTreatment = edgeTreatment,
    spec = ProgressiveBlurSpec.TwoPoint(
        startRadius = startRadius,
        endRadius = endRadius,
    ),
)

fun Modifier.verticalProgressiveBlur(
    edgeTreatment: TileMode = TileMode.Clamp,
    stops: BlurStopsScope.(size: Size) -> Unit,
): Modifier = this then ProgressiveBlurElement(
    edgeTreatment = edgeTreatment,
    spec = ProgressiveBlurSpec.Stops(
        stops = stops,
    ),
)

fun GraphicsLayerScope.blurEffect(
    radius: () -> Dp,
    progress: () -> Float,
    shape: Shape,
    clip: () -> Boolean,
) {
    val currentRadius = radius()
    val currentProgress = progress()
    if (currentProgress <= 0f) return

    val horizontalBlurPixels = currentRadius.toPx() * currentProgress
    val verticalBlurPixels = currentRadius.toPx() * currentProgress

    if (horizontalBlurPixels <= 0f || verticalBlurPixels <= 0f) return

    this.renderEffect = BlurEffect(
        radiusX = horizontalBlurPixels,
        radiusY = verticalBlurPixels,
        edgeTreatment = TileMode.Decal,
    )

    this.shape = shape
    this.clip = clip()
}

interface BlurStopsScope : Density {
    fun stop(
        fraction: Float,
        radius: Dp,
    )
}

internal expect fun verticalProgressiveBlurEffect(
    fractions: FloatArray,
    radiiPx: FloatArray,
    count: Int,
    lineStartY: Float,
    lineEndY: Float,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect?

private sealed interface ProgressiveBlurSpec {
    data class TwoPoint(
        val startRadius: Density.() -> Dp,
        val endRadius: Density.() -> Dp,
    ) : ProgressiveBlurSpec

    data class Stops(
        val stops: BlurStopsScope.(size: Size) -> Unit,
    ) : ProgressiveBlurSpec
}

private data class ProgressiveBlurElement(
    val edgeTreatment: TileMode,
    val spec: ProgressiveBlurSpec,
) : ModifierNodeElement<ProgressiveBlurNode>() {

    override fun create() = ProgressiveBlurNode(
        edgeTreatment = edgeTreatment,
        spec = spec,
    )

    override fun update(node: ProgressiveBlurNode) {
        node.edgeTreatment = edgeTreatment
        node.spec = spec
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "verticalProgressiveBlur"
        properties["edgeTreatment"] = edgeTreatment
    }
}

private class ProgressiveBlurNode(
    var edgeTreatment: TileMode,
    var spec: ProgressiveBlurSpec,
) : Modifier.Node(),
    DrawModifierNode {

    private val stopsBuffer = BlurStopsBuffer()
    private var contentLayer: GraphicsLayer? = null

    private var bandLayer: GraphicsLayer? = null

    private var cachedEffect: RenderEffect? = null
    private var cachedBandTop = 0
    private var cachedBandBottom = 0
    private var cachedSize = Size.Unspecified
    private var cachedEdgeTreatment = edgeTreatment
    private var cachedDensity = Float.NaN
    private var cachedCount = 0
    private val cachedFractions = FloatArray(MaxBlurStops)
    private val cachedRadiiPx = FloatArray(MaxBlurStops)

    override fun onDetach() {
        val graphicsContext = requireGraphicsContext()
        contentLayer?.let(graphicsContext::releaseGraphicsLayer)
        bandLayer?.let(graphicsContext::releaseGraphicsLayer)
        contentLayer = null
        bandLayer = null
        cachedEffect = null
        cachedCount = 0
    }

    override fun ContentDrawScope.draw() {
        resolveStops()
        if (!isCached()) updateCache()

        val effect = cachedEffect
        if (effect == null) {
            drawContent()
            return
        }

        val graphicsContext = requireGraphicsContext()
        val content = contentLayer
            ?: graphicsContext.createGraphicsLayer().also {
                contentLayer = it
            }
        val band = bandLayer
            ?: graphicsContext.createGraphicsLayer().also {
                bandLayer = it
            }

        val bandTop = cachedBandTop
        val bandBottom = cachedBandBottom

        content.record {
            this@draw.drawContent()
        }
        clipRect(
            top = bandTop.toFloat(),
            bottom = bandBottom.toFloat(),
            clipOp = ClipOp.Difference,
        ) {
            drawLayer(content)
        }

        band.record(
            size = IntSize(
                width = ceil(size.width).toInt(),
                height = bandBottom - bandTop,
            ),
        ) {
            translate(top = -bandTop.toFloat()) {
                drawLayer(content)
            }
        }
        band.topLeft = IntOffset(
            x = 0,
            y = bandTop,
        )
        band.renderEffect = effect
        drawLayer(band)
    }

    private fun ContentDrawScope.resolveStops() {
        stopsBuffer.reset(density = this)
        when (val currentSpec = spec) {
            is ProgressiveBlurSpec.TwoPoint -> {
                stopsBuffer.stop(
                    fraction = 0f,
                    radius = currentSpec.startRadius(this),
                )
                stopsBuffer.stop(
                    fraction = 1f,
                    radius = currentSpec.endRadius(this),
                )
            }
            is ProgressiveBlurSpec.Stops -> currentSpec.stops(stopsBuffer, size)
        }
        require(stopsBuffer.count >= 2) {
            "expected between 2 and $MaxBlurStops stops but was ${stopsBuffer.count}"
        }
    }

    private fun ContentDrawScope.isCached(): Boolean {
        val count = stopsBuffer.count
        if (cachedCount != count) return false
        if (cachedSize != size) return false
        if (cachedDensity != density) return false
        if (cachedEdgeTreatment != edgeTreatment) return false
        for (index in 0 until count) {
            if (cachedFractions[index] != stopsBuffer.fractions[index]) return false
            if (cachedRadiiPx[index] != stopsBuffer.radiiPx[index]) return false
        }
        return true
    }

    private fun ContentDrawScope.updateCache() {
        val count = stopsBuffer.count
        val fractions = stopsBuffer.fractions
        val radiiPx = stopsBuffer.radiiPx

        cachedCount = count
        cachedSize = size
        cachedDensity = density
        cachedEdgeTreatment = edgeTreatment
        fractions.copyInto(
            destination = cachedFractions,
            endIndex = count,
        )
        radiiPx.copyInto(
            destination = cachedRadiiPx,
            endIndex = count,
        )

        var firstBlurred = -1
        var lastBlurred = -1
        var maxRadiusPx = 0f
        for (index in 0 until count) {
            val radiusPx = radiiPx[index]
            if (radiusPx <= 0f) continue
            if (firstBlurred < 0) firstBlurred = index
            lastBlurred = index
            maxRadiusPx = max(
                a = maxRadiusPx,
                b = radiusPx,
            )
        }
        if (firstBlurred < 0 || size.isEmpty()) {
            cachedEffect = null
            return
        }

        val padding = min(
            a = maxRadiusPx,
            b = MaxProgressiveBlurRadiusPx,
        )
        val blurStart =
            if (firstBlurred == 0) 0f
            else fractions[firstBlurred - 1] * size.height
        val blurEnd =
            if (lastBlurred == count - 1) size.height
            else fractions[lastBlurred + 1] * size.height

        val bandTop = floor(
            max(
                a = 0f,
                b = blurStart - padding,
            ),
        ).toInt()
        val bandBottom = ceil(
            min(
                a = size.height,
                b = blurEnd + padding,
            ),
        ).toInt()

        cachedBandTop = bandTop
        cachedBandBottom = bandBottom
        cachedEffect = verticalProgressiveBlurEffect(
            fractions = fractions,
            radiiPx = radiiPx,
            count = count,
            // The stops span the whole content, which starts above the band.
            lineStartY = -bandTop.toFloat(),
            lineEndY = size.height - bandTop,
            size = Size(
                width = size.width,
                height = (bandBottom - bandTop).toFloat(),
            ),
            density = this,
            edgeTreatment = edgeTreatment,
        )
    }
}

/**
 * A reusable [BlurStopsScope] that writes stops into fixed size buffers.
 */
private class BlurStopsBuffer : BlurStopsScope {
    val fractions = FloatArray(MaxBlurStops)
    val radiiPx = FloatArray(MaxBlurStops)

    var count = 0
        private set

    override var density = 1f
        private set

    override var fontScale = 1f
        private set

    fun reset(density: Density) {
        count = 0
        this.density = density.density
        fontScale = density.fontScale
    }

    override fun stop(
        fraction: Float,
        radius: Dp,
    ) {
        require(count < MaxBlurStops) {
            "expected between 2 and $MaxBlurStops stops but was ${count + 1}"
        }
        require(fraction in 0f..1f) {
            "expected a fraction between 0 and 1 but was $fraction"
        }
        require(count == 0 || fraction >= fractions[count - 1]) {
            "expected stops in non-decreasing fraction order but $fraction " +
                "follows ${fractions[count - 1]}"
        }
        fractions[count] = fraction
        radiiPx[count] = radius.toPx().coerceAtLeast(0f)
        count++
    }
}

const val MaxBlurStops = 16

internal const val MaxProgressiveBlurRadiusPx = 150f
