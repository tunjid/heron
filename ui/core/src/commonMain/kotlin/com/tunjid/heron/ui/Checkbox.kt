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

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val light by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = MatteLightAnimationSpec,
    )
    val mark by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = CheckboxMarkAnimationSpec,
    )
    val colors = rememberMatteColors()

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .then(
                if (onCheckedChange != null) Modifier.toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    enabled = enabled,
                    role = Role.Checkbox,
                    interactionSource = interactionSource,
                    indication = null,
                )
                else Modifier,
            )
            .graphicsLayer {
                alpha = if (enabled) 1f else MatteDisabledAlpha
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(CheckboxSize)
                .checkboxSocket(
                    colors = colors,
                    light = { light },
                    mark = { mark },
                ),
        )
    }
}

/**
 * A rounded square socket etched into the surface. Once checked, it lights its floor
 * and etches a check into the light.
 */
private fun Modifier.checkboxSocket(
    colors: MatteColors,
    light: () -> Float,
    mark: () -> Float,
) = drawWithCache {
    val socket = matteChamfer(
        colors = colors,
        chamferSize = size,
        cornerRadius = CheckboxCornerRadius.toPx(),
    )
    // The check, laid out on a 28 unit grid.
    val unit = size.minDimension / CheckGridSize
    val check = Path().apply {
        moveTo(
            x = 8.5f * unit,
            y = 14.5f * unit,
        )
        lineTo(
            x = 12.3f * unit,
            y = 18.3f * unit,
        )
        lineTo(
            x = 19.7f * unit,
            y = 10.1f * unit,
        )
    }
    val checkMeasure = PathMeasure().apply {
        setPath(
            path = check,
            forceClosed = false,
        )
    }
    val checkLength = checkMeasure.length
    val drawnCheck = Path()
    val checkStroke = Stroke(
        width = 2.4f * unit,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )
    val highlightOffset = Offset(
        x = 0.6f * unit,
        y = 0.8f * unit,
    )
    onDrawBehind {
        socket(
            lerp(
                start = colors.socket,
                stop = colors.track,
                fraction = light().coerceIn(0f, 1f),
            ),
        )

        val markProgress = mark().coerceIn(0f, 1f)
        if (markProgress <= 0f) return@onDrawBehind
        drawnCheck.reset()
        checkMeasure.getSegment(
            startDistance = 0f,
            stopDistance = checkLength * markProgress,
            destination = drawnCheck,
            startWithMoveTo = true,
        )
        translate(
            left = highlightOffset.x,
            top = highlightOffset.y,
        ) {
            drawPath(
                path = drawnCheck,
                color = colors.etchHighlight,
                style = checkStroke,
            )
        }
        drawPath(
            path = drawnCheck,
            color = colors.etch,
            style = checkStroke,
        )
    }
}

private val CheckboxSize = 28.dp
private val CheckboxCornerRadius = 9.dp

private const val CheckGridSize = 28f

private val CheckboxMarkAnimationSpec = tween<Float>(
    durationMillis = 220,
    delayMillis = 60,
    easing = FastOutSlowInEasing,
)
