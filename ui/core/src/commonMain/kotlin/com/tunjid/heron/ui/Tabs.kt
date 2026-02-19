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
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import com.tunjid.heron.ui.TabsState.Companion.TabBackgroundColor
import kotlin.jvm.JvmInline
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

data class Tab(val title: String, val id: String = title, val hasUpdate: Boolean)

@Stable
class TabsState
private constructor(
    tabs: List<Tab>,
    isCollapsed: Boolean,
    val selectedTabIndex: () -> Float,
    val onTabSelected: (Int) -> Unit,
    val onTabReselected: (Int) -> Unit,
) {

    val tabList: List<Tab>
        get() = tabs

    var isCollapsed by mutableStateOf(isCollapsed)
        internal set

    private val derivedTabIndex by derivedStateOf { selectedTabIndex().roundToInt() }

    internal val tabs = mutableStateListOf(*(tabs.toTypedArray()))

    internal val visibleTabs
        get() =
            when {
                derivedTabIndex !in tabs.indices -> emptyList()
                isCollapsed -> listOf(tabs[derivedTabIndex])
                else -> tabs.toList()
            }

    internal val tabIndicatorIndex
        get() = if (isCollapsed) 0f else selectedTabIndex()

    companion object {
        @Composable
        fun rememberTabsState(
            tabs: List<Tab>,
            isCollapsed: Boolean = false,
            selectedTabIndex: () -> Float,
            onTabSelected: (Int) -> Unit,
            onTabReselected: (Int) -> Unit,
        ) =
            remember {
                    TabsState(
                        selectedTabIndex = selectedTabIndex,
                        isCollapsed = isCollapsed,
                        tabs = tabs,
                        onTabSelected = onTabSelected,
                        onTabReselected = onTabReselected,
                    )
                }
                .also {
                    if (it.tabs != tabs) {
                        it.tabs.clear()
                        it.tabs.addAll(tabs)
                    }
                    it.isCollapsed = isCollapsed
                }

        val TabBackgroundColor
            @Composable get() = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    }
}

@Composable
fun Tabs(
    modifier: Modifier = Modifier,
    tabsState: TabsState,
    tabContent: @Composable TabsState.(Tab) -> Unit = { Tab(tab = it) },
) =
    with(tabsState) {
        Box(modifier = modifier) {
            val lazyListState = rememberLazyListState()
            LazyRow(
                modifier = Modifier,
                state = lazyListState,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(
                    items = visibleTabs,
                    key = Tab::id,
                    itemContent = { tab ->
                        Box(
                            modifier = Modifier.animateItem(),
                            content = {
                                if (tab.hasUpdate)
                                    Badge(
                                        modifier = Modifier.align(Alignment.TopEnd).offset(y = 2.dp)
                                    )
                                tabContent(tab)
                            },
                        )
                    },
                )
            }
            Indicator(lazyListState, ::tabIndicatorIndex)
        }
    }

@Composable
fun TabsState.Tab(modifier: Modifier = Modifier, tab: Tab) {
    FilterChip(
        modifier = modifier,
        shape = TabShape,
        border = null,
        selected = false,
        onClick = click@{
                val index = tabs.indexOf(tab)
                if (index < 0) return@click

                if (index != selectedTabIndex().roundToInt()) onTabSelected(index)
                else onTabReselected(index)
            },
        label = { Text(tab.title) },
    )
}

@Composable
private fun BoxScope.Indicator(lazyListState: LazyListState, selectedTabIndex: () -> Float) {
    var packedSizeAndPosition by remember {
        mutableLongStateOf(TabSizeAndPosition(size = 0, position = 0).packed)
    }
    val sizeAndPosition = TabSizeAndPosition(packedSizeAndPosition)

    // Keep selected tab on screen
    LaunchedEffect(lazyListState) {
        snapshotFlow {
                val roundedIndex = selectedTabIndex().fastRoundToInt()

                val layoutInfo = lazyListState.layoutInfo
                val visibleItemsInfo = layoutInfo.visibleItemsInfo

                val visibleIndex = visibleItemsInfo.binarySearch { it.index - roundedIndex }
                if (visibleIndex < 0) return@snapshotFlow roundedIndex

                val item = visibleItemsInfo[visibleIndex]

                val isFullyVisible =
                    item.offset >= 0 && (item.offset + item.size) <= layoutInfo.viewportEndOffset

                if (isFullyVisible) null else roundedIndex
            }
            .collect { index -> if (index != null) lazyListState.animateScrollToItem(index) }
    }

    // Interpolated highlighted tab position
    LaunchedEffect(lazyListState) {
        snapshotFlow {
                val currentIndex = selectedTabIndex()
                val flooredIndex = floor(currentIndex).fastRoundToInt()
                val roundedIndex = ceil(currentIndex).fastRoundToInt()
                val fraction = currentIndex - flooredIndex

                val visibleItemsInfo = lazyListState.layoutInfo.visibleItemsInfo

                val flooredPosition = visibleItemsInfo.binarySearch { it.index - flooredIndex }
                if (flooredPosition < 0)
                    return@snapshotFlow visibleItemsInfo.firstOrNull().tabSizeAndPosition()

                val roundedPosition =
                    lazyListState.layoutInfo.visibleItemsInfo.binarySearch {
                        it.index - roundedIndex
                    }
                if (roundedPosition < 0)
                    return@snapshotFlow visibleItemsInfo.firstOrNull().tabSizeAndPosition()

                val floored = lazyListState.layoutInfo.visibleItemsInfo[flooredPosition]
                val rounded = lazyListState.layoutInfo.visibleItemsInfo[roundedPosition]

                TabSizeAndPosition(
                    size = lerp(floored.size, rounded.size, fraction),
                    position = lerp(floored.offset, rounded.offset, fraction),
                )
            }
            .collect { packedSizeAndPosition = it.packed }
    }

    val density = LocalDensity.current
    Box(
        Modifier.align(Alignment.CenterStart)
            .offset { IntOffset(x = sizeAndPosition.position, y = 0) }
            .height(32.dp)
            .matchParentSize()
            .width(with(density) { sizeAndPosition.size.toDp() })
            .background(color = TabBackgroundColor, shape = TabShape)
    )
}

private fun LazyListItemInfo?.tabSizeAndPosition() =
    if (this != null) TabSizeAndPosition(size = size, position = offset)
    else TabSizeAndPosition(0, 0)

@JvmInline
value class TabSizeAndPosition(val packed: Long) {
    constructor(size: Int, position: Int) : this(packInts(size, position))

    val size: Int
        get() = unpackInt1(packed)

    val position: Int
        get() = unpackInt2(packed)
}

private val TabShape = RoundedCornerShape(16.dp)

val PagerState.tabIndex
    get() = currentPage + currentPageOffsetFraction
