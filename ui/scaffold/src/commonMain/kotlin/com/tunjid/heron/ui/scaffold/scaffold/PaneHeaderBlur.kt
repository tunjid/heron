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

package com.tunjid.heron.ui.scaffold.scaffold

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.heron.ui.PinnedCollapsingHeaderState
import com.tunjid.heron.ui.modifiers.verticalProgressiveBlur

@Stable
class PaneHeaderState internal constructor(
    fade: Dp,
    top: Density.() -> Float,
    height: Density.() -> Float,
    radius: () -> Dp,
    progress: () -> Float,
) {
    var radius by mutableStateOf(radius)
    var fade by mutableStateOf(fade)

    /**
     * Where the band of content behind the header starts, from the top of the pane.
     * Used to check if shared elements occlude the header.
     */
    var top by mutableStateOf(top)

    /**
     * The height of the band of content behind the header. It is also the extent
     * of the blur, measured from the top of the blurred content.
     */
    var height by mutableStateOf(height)
    var progress by mutableStateOf(progress)

    internal val currentRadius: Dp
        get() = progress().let { progress ->
            if (progress.isNaN()) 0.dp
            else radius() * progress.coerceIn(0f, 1f)
        }
}

@Composable
fun PaneScaffoldState.rememberPinnedHeaderState(
    pinnedCollapsingHeaderState: PinnedCollapsingHeaderState,
): PaneHeaderState = rememberHeaderState(
    // The body scrolls up behind the bottom of the pinned header.
    top = {
        pinnedCollapsingHeaderState.pinnedHeight - pinnedCollapsingHeaderState.bodyOverlap
    },
    height = { pinnedCollapsingHeaderState.bodyOverlap },
    progress = pinnedCollapsingHeaderState::bodyOverlapFraction,
)

@Composable
fun PaneScaffoldState.rememberTopAppBarHeaderState(
    topAppBarHeight: Dp,
    isOffsetByScroll: Boolean = false,
): PaneHeaderState = rememberHeaderState(
    top = { 0f },
    height = {
        when {
            isOffsetByScroll -> topAppBarHeight.toPx() + topAppBarOffset().y
            else -> topAppBarHeight.toPx()
        }
    },
    progress = ::topAppBarTransparencyFactor,
)

@Composable
private fun PaneScaffoldState.rememberHeaderState(
    top: Density.() -> Float,
    height: Density.() -> Float,
    progress: () -> Float,
): PaneHeaderState {
    val state = remember(this) {
        PaneHeaderState(
            radius = {
                // Registered even without blur, so occlusion checks still work.
                if (prefersBlurredHeaders) 16.dp * headerBlurStrength.value
                else 0.dp
            },
            fade = 24.dp,
            top = top,
            height = height,
            progress = progress,
        )
    }
    state.top = top
    state.height = height
    state.progress = progress

    DisposableEffect(this, state) {
        headerStates.add(state)
        onDispose { headerStates.remove(state) }
    }
    return state
}

fun Modifier.pinnedHeaderScrim(
    state: PinnedCollapsingHeaderState,
    scrimColor: Color,
    scrimAlpha: Float = 0.9f,
    fade: Dp = 24.dp,
): Modifier = drawBehind {
    val overlap = state.bodyOverlap
    if (overlap <= 0f) return@drawBehind

    val color = scrimColor.copy(alpha = scrimAlpha * state.bodyOverlapFraction)
    val bandStart = state.pinnedHeight - overlap
    val bandEnd = ((overlap - fade.toPx()) / overlap).coerceIn(0f, 1f)
    drawRect(
        brush = Brush.verticalGradient(
            0f to color,
            bandEnd to color,
            1f to color.copy(alpha = 0f),
            startY = bandStart,
            endY = state.pinnedHeight,
        ),
        topLeft = Offset(
            x = 0f,
            y = bandStart,
        ),
        size = Size(
            width = size.width,
            height = overlap,
        ),
    )
}

fun Modifier.headerBlur(
    state: PaneHeaderState,
): Modifier = verticalProgressiveBlur { size ->
    val height = state.height(this)
    val currentRadius = state.currentRadius
    val fadeEnd =
        if (size.height <= 0f) 0f
        else (height / size.height).coerceIn(0f, 1f)
    val bandEnd =
        if (size.height <= 0f) 0f
        else ((height - state.fade.toPx()) / size.height).coerceIn(0f, fadeEnd)
    stop(
        fraction = 0f,
        radius = currentRadius,
    )
    stop(
        fraction = bandEnd,
        radius = currentRadius,
    )
    stop(
        fraction = fadeEnd,
        radius = 0.dp,
    )
}
