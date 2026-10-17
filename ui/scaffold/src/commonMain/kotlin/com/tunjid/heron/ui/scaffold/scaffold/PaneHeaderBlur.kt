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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.heron.ui.PinnedCollapsingHeaderState
import com.tunjid.heron.ui.modifiers.ifNotNull
import com.tunjid.heron.ui.modifiers.verticalProgressiveBlur

@Stable
class PaneHeaderBlurState internal constructor(
    fade: Dp,
    height: Density.() -> Float,
    radius: () -> Dp,
    progress: () -> Float,
) {
    var radius by mutableStateOf(radius)
    var fade by mutableStateOf(fade)
    var height by mutableStateOf(height)
    var progress by mutableStateOf(progress)

    internal val currentRadius: Dp
        get() = progress().let { progress ->
            if (progress.isNaN()) 0.dp
            else radius() * progress.coerceIn(0f, 1f)
        }
}

@Composable
fun PaneScaffoldState.rememberPinnedHeaderBlurState(
    pinnedCollapsingHeaderState: PinnedCollapsingHeaderState,
): PaneHeaderBlurState? =
    if (prefersBlurredHeaders) rememberHeaderBlurState(
        height = { pinnedCollapsingHeaderState.bodyOverlap },
        progress = pinnedCollapsingHeaderState::bodyOverlapFraction,
    )
    else null

@Composable
fun PaneScaffoldState.rememberTopAppBarHeaderBlurState(
    topAppBarHeight: Dp,
    isOffsetByScroll: Boolean = false,
): PaneHeaderBlurState? =
    if (prefersBlurredHeaders) rememberHeaderBlurState(
        height = {
            when {
                isOffsetByScroll -> topAppBarHeight.toPx() + topAppBarOffset().y
                else -> topAppBarHeight.toPx()
            }
        },
        progress = ::topAppBarTransparencyFactor,
    )
    else null

@Composable
private fun PaneScaffoldState.rememberHeaderBlurState(
    height: Density.() -> Float,
    progress: () -> Float,
): PaneHeaderBlurState {
    val state = remember(this) {
        PaneHeaderBlurState(
            radius = {
                16.dp * headerBlurStrength.value
            },
            fade = 24.dp,
            height = height,
            progress = progress,
        )
    }
    state.height = height
    state.progress = progress

    DisposableEffect(this, state) {
        headerBlurStates.add(state)
        onDispose { headerBlurStates.remove(state) }
    }
    return state
}

fun Modifier.topAppBarBlur(
    blurState: PaneHeaderBlurState?,
): Modifier = headerBlur(
    state = blurState,
)

fun Modifier.pinnedHeaderBlur(
    state: PinnedCollapsingHeaderState,
    blurState: PaneHeaderBlurState?,
    scrimColor: Color,
    scrimAlpha: Float = 0.9f,
    fade: Dp = 24.dp,
): Modifier = this
    .headerBlur(state = blurState)
    // Drawn over the content, inside the blurred band.
    .drawWithContent {
        drawContent()
        val overlap = state.bodyOverlap
        if (overlap <= 0f) return@drawWithContent

        val color = scrimColor.copy(alpha = scrimAlpha * state.bodyOverlapFraction)
        val bandEnd = ((overlap - fade.toPx()) / overlap).coerceIn(0f, 1f)
        drawRect(
            brush = Brush.verticalGradient(
                0f to color,
                bandEnd to color,
                1f to color.copy(alpha = 0f),
                startY = 0f,
                endY = overlap,
            ),
            size = Size(
                width = size.width,
                height = overlap,
            ),
        )
    }

private fun Modifier.headerBlur(
    state: PaneHeaderBlurState?,
): Modifier = ifNotNull(state) {
    verticalProgressiveBlur { size ->
        val height = it.height(this)
        val currentRadius = it.currentRadius
        val fadeEnd =
            if (size.height <= 0f) 0f
            else (height / size.height).coerceIn(0f, 1f)
        val bandEnd =
            if (size.height <= 0f) 0f
            else ((height - it.fade.toPx()) / size.height).coerceIn(0f, fadeEnd)
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
}
