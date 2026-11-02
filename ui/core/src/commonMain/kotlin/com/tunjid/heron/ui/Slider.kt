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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.tunjid.heron.ui.modifiers.ifTrue
import kotlinx.coroutines.flow.collectLatest

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Color.White,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val currentValue by rememberUpdatedState(value.coerceIn(0f, 1f))
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

    var isDragging by remember { mutableStateOf(false) }
    val progress = remember { Animatable(value.coerceIn(0f, 1f)) }
    LaunchedEffect(progress) {
        snapshotFlow { currentValue }
            .collectLatest { target ->
                // The head follows a drag exactly, and springs to values set any other way.
                if (isDragging) progress.snapTo(target)
                else progress.animateTo(
                    targetValue = target,
                    animationSpec = MatteThumbAnimationSpec,
                )
            }
    }
    val thickness by animateFloatAsState(
        targetValue = if (isDragging) SliderDraggedScale else 1f,
        animationSpec = SliderThicknessAnimationSpec,
    )

    val layoutDirection = LocalLayoutDirection.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SliderMinHeight)
            .progressSemantics(
                value = value.coerceIn(0f, 1f),
            )
            .semantics {
                if (enabled) setProgress { target ->
                    currentOnValueChange(target.coerceIn(0f, 1f))
                    currentOnValueChangeFinished?.invoke()
                    true
                }
            }
            .ifTrue(enabled) {
                pointerInput(
                    layoutDirection,
                    interactionSource,
                ) {
                    val barHeight = SliderBarHeight.toPx()
                    fun valueAt(
                        x: Float,
                    ): Float {
                        val minX = barHeight / 2
                        val maxX = size.width - minX
                        val fraction = ((x - minX) / (maxX - minX)).coerceIn(0f, 1f)
                        return if (layoutDirection == LayoutDirection.Rtl) 1f - fraction
                        else fraction
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        // Consumed so a tap to seek doesn't also reach clickable ancestors.
                        down.consume()
                        val dragStart = DragInteraction.Start()
                        interactionSource?.tryEmit(dragStart)
                        currentOnValueChange(valueAt(down.position.x))
                        isDragging = true
                        var isDragCompleted = false
                        try {
                            isDragCompleted = horizontalDrag(down.id) { change ->
                                change.consume()
                                currentOnValueChange(valueAt(change.position.x))
                            }
                        } finally {
                            isDragging = false
                            interactionSource?.tryEmit(
                                if (isDragCompleted) DragInteraction.Stop(dragStart)
                                else DragInteraction.Cancel(dragStart),
                            )
                        }
                        currentOnValueChangeFinished?.invoke()
                    }
                }
            }
            .graphicsLayer {
                alpha = if (enabled) 1f else MatteDisabledAlpha
            }
            .drawBehind {
                val barHeight = SliderBarHeight.toPx() * thickness
                val cornerRadius = CornerRadius(barHeight / 2)
                val top = (size.height - barHeight) / 2
                drawRoundRect(
                    color = color.copy(alpha = color.alpha * SliderUnfilledAlpha),
                    topLeft = Offset(
                        x = 0f,
                        y = top,
                    ),
                    size = Size(
                        width = size.width,
                        height = barHeight,
                    ),
                    cornerRadius = cornerRadius,
                )
                // The head's centre runs from one rounded end to the other, so the fill
                // always reaches at least a full circle.
                val headCenter = (barHeight / 2) +
                    ((size.width - barHeight) * progress.value.coerceIn(0f, 1f))
                val filledWidth = headCenter + (barHeight / 2)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(
                        x = if (layoutDirection == LayoutDirection.Rtl) size.width - filledWidth
                        else 0f,
                        y = top,
                    ),
                    size = Size(
                        width = filledWidth,
                        height = barHeight,
                    ),
                    cornerRadius = cornerRadius,
                )
            },
    )
}

private val SliderMinHeight = 48.dp
private val SliderBarHeight = 4.dp

private const val SliderDraggedScale = 1.5f
private const val SliderUnfilledAlpha = 0.3f

private val SliderThicknessAnimationSpec = tween<Float>(
    durationMillis = 150,
)
