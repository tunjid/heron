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

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import com.tunjid.composables.accumulatedoffsetnestedscrollconnection.AccumulatedOffsetNestedScrollConnection
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun AccumulatedOffsetNestedScrollConnection.PagerTopGapCloseEffect(
    pagerState: PagerState,
    firstVisibleItemIndex: () -> Int,
    firstVisibleItemScrollOffset: () -> Int,
    scrollBy: suspend (Float) -> Unit,
) {
    val updatedFirstVisibleItemIndex by rememberUpdatedState(firstVisibleItemIndex)
    val updatedFirstVisibleItemScrollOffset by rememberUpdatedState(firstVisibleItemScrollOffset)
    val updatedScrollBy by rememberUpdatedState(scrollBy)

    LaunchedEffect(pagerState) {
        snapshotFlow {
            val fraction = pagerState.currentPageOffsetFraction
            // Find next page
            when {
                fraction > 0 -> ceil(pagerState.currentPage + fraction)
                else -> floor(pagerState.currentPage + fraction)
            }.roundToInt()
        }
            .collect {
                // Already scrolled past the first
                if (updatedFirstVisibleItemIndex() != 0) return@collect
                val firstItemOffset = updatedFirstVisibleItemScrollOffset()
                val tabOffset = offset.y

                // tab offset is negative
                val gapToClose = firstItemOffset + tabOffset
                // Close the gap
                if (gapToClose < 0) updatedScrollBy(-gapToClose)
            }
    }
}

/**
 * The rounded difference between [maxOffset] and the current [offset]. For a bar that rests at
 * `offset == minOffset` and hides by accumulating toward [maxOffset] (e.g. an auto-hiding bottom
 * navigation bar), this is its still-on-screen extent: the full bar at rest, [IntOffset.Zero] once
 * hidden.
 */
val AccumulatedOffsetNestedScrollConnection.roundedMaxDelta: IntOffset
    get() = (maxOffset - offset).round()

fun AccumulatedOffsetNestedScrollConnection.verticalOffsetProgress(): Float {
    val minDimension = min(
        a = abs(minOffset.y),
        b = abs(maxOffset.y),
    )
    val maxDimension = max(
        a = abs(minOffset.y),
        b = abs(maxOffset.y),
    )
    val currentDifference = abs(offset.y) - minDimension
    val maxDifference = maxDimension - minDimension

    return currentDifference / maxDifference
}
