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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import kotlin.random.Random

@Immutable
internal class BokehPainter(
    private val seed: Int,
) : Painter() {

    private val random = Random(seed)

    private val palette = BokehPalettes[random.nextInt(BokehPalettes.size)]

    private val blobs = List(BlobCount) { index ->
        Blob(
            centerX = BlobMinCenter + random.nextFloat() * BlobCenterRange,
            centerY = BlobMinCenter + random.nextFloat() * BlobCenterRange,
            radiusFraction = BlobMinRadius + random.nextFloat() * BlobRadiusRange,
            // Skip the base color so every blob shows against it.
            color = palette[1 + (index + random.nextInt(palette.size - 1)) % (palette.size - 1)],
        )
    }

    private var scaledSize = Size.Unspecified
    private var brushes: List<Brush> = emptyList()

    override val intrinsicSize: Size
        get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        if (scaledSize != size) {
            brushes = brushesFor(size = size)
            scaledSize = size
        }

        drawRect(color = palette.first())
        brushes.forEach { brush ->
            drawRect(brush = brush)
        }
    }

    private fun brushesFor(
        size: Size,
    ): List<Brush> {
        val extent = size.maxDimension
        return blobs.map { blob ->
            // A gradual falloff keeps the edge of each blob from showing.
            Brush.radialGradient(
                0f to blob.color,
                BlobMid to blob.color.copy(alpha = BlobMidAlpha),
                1f to blob.color.copy(alpha = 0f),
                center = Offset(
                    x = blob.centerX * size.width,
                    y = blob.centerY * size.height,
                ),
                radius = blob.radiusFraction * extent,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BokehPainter) return false
        return seed == other.seed
    }

    override fun hashCode(): Int = seed
}

private class Blob(
    val centerX: Float,
    val centerY: Float,
    val radiusFraction: Float,
    val color: Color,
)

// Each palette is a base color followed by colors that sit well over it.
private val BokehPalettes = listOf(
    // Orchid
    listOf(
        Color(0xFFB57BC4),
        Color(0xFFD63FC6),
        Color(0xFF9AA6C8),
        Color(0xFFE48FC0),
    ),
    // Apricot
    listOf(
        Color(0xFFF0A672),
        Color(0xFFE9B85A),
        Color(0xFFC2459A),
        Color(0xFFF4B48A),
    ),
    // Dusk
    listOf(
        Color(0xFF9AA6C8),
        Color(0xFFE8B45C),
        Color(0xFF5C93C9),
        Color(0xFFA8B4D4),
    ),
    // Lagoon
    listOf(
        Color(0xFF609AA1),
        Color(0xFF6E9E5A),
        Color(0xFFC07FB4),
        Color(0xFF5590A0),
    ),
    // Ember
    listOf(
        Color(0xFFD9775A),
        Color(0xFFF2C46B),
        Color(0xFF9C4A8C),
        Color(0xFFE89A7A),
    ),
    // Tide
    listOf(
        Color(0xFF6C8FC7),
        Color(0xFF7FC8C0),
        Color(0xFFB79AD8),
        Color(0xFF6189CA),
    ),
)

private const val BlobCount = 5
private const val BlobMinCenter = -0.1f
private const val BlobCenterRange = 1.2f
private const val BlobMinRadius = 0.45f
private const val BlobRadiusRange = 0.35f
private const val BlobMid = 0.45f
private const val BlobMidAlpha = 0.5f
