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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.ui.UiTokens

/**
 * Holds window-aware display values for timeline grids so each grid (and item) reads its sizing
 * from a single place. Derived purely from the window, since a pane is always at most as wide as
 * the window.
 */
@Stable
class TimelineDisplayState
internal constructor(
    private val density: () -> Density,
    private val windowWidthPx: () -> Int,
) {
    /**
     * The card size to feed to `StaggeredGridCells.Adaptive`. Returns the presentation's baseline
     * [cardSize] unless the full window would fit more than [maxColumns] of them, in which case it
     * grows just enough to cap the column count. The window is an upper bound on every pane's
     * width, so a narrower pane simply renders fewer columns.
     */
    @Stable
    fun cardSize(presentation: Timeline.Presentation): Dp =
        when (val widthDp = with(density()) { windowWidthPx().toDp() }) {
            in 0.dp..UiTokens.SecondaryPaneMinWidthBreakpoint -> presentation.cardSize
            else ->
                maxOf(
                    a = presentation.cardSize,
                    b = widthDp.minus(UiTokens.NavRailWidth).div(presentation.maxColumns),
                )
        }

    @Stable
    fun horizontalPadding(presentation: Timeline.Presentation): Dp =
        presentation.timelineHorizontalPadding

    @Stable
    fun horizontalItemSpacing(presentation: Timeline.Presentation): Dp =
        presentation.lazyGridHorizontalItemSpacing

    @Stable
    fun verticalItemSpacing(presentation: Timeline.Presentation): Dp =
        presentation.lazyGridVerticalItemSpacing
}

@Composable
fun rememberTimelineDisplayState(): TimelineDisplayState {
    val density = rememberUpdatedState(LocalDensity.current)
    val windowWidthPx = rememberUpdatedState(LocalWindowInfo.current.containerSize.width)
    return remember {
        TimelineDisplayState(
            density = density::value,
            windowWidthPx = windowWidthPx::value,
        )
    }
}
