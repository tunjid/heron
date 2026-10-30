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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.floor
import kotlinx.coroutines.flow.collectLatest

@Stable
class SelectorState<T>(
    selectedItem: T,
    items: List<T>,
) {
    var selectedItem by mutableStateOf(selectedItem)
        internal set
    var items by mutableStateOf(items)
        internal set

    internal val selectedIndex
        get() = items.indexOf(selectedItem)

    internal val indicatorPosition = Animatable(
        initialValue = selectedIndex.coerceAtLeast(0).toFloat(),
    )

    private var itemCenters = IntArray(0)
    private var trackTop = 0

    internal fun itemNearestTrackOffset(
        y: Float,
    ): T? {
        if (itemCenters.isEmpty()) return null
        val index = itemCenters.indices.minBy { index ->
            abs(itemCenters[index] - (trackTop + y))
        }
        return items.getOrNull(index)
    }

    internal fun onItemsMeasured(
        itemCenters: IntArray,
        trackTop: Int,
    ) {
        this.itemCenters = itemCenters
        this.trackTop = trackTop
    }

    enum class IndicatorSide {
        Start,
        End,
    }
}

@Composable
fun <T> rememberSelectorState(
    selectedItem: T,
    items: List<T>,
) = remember {
    SelectorState(
        selectedItem = selectedItem,
        items = items,
    )
}.also {
    it.selectedItem = selectedItem
    it.items = items
}

@Composable
fun <T> Selector(
    modifier: Modifier = Modifier,
    state: SelectorState<T>,
    onItemSelected: (T) -> Unit,
    indicatorSide: SelectorState.IndicatorSide = SelectorState.IndicatorSide.Start,
    itemContent: @Composable (T) -> Unit,
) {
    LaunchedEffect(state) {
        snapshotFlow(state::selectedIndex)
            .collectLatest { index ->
                if (index < 0) return@collectLatest
                state.indicatorPosition.animateTo(
                    targetValue = index.toFloat(),
                    animationSpec = MatteThumbAnimationSpec,
                )
            }
    }

    val colors = rememberMatteColors()
    val currentOnItemSelected by rememberUpdatedState(onItemSelected)

    Layout(
        modifier = modifier
            .selectableGroup(),
        content = {
            state.items.forEach { item ->
                key(item) {
                    itemContent(item)
                }
            }
            // The indicator is always the last child: the column the thumb travels
            // in, which is not drawn but can be tapped, then the thumb.
            Box(
                modifier = Modifier
                    .pointerInput(state) {
                        detectTapGestures { offset ->
                            state.itemNearestTrackOffset(
                                y = offset.y,
                            )?.let(currentOnItemSelected)
                        }
                    },
            )
            Box(
                modifier = Modifier
                    .matteThumb(
                        colors = colors,
                    ),
            )
        },
        measurePolicy = { measurables, constraints ->
            val indicatorPadding = IndicatorPadding.roundToPx()
            val trackInset = MatteTrackInset.roundToPx()
            val thumbSize = MatteThumbSize.roundToPx()
            val trackThickness = MatteTrackThickness.roundToPx()
            val indicatorSpace = trackThickness + (indicatorPadding * 2)

            val itemConstraints = constraints
                .copy(
                    minWidth = 0,
                    minHeight = 0,
                )
                .offset(
                    horizontal = -indicatorSpace,
                )

            val itemPlaceables = measurables
                .subList(
                    fromIndex = 0,
                    toIndex = measurables.size - IndicatorChildCount,
                )
                .map { it.measure(itemConstraints) }

            val (trackMeasurable, thumbMeasurable) =
                measurables.takeLast(IndicatorChildCount)

            val itemCenters = IntArray(itemPlaceables.size)
            var itemY = 0
            itemPlaceables.forEachIndexed { index, placeable ->
                itemCenters[index] = itemY + (placeable.height / 2)
                itemY += placeable.height
            }

            // The track hugs the thumb at the first and last items as it does at
            // its sides.
            val trackTop = (itemCenters.firstOrNull() ?: 0) - (trackThickness / 2)
            val trackBottom = (itemCenters.lastOrNull() ?: 0) + (trackThickness / 2)

            state.onItemsMeasured(
                itemCenters = itemCenters,
                trackTop = trackTop,
            )

            val trackPlaceable = trackMeasurable.measure(
                Constraints.fixed(
                    width = trackThickness,
                    height = trackBottom - trackTop,
                ),
            )
            val thumbPlaceable = thumbMeasurable.measure(
                Constraints.fixed(
                    width = thumbSize,
                    height = thumbSize,
                ),
            )

            val width = constraints.constrainWidth(
                if (itemPlaceables.isEmpty()) 0
                else itemPlaceables.maxOf(Placeable::width) + indicatorSpace,
            )
            val height = constraints.constrainHeight(itemY)

            layout(
                width = width,
                height = height,
            ) {
                val itemX = when (indicatorSide) {
                    SelectorState.IndicatorSide.Start -> indicatorSpace
                    SelectorState.IndicatorSide.End -> 0
                }
                itemPlaceables.forEachIndexed { index, placeable ->
                    placeable.placeRelative(
                        x = itemX,
                        y = itemCenters[index] - (placeable.height / 2),
                    )
                }

                if (itemPlaceables.isEmpty()) return@layout

                val trackX = when (indicatorSide) {
                    SelectorState.IndicatorSide.Start -> indicatorPadding
                    SelectorState.IndicatorSide.End -> width - indicatorPadding - trackThickness
                }
                trackPlaceable.placeRelative(
                    x = trackX,
                    y = trackTop,
                )

                if (state.selectedIndex < 0) return@layout

                val thumbCenterY = itemCenters.centerAt(
                    index = state.indicatorPosition.value,
                )
                thumbPlaceable.placeRelative(
                    x = trackX + trackInset,
                    y = (thumbCenterY - (thumbSize / 2f)).fastRoundToInt(),
                )
            }
        },
    )
}

private fun IntArray.centerAt(
    index: Float,
): Float {
    if (isEmpty()) return 0f
    val clampedIndex = index.coerceIn(0f, lastIndex.toFloat())
    val flooredIndex = floor(clampedIndex).fastRoundToInt()
    val ceilingIndex = (flooredIndex + 1).coerceAtMost(lastIndex)
    return lerp(
        start = this[flooredIndex].toFloat(),
        stop = this[ceilingIndex].toFloat(),
        fraction = clampedIndex - flooredIndex,
    )
}

private val IndicatorPadding = 8.dp
private const val IndicatorChildCount = 2
