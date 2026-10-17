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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.tunjid.composables.collapsingheader.CollapsingHeaderState
import com.tunjid.composables.collapsingheader.rememberCollapsingHeaderState
import kotlin.math.max
import kotlin.math.min

/**
 * A state derived from [CollapsingHeaderState] that collapses the header to near zero,
 * while it's public APIs represent the header collapsing to a [pinnedHeight].
 *
 * This allows the body scroll behind the header, while the header itself remains pinned at
 * [pinnedHeight].
 *
 * @see [CollapsingHeaderState].
 */
@Stable
class PinnedCollapsingHeaderState internal constructor(
    val collapsingHeaderState: CollapsingHeaderState,
    val pinnedHeight: Float,
) {
    val expandedHeight: Float
        get() = collapsingHeaderState.expandedHeight

    val translation: Float
        // Translation increases as its collapsed. Cap it at max translation.
        get() = min(
            a = collapsingHeaderState.translation,
            b = maxTranslation,
        )

    /**
     * The progress between the expanded and pinned states.
     * It goes from 0F at expanded to 1F at pinned.
     */
    val progress: Float
        get() = if (maxTranslation <= 0f) 1f else translation / maxTranslation

    /**
     * The body's displacement behind the header.
     * It goes from 0F to [pinnedHeight].
     */
    val bodyOverlap: Float
        get() = (collapsingHeaderState.translation - maxTranslation).coerceIn(
            minimumValue = 0f,
            maximumValue = pinnedHeight,
        )

    /**
     * [bodyOverlap] represented as a fraction.
     * It goes from 0F to 1F.
     */
    val bodyOverlapFraction: Float
        get() = if (pinnedHeight <= 0f) 0f else bodyOverlap / pinnedHeight

    private val maxTranslation: Float
        get() = max(
            a = 0f,
            b = expandedHeight - pinnedHeight,
        )
}

@Composable
fun rememberPinnedCollapsingHeaderState(
    pinnedHeight: Dp,
    initialExpandedHeight: Dp,
): PinnedCollapsingHeaderState {
    val density = LocalDensity.current
    val collapsingHeaderState = rememberCollapsingHeaderState(
        collapsedHeight = with(density) { Dp.Hairline.toPx() },
        initialExpandedHeight = with(density) { initialExpandedHeight.toPx() },
    )
    val pinnedHeightPx = with(density) { pinnedHeight.toPx() }
    return remember(
        collapsingHeaderState,
        pinnedHeightPx,
    ) {
        // Draw the header above the body so the body can scroll behind it.
        collapsingHeaderState.headerZIndex = 1f
        PinnedCollapsingHeaderState(
            collapsingHeaderState = collapsingHeaderState,
            pinnedHeight = pinnedHeightPx,
        )
    }
}
