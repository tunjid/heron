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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.heron.ui.UiTokens.withDim
import kotlin.math.absoluteValue

@Composable
fun Indicator(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.primary.withDim(true),
    indicatorSize: Dp = 6.dp,
    spacing: Dp = 6.dp,
    maxVisibleDots: Int = 5,
) {
    Indicator(
        modifier = modifier,
        itemCount = pagerState.pageCount,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        indicatorSize = indicatorSize,
        spacing = spacing,
        maxVisibleDots = maxVisibleDots,
        currentPosition = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
    )
}

@Composable
fun Indicator(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.primary.withDim(true),
    indicatorSize: Dp = 6.dp,
    spacing: Dp = 6.dp,
    maxVisibleDots: Int = 5,
) {
    val itemCount by remember { derivedStateOf { lazyListState.layoutInfo.totalItemsCount } }
    Indicator(
        modifier = modifier,
        itemCount = itemCount,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        indicatorSize = indicatorSize,
        spacing = spacing,
        maxVisibleDots = maxVisibleDots,
        currentPosition = {
            val firstVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()
            if (firstVisibleItem == null || firstVisibleItem.size == 0) 0f
            else lazyListState.firstVisibleItemIndex +
                (lazyListState.firstVisibleItemScrollOffset.toFloat() / firstVisibleItem.size)
        },
    )
}

@Composable
fun Indicator(
    modifier: Modifier = Modifier,
    itemCount: Int,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.primary.withDim(true),
    indicatorSize: Dp = 6.dp,
    spacing: Dp = 6.dp,
    maxVisibleDots: Int = 5,
    currentPosition: () -> Float,
) {
    if (itemCount <= 1) return

    val density = LocalDensity.current
    val indicatorSizePx = with(density) { indicatorSize.toPx() }
    val spacingPx = with(density) { spacing.toPx() }
    val stepPx = indicatorSizePx + spacingPx

    val totalWidth = (maxVisibleDots * stepPx) - spacingPx

    Canvas(
        modifier = modifier
            .width(with(density) { totalWidth.toDp() })
            .height(indicatorSize),
    ) {
        val currentPos = currentPosition()

        // Calculate the center of the visible window
        val viewCenter = if (itemCount <= maxVisibleDots) {
            (itemCount - 1) / 2f
        } else {
            // Clamp the view center so we don't scroll past the start or end
            val lowerBound = (maxVisibleDots / 2).toFloat()
            val upperBound = (itemCount - 1 - (maxVisibleDots / 2)).toFloat()
            currentPos.coerceIn(lowerBound, upperBound)
        }

        val canvasCenter = size.width / 2f

        for (i in 0 until itemCount) {
            // Calculate visual position relative to canvas center
            val distFromViewCenter = i - viewCenter
            val x = canvasCenter + distFromViewCenter * stepPx

            // Skip if far out of view
            if (x < -stepPx || x > size.width + stepPx) continue

            // Calculate scale based on distance from selection (Active vs Inactive)
            val distFromSelection = (i - currentPos).absoluteValue

            // Active dot is 1.2x bigger than inactive (1.0x).
            // Scale changes linearly with offset.
            var scale = if (distFromSelection < 1f) {
                1.2f - 0.2f * distFromSelection
            } else {
                1.0f
            }

            // Edge scaling for shifting effect
            if (itemCount > maxVisibleDots) {
                val distFromView = (i - viewCenter).absoluteValue
                val halfVisible = maxVisibleDots / 2f

                // Scale down dots as they approach the edge of the visible window
                val edgeDist = distFromView - (halfVisible - 1f)
                if (edgeDist > 0) {
                    val edgeScale = 1f - 0.5f * edgeDist.coerceAtMost(1f)
                    scale *= edgeScale
                }
            }

            val radius = (indicatorSizePx / 2f) * scale

            val color = if (distFromSelection < 1f) {
                lerp(activeColor, inactiveColor, distFromSelection)
            } else {
                inactiveColor
            }

            drawCircle(
                color = color,
                radius = radius,
                center = Offset(x, size.height / 2f),
            )
        }
    }
}
