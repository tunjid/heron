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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.DropShadowPainter
import androidx.compose.ui.graphics.shadow.InnerShadowPainter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Immutable
internal class MatteColors(
    val track: Color,
    val thumb: Color,
    val mutedThumb: Color,
    val shade: Color,
    val highlight: Color,
    val thumbLift: Color,
    val socket: Color,
)

@Composable
internal fun rememberMatteColors(): MatteColors {
    val colorScheme = MaterialTheme.colorScheme
    val isLight = colorScheme.surface.luminance() > 0.5f
    return remember(
        colorScheme,
        isLight,
    ) {
        MatteColors(
            track = colorScheme.primaryContainer.copy(
                alpha = MatteTrackAlpha,
            ),
            thumb = colorScheme.primary.copy(
                alpha = MatteThumbAlpha,
            ),
            mutedThumb = colorScheme.outline.copy(
                alpha = if (isLight) 0.45f else 0.55f,
            ),
            shade = if (isLight) MatteLightThemeShade else MatteDarkThemeShade,
            // A bright highlight on a dark surface reads as a glow, so it is a whisper there.
            highlight = Color.White.copy(
                alpha = if (isLight) 0.95f else 0.07f,
            ),
            thumbLift = Color.White.copy(
                alpha = if (isLight) 0.28f else 0.16f,
            ),
            // A socket's floor is pressed a touch darker than the surface it sits in.
            socket = Color.Black
                .copy(
                    alpha = if (isLight) 0.03f else 0.15f,
                )
                .compositeOver(colorScheme.surface),
        )
    }
}

internal fun Modifier.matteTrack(
    colors: MatteColors,
) = drawWithCache {
    val shade = InnerShadowPainter(
        shape = CircleShape,
        shadow = Shadow(
            radius = MatteDepth * 2,
            color = colors.shade,
            offset = DpOffset(
                x = MatteDepth,
                y = MatteDepth,
            ),
        ),
    )
    val highlight = InnerShadowPainter(
        shape = CircleShape,
        shadow = Shadow(
            radius = MatteDepth * 2,
            color = colors.highlight,
            offset = DpOffset(
                x = -MatteDepth,
                y = -MatteDepth,
            ),
        ),
    )
    val cornerRadius = CornerRadius(size.minDimension / 2)
    onDrawBehind {
        drawRoundRect(
            color = colors.track,
            cornerRadius = cornerRadius,
        )
        with(shade) {
            draw(size = size)
        }
        with(highlight) {
            draw(size = size)
        }
    }
}

/**
 * A chamfer etched into the surface: a wall sloping away from the light at the top start, around
 * a flat floor. Returns a function that draws it at the origin with the floor color given.
 */
internal fun CacheDrawScope.matteChamfer(
    colors: MatteColors,
    chamferSize: Size,
): DrawScope.(floor: Color) -> Unit {
    val bevel = MatteChamferBevel.toPx()
    val floorSize = Size(
        width = chamferSize.width - (bevel * 2),
        height = chamferSize.height - (bevel * 2),
    )
    val wall = Brush.linearGradient(
        0f to colors.shade,
        0.75f to colors.highlight,
        start = Offset.Zero,
        end = Offset(
            x = chamferSize.width,
            y = chamferSize.height,
        ),
    )
    val floorShade = InnerShadowPainter(
        shape = CircleShape,
        shadow = Shadow(
            radius = 1.dp,
            color = colors.shade,
        ),
    )
    return { floor ->
        drawRoundRect(
            brush = wall,
            size = chamferSize,
            cornerRadius = CornerRadius(chamferSize.minDimension / 2),
        )
        translate(
            left = bevel,
            top = bevel,
        ) {
            drawRoundRect(
                color = floor,
                size = floorSize,
                cornerRadius = CornerRadius(floorSize.minDimension / 2),
            )
            with(floorShade) {
                draw(size = floorSize)
            }
        }
    }
}

internal fun Modifier.matteThumb(
    colors: MatteColors,
    color: () -> Color = colors::thumb,
) = drawWithCache {
    val outline = Path().apply {
        addRoundRect(
            RoundRect(
                rect = size.toRect(),
                cornerRadius = CornerRadius(size.minDimension / 2),
            ),
        )
    }
    val shade = DropShadowPainter(
        shape = CircleShape,
        shadow = Shadow(
            radius = MatteDepth * 2,
            color = colors.shade,
            offset = DpOffset(
                x = MatteDepth,
                y = MatteDepth,
            ),
        ),
    )
    val highlight = DropShadowPainter(
        shape = CircleShape,
        shadow = Shadow(
            radius = MatteDepth * 1.5f,
            color = colors.highlight,
            offset = DpOffset(
                x = MatteDepth * -0.75f,
                y = MatteDepth * -0.75f,
            ),
        ),
    )
    // The rim catches the most light along the top.
    val rimLight = InnerShadowPainter(
        shape = CircleShape,
        shadow = Shadow(
            radius = 1.dp,
            color = MatteRimLight,
            offset = DpOffset(
                x = 0.dp,
                y = 1.dp,
            ),
        ),
    )
    val sheen = Brush.linearGradient(
        0f to colors.thumbLift,
        0.6f to Color.Transparent,
        1f to MatteThumbDip,
        start = Offset.Zero,
        end = Offset(
            x = size.width,
            y = size.height,
        ),
    )
    val rimWidth = MatteRimWidth.toPx()
    val rimStroke = Stroke(width = rimWidth)
    onDrawBehind {
        // The thumb is translucent, so its shadows must not show through it.
        clipPath(
            path = outline,
            clipOp = ClipOp.Difference,
        ) {
            with(shade) {
                draw(size = size)
            }
            with(highlight) {
                draw(size = size)
            }
        }
        drawPath(
            path = outline,
            color = color(),
        )
        drawPath(
            path = outline,
            brush = sheen,
        )
        with(rimLight) {
            draw(size = size)
        }
        drawRoundRect(
            color = MatteRimEdge,
            topLeft = Offset(
                x = rimWidth / 2,
                y = rimWidth / 2,
            ),
            size = Size(
                width = size.width - rimWidth,
                height = size.height - rimWidth,
            ),
            cornerRadius = CornerRadius((size.minDimension - rimWidth) / 2),
            style = rimStroke,
        )
    }
}

internal val MatteThumbSize = 24.dp

internal val MatteTrackInset = 4.dp

internal val MatteTrackThickness = MatteThumbSize + (MatteTrackInset * 2)

internal val MatteThumbAnimationSpec = spring<Float>(
    dampingRatio = 0.6f,
    stiffness = Spring.StiffnessMediumLow,
)

internal const val MatteDisabledAlpha = 0.38f

private val MatteChamferBevel = 3.dp

private const val MatteTrackAlpha = 0.8f
private const val MatteThumbAlpha = 0.7f

private val MatteDepth = 2.5.dp

private val MatteRimWidth = 1.dp

private val MatteLightThemeShade = Color(0x38142846)
private val MatteDarkThemeShade = Color.Black.copy(alpha = 0.55f)
private val MatteThumbDip = Color.Black.copy(alpha = 0.12f)
private val MatteRimLight = Color.White.copy(alpha = 0.35f)
private val MatteRimEdge = Color.White.copy(alpha = 0.22f)
