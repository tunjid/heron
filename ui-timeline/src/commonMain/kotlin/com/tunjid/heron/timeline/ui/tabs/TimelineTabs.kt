package com.tunjid.heron.timeline.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt

data class TimelineTab(
    val title: String,
    val hasUpdate: Boolean,
)

@Composable
fun TimelineTabs(
    modifier: Modifier = Modifier,
    tabs: List<TimelineTab>,
    selectedTabIndex: Float,
    onTabSelected: (Int) -> Unit,
    onTabReselected: (Int) -> Unit,
) {
    Box(modifier = modifier) {
        val lazyListState = rememberLazyListState()
        LazyRow(
            modifier = Modifier,
            state = lazyListState,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(
                items = tabs,
                key = { _, tab -> tab.title },
                itemContent = { index, tab ->
                    BadgedBox(
                        badge = {
                            if (tab.hasUpdate) Badge()
                        },
                        content = {
                            FilterChip(
                                modifier = Modifier,
                                shape = TabShape,
                                border = null,
                                selected = false,
                                onClick = {
                                    if (index != selectedTabIndex.roundToInt()) onTabSelected(index)
                                    else onTabReselected(index)
                                },
                                label = {
                                    Text(tab.title)
                                },
                            )
                        }
                    )
                }
            )
        }
        Indicator(lazyListState, selectedTabIndex)
    }
}

@Composable
private fun BoxScope.Indicator(
    lazyListState: LazyListState,
    selectedTabIndex: Float,
) {
    val updatedSelectedTabIndex by rememberUpdatedState(selectedTabIndex)
    var interpolatedOffset by remember { mutableStateOf(IntOffset.Zero) }

    // Keep selected tab on screen
    LaunchedEffect(lazyListState) {
        snapshotFlow {
            val layoutInfo = lazyListState.layoutInfo
            val roundedIndex = updatedSelectedTabIndex.roundToInt()

            if (roundedIndex == layoutInfo.totalItemsCount - 1)
                return@snapshotFlow layoutInfo.totalItemsCount - 1

            val index = layoutInfo.visibleItemsInfo.binarySearch {
                it.index - roundedIndex
            }
            if (index < 0) return@snapshotFlow roundedIndex
            val item = layoutInfo.visibleItemsInfo[index]

            if (item.offset + item.size > layoutInfo.viewportEndOffset)
                lazyListState.firstVisibleItemIndex + 1
            else lazyListState.firstVisibleItemIndex
        }
            .collect { lazyListState.animateScrollToItem(it) }
    }

    // Interpolated highlighted tab position
    LaunchedEffect(lazyListState) {
        snapshotFlow {
            val flooredIndex = floor(updatedSelectedTabIndex).roundToInt()
            val roundedIndex = round(updatedSelectedTabIndex).roundToInt()
            val fraction = updatedSelectedTabIndex - flooredIndex

            val flooredPosition = lazyListState.layoutInfo.visibleItemsInfo.binarySearch {
                it.index - flooredIndex
            }
            if (flooredPosition < 0) return@snapshotFlow IntOffset.Zero

            val roundedPosition = lazyListState.layoutInfo.visibleItemsInfo.binarySearch {
                it.index - roundedIndex
            }
            if (roundedPosition < 0) return@snapshotFlow IntOffset.Zero

            val floored = lazyListState.layoutInfo.visibleItemsInfo[flooredPosition]
            val rounded = lazyListState.layoutInfo.visibleItemsInfo[roundedPosition]

            IntOffset(
                lerp(floored.size, rounded.size, fraction),
                lerp(floored.offset, rounded.offset, fraction),
            )
        }
            .collect {
                interpolatedOffset = it
            }
    }

    val density = LocalDensity.current
    Box(
        Modifier
            .align(Alignment.CenterStart)
            .offset { IntOffset(x = interpolatedOffset.y, y = 0) }
            .height(32.dp)
            .matchParentSize()
            .width(with(density) { interpolatedOffset.x.toDp() })
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = TabShape,
            )
    )
}

val TabShape = RoundedCornerShape(16.dp)
