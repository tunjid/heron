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

import androidx.compose.foundation.shape.PolygonShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.media.images.ImageArgs
import com.tunjid.heron.media.images.ImageRequest
import kotlin.random.Random

/**
 * [ImageArgs] for the avatar of a collection.
 */
@Composable
internal fun rememberCollectionAvatarArgs(
    avatar: ImageUri?,
    collectionUri: RecordUri?,
    shape: PolygonShape,
): ImageArgs {
    val colorScheme = MaterialTheme.colorScheme
    return remember(
        avatar,
        collectionUri,
        shape,
        colorScheme,
    ) {
        ImageArgs(
            request = when {
                avatar != null -> ImageRequest.Network(
                    url = avatar.uri,
                )
                collectionUri != null -> {
                    // Sea glass is seafoam, tinted slightly by the theme so it sits in every palette.
                    val isDark = colorScheme.surface.luminance() < 0.5f
                    ImageRequest.Painted(
                        painter = BeachGlassPainter(
                            seed = collectionUri.uri.hashCode(),
                            bodyLight = lerp(
                                start = if (isDark) SeafoamDarkThemeLight else SeafoamLight,
                                stop = colorScheme.tertiaryContainer,
                                fraction = ThemeTint,
                            ),
                            bodyDeep = lerp(
                                start = if (isDark) SeafoamDarkThemeDeep else SeafoamDeep,
                                stop = colorScheme.tertiaryContainer,
                                fraction = ThemeTint,
                            ),
                            highlight = Color.White,
                        ),
                    )
                }
                else -> ImageRequest.Network(
                    url = BlueskyClouds.uri,
                )
            },
            contentScale = ContentScale.Crop,
            contentDescription = null,
            shape = shape,
        )
    }
}

/**
 * Frosted beach glass: an opaque, weathered piece of glass lit from the top left.
 *
 * Continuous tones come from a few large radial gradients: the body glows from light to deep,
 * faint mottles give it an uneven, cloudy finish, the edges catch the light where the glass is
 * thinner, and a soft highlight polishes it. A fine dither in light and deep frosts the surface.
 *
 * Everything is seeded from [seed] in normalized coordinates, so the glass scales with its
 * bounds like a cropped image. Gradients are only rebuilt when the size changes, and the frost
 * is drawn in one call per color.
 */
@Immutable
internal class BeachGlassPainter(
    private val seed: Int,
    private val bodyLight: Color,
    private val bodyDeep: Color,
    private val highlight: Color,
) : Painter() {

    private val random = Random(seed)

    private val mottles = List(MottleCount) { index ->
        Mottle(
            centerX = random.nextFloat(),
            centerY = random.nextFloat(),
            radiusFraction = MottleMinRadius + random.nextFloat() * MottleRadiusRange,
            color = if (index % 2 == 0) bodyDeep.copy(alpha = MottleDeepAlpha)
            else highlight.copy(alpha = MottleLightAlpha),
        )
    }

    private val frost = listOf(
        PointBatch(
            color = highlight.copy(alpha = FrostLightAlpha),
            diameterFraction = FrostDiameter,
            normalizedPoints = FloatArray(FrostLightPoints * 2) { random.nextFloat() },
        ),
        PointBatch(
            color = bodyDeep.copy(alpha = FrostDeepAlpha),
            diameterFraction = FrostDiameter,
            normalizedPoints = FloatArray(FrostDeepPoints * 2) { random.nextFloat() },
        ),
    )

    private val paint = Paint().apply {
        strokeCap = StrokeCap.Round
    }

    private var scaledSize = Size.Unspecified
    private var brushes: Brushes? = null

    override val intrinsicSize: Size
        get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        if (scaledSize != size) {
            frost.forEach { batch ->
                batch.scaleTo(size = size)
            }
            brushes = brushesFor(size = size)
            scaledSize = size
        }
        val currentBrushes = brushes ?: return

        drawRect(brush = currentBrushes.body)
        currentBrushes.mottles.forEachIndexed { index, brush ->
            val mottle = mottles[index]
            drawCircle(
                brush = brush,
                radius = mottle.radiusFraction * size.maxDimension,
                center = Offset(
                    x = mottle.centerX * size.width,
                    y = mottle.centerY * size.height,
                ),
            )
        }

        val extent = size.minDimension
        drawIntoCanvas { canvas ->
            frost.forEach { batch ->
                paint.color = batch.color
                paint.strokeWidth = batch.diameterFraction * extent
                canvas.drawRawPoints(
                    pointMode = PointMode.Points,
                    points = batch.scaledPoints,
                    paint = paint,
                )
            }
        }

        drawRect(brush = currentBrushes.rim)
        drawRect(brush = currentBrushes.sheen)
    }

    private fun brushesFor(
        size: Size,
    ): Brushes {
        val extent = size.maxDimension
        return Brushes(
            body = Brush.radialGradient(
                colors = listOf(
                    bodyLight,
                    bodyDeep,
                ),
                center = Offset(
                    x = LightX * size.width,
                    y = LightY * size.height,
                ),
                radius = BodyRadius * extent,
            ),
            mottles = mottles.map { mottle ->
                Brush.radialGradient(
                    colors = listOf(
                        mottle.color,
                        mottle.color.copy(alpha = 0f),
                    ),
                    center = Offset(
                        x = mottle.centerX * size.width,
                        y = mottle.centerY * size.height,
                    ),
                    radius = mottle.radiusFraction * extent,
                )
            },
            rim = Brush.radialGradient(
                RimStart to highlight.copy(alpha = 0f),
                1f to highlight.copy(alpha = RimAlpha),
                center = size.center,
                radius = RimRadius * extent,
            ),
            sheen = Brush.radialGradient(
                0f to highlight.copy(alpha = SheenAlpha),
                SheenMid to highlight.copy(alpha = SheenMidAlpha),
                1f to highlight.copy(alpha = 0f),
                center = Offset(
                    x = SheenX * size.width,
                    y = SheenY * size.height,
                ),
                radius = SheenRadius * extent,
            ),
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BeachGlassPainter) return false
        return seed == other.seed &&
            bodyLight == other.bodyLight &&
            bodyDeep == other.bodyDeep &&
            highlight == other.highlight
    }

    override fun hashCode(): Int {
        var result = seed
        result = 31 * result + bodyLight.hashCode()
        result = 31 * result + bodyDeep.hashCode()
        result = 31 * result + highlight.hashCode()
        return result
    }
}

private class Brushes(
    val body: Brush,
    val mottles: List<Brush>,
    val rim: Brush,
    val sheen: Brush,
)

private class Mottle(
    val centerX: Float,
    val centerY: Float,
    val radiusFraction: Float,
    val color: Color,
)

/**
 * Points sharing a [color] and a diameter, a fraction of the smaller dimension of the bounds.
 * [normalizedPoints] holds x, y pairs in the range 0 to 1.
 */
private class PointBatch(
    val color: Color,
    val diameterFraction: Float,
    private val normalizedPoints: FloatArray,
) {
    val scaledPoints = FloatArray(normalizedPoints.size)

    fun scaleTo(
        size: Size,
    ) {
        for (index in normalizedPoints.indices step 2) {
            scaledPoints[index] = normalizedPoints[index] * size.width
            scaledPoints[index + 1] = normalizedPoints[index + 1] * size.height
        }
    }
}

private val SeafoamLight = Color(0xFFCDE8DC)
private val SeafoamDeep = Color(0xFF9CC8B4)
private val SeafoamDarkThemeLight = Color(0xFF2E6E58)
private val SeafoamDarkThemeDeep = Color(0xFF1C4A3A)
private const val ThemeTint = 0.2f

private const val BodyRadius = 0.85f
private const val LightX = 0.36f
private const val LightY = 0.32f

private const val MottleCount = 4
private const val MottleMinRadius = 0.22f
private const val MottleRadiusRange = 0.2f
private const val MottleDeepAlpha = 0.22f
private const val MottleLightAlpha = 0.14f

private const val FrostLightPoints = 900
private const val FrostDeepPoints = 500
private const val FrostDiameter = 0.007f
private const val FrostLightAlpha = 0.13f
private const val FrostDeepAlpha = 0.12f

private const val RimStart = 0.72f
private const val RimRadius = 0.62f
private const val RimAlpha = 0.28f

private const val SheenX = 0.32f
private const val SheenY = 0.28f
private const val SheenRadius = 0.7f
private const val SheenMid = 0.55f
private const val SheenAlpha = 0.32f
private const val SheenMidAlpha = 0.06f
