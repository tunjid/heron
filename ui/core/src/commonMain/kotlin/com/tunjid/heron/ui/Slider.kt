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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import com.tunjid.heron.ui.modifiers.ifTrue
import kotlinx.coroutines.flow.collectLatest

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val currentValue by rememberUpdatedState(value.coerceIn(0f, 1f))
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

    var isDragging by remember { mutableStateOf(false) }
    val progress = remember { Animatable(value.coerceIn(0f, 1f)) }
    LaunchedEffect(progress) {
        snapshotFlow { currentValue }
            .collectLatest { target ->
                // The thumb follows a drag exactly, and springs to values set any other way.
                if (isDragging) progress.snapTo(target)
                else progress.animateTo(
                    targetValue = target,
                    animationSpec = MatteThumbAnimationSpec,
                )
            }
    }

    val colors = rememberMatteColors()
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
                pointerInput(layoutDirection) {
                    val inset = MatteTrackInset.toPx()
                    val thumbSize = MatteThumbSize.toPx()
                    fun valueAt(
                        x: Float,
                    ): Float {
                        val minX = inset + (thumbSize / 2)
                        val maxX = size.width - minX
                        val fraction = ((x - minX) / (maxX - minX)).coerceIn(0f, 1f)
                        return if (layoutDirection == LayoutDirection.Rtl) 1f - fraction
                        else fraction
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        currentOnValueChange(valueAt(down.position.x))
                        isDragging = true
                        horizontalDrag(down.id) { change ->
                            change.consume()
                            currentOnValueChange(valueAt(change.position.x))
                        }
                        isDragging = false
                        currentOnValueChangeFinished?.invoke()
                    }
                }
            }
            .graphicsLayer {
                alpha = if (enabled) 1f else MatteDisabledAlpha
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MatteTrackThickness)
                .matteTrack(
                    colors = colors,
                ),
        )
        Box(
            modifier = Modifier
                .padding(start = MatteTrackInset)
                .layout { measurable, constraints ->
                    val thumbSize = MatteThumbSize.roundToPx()
                    val travel = constraints.maxWidth - MatteTrackInset.roundToPx() - thumbSize
                    // The spring may overshoot, but the thumb stays within the track.
                    val width =
                        thumbSize + (travel * progress.value.coerceIn(0f, 1f)).fastRoundToInt()
                    val placeable = measurable.measure(
                        Constraints.fixed(
                            width = width.coerceAtLeast(thumbSize),
                            height = thumbSize,
                        ),
                    )
                    layout(
                        width = placeable.width,
                        height = placeable.height,
                    ) {
                        placeable.placeRelative(
                            x = 0,
                            y = 0,
                        )
                    }
                }
                .matteThumb(
                    colors = colors,
                ),
        )
    }
}

private val SliderMinHeight = 48.dp
