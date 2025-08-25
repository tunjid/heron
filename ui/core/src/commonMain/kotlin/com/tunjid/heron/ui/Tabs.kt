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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

data class Tab(
    val title: String,
    val hasUpdate: Boolean,
)

@Stable
class TabsState private constructor(
    tabs: List<Tab>,
    val selectedTabIndex: () -> Float,
    val onTabSelected: (Int) -> Unit,
    val onTabReselected: (Int) -> Unit,
) {

    val tabList: List<Tab> get() = tabs

    internal val tabs = mutableStateListOf(*(tabs.toTypedArray()))

    companion object {
        @Composable
        fun rememberTabsState(
            tabs: List<Tab>,
            selectedTabIndex: () -> Float,
            onTabSelected: (Int) -> Unit,
            onTabReselected: (Int) -> Unit,
        ) = remember {
            TabsState(
                selectedTabIndex = selectedTabIndex,
                tabs = tabs,
                onTabSelected = onTabSelected,
                onTabReselected = onTabReselected,
            )
        }.also {
            if (it.tabs != tabs) {
                it.tabs.clear()
                it.tabs.addAll(tabs)
            }
        }
    }
}

@Composable
fun Tabs(
    modifier: Modifier = Modifier,
    tabsState: TabsState,
    tabContent: @Composable TabsState.(Tab) -> Unit = { Tab(tab = it) },
) = with(tabsState) {
    Box(modifier = modifier) {
        val lazyListState = rememberLazyListState()
        LazyRow(
            modifier = modifier,
            state = lazyListState,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(
                items = tabs,
                key = Tab::title,
                itemContent = { tab ->
                    BadgedBox(
                        modifier = Modifier,
                        badge = {
                            if (tab.hasUpdate) Badge()
                        },
                        content = {
                            tabContent(tab)
                        },
                    )
                },
            )
        }
        Indicator(lazyListState, selectedTabIndex)
    }
}

@Composable
fun TabsState.Tab(
    modifier: Modifier = Modifier,
    tab: Tab,
) {
    FilterChip(
        modifier = modifier,
        shape = TabShape,
        border = null,
        selected = false,
        onClick = click@{
            val index = tabs.indexOf(tab)
            if (index < 0) return@click

            if (index != selectedTabIndex().roundToInt()) {
                onTabSelected(index)
            } else {
                onTabReselected(index)
            }
        },
        label = {
            Text(tab.title)
        },
    )
}

@Composable
private fun BoxScope.Indicator(
    lazyListState: LazyListState,
    selectedTabIndex: () -> Float,
) {
    var interpolatedOffset by remember { mutableStateOf(IntOffset.Zero) }

    // Keep selected tab on screen
    LaunchedEffect(lazyListState) {
        snapshotFlow {
            val layoutInfo = lazyListState.layoutInfo
            val roundedIndex = selectedTabIndex().roundToInt()

            if (roundedIndex == layoutInfo.totalItemsCount - 1) {
                return@snapshotFlow layoutInfo.totalItemsCount - 1
            }

            val index = layoutInfo.visibleItemsInfo.binarySearch {
                it.index - roundedIndex
            }
            if (index < 0) return@snapshotFlow roundedIndex
            val item = layoutInfo.visibleItemsInfo[index]

            if (item.offset + item.size > layoutInfo.viewportEndOffset) {
                lazyListState.firstVisibleItemIndex + 1
            } else {
                lazyListState.firstVisibleItemIndex
            }
        }
            .collect { lazyListState.animateScrollToItem(it) }
    }

    // Interpolated highlighted tab position
    LaunchedEffect(lazyListState) {
        snapshotFlow {
            val currentIndex = selectedTabIndex()
            val flooredIndex = floor(currentIndex).roundToInt()
            val roundedIndex = ceil(currentIndex).roundToInt()
            val fraction = currentIndex - flooredIndex

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
            ),
    )
}

private val TabShape = RoundedCornerShape(16.dp)

val PagerState.tabIndex get() = currentPage + currentPageOffsetFraction
