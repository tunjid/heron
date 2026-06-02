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

package com.tunjid.heron

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.tunjid.composables.lazy.list.interpolatedFirstItemIndex
import com.tunjid.composables.lazy.pager.interpolatedFirstItemIndex
import com.tunjid.composables.lazy.staggeredgrid.interpolatedFirstItemIndex
import kotlin.math.min
import kotlin.math.round
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun LazyStaggeredGridState.interpolatedVisibleIndexEffect(
    denominator: Int,
    itemsAvailable: Int,
    onIndex: (Float) -> Unit,
) {
    interpolatedVisibleIndexEffect(
        scrollableState = this,
        itemsAvailable = itemsAvailable,
        denominator = denominator,
        isEmpty = { layoutInfo.visibleItemsInfo.isEmpty() },
        interpolatedFirstItemIndex = { interpolatedFirstItemIndex() },
        onIndex = onIndex,
    )
}

@Composable
fun LazyListState.interpolatedVisibleIndexEffect(
    denominator: Int,
    itemsAvailable: Int,
    onIndex: (Float) -> Unit,
) {
    interpolatedVisibleIndexEffect(
        scrollableState = this,
        itemsAvailable = itemsAvailable,
        denominator = denominator,
        isEmpty = { layoutInfo.visibleItemsInfo.isEmpty() },
        interpolatedFirstItemIndex = { interpolatedFirstItemIndex() },
        onIndex = onIndex,
    )
}

@Composable
fun PagerState.interpolatedVisibleIndexEffect(
    denominator: Int,
    itemsAvailable: Int,
    onIndex: (Float) -> Unit,
) {
    interpolatedVisibleIndexEffect(
        scrollableState = this,
        itemsAvailable = itemsAvailable,
        denominator = denominator,
        isEmpty = { layoutInfo.visiblePagesInfo.isEmpty() },
        interpolatedFirstItemIndex = { interpolatedFirstItemIndex() },
        onIndex = onIndex,
    )
}

@Composable
private fun <T : ScrollableState> interpolatedVisibleIndexEffect(
    scrollableState: T,
    itemsAvailable: Int,
    denominator: Int,
    isEmpty: T.() -> Boolean,
    interpolatedFirstItemIndex: T.() -> Float,
    onIndex: (Float) -> Unit,
) {
    LaunchedEffect(scrollableState, itemsAvailable) {
        snapshotFlow {
                if (!scrollableState.isScrollInProgress) return@snapshotFlow Float.NaN
                if (itemsAvailable == 0) return@snapshotFlow Float.NaN

                if (scrollableState.isEmpty()) return@snapshotFlow Float.NaN

                val firstIndex =
                    min(
                        a = scrollableState.interpolatedFirstItemIndex(),
                        b = itemsAvailable.toFloat(),
                    )

                if (firstIndex.isNaN()) Float.NaN else firstIndex.inDenominationsOf(denominator)
            }
            .distinctUntilChanged()
            .collect { if (!it.isNaN()) onIndex(it) }
    }
}

private fun Float.inDenominationsOf(denominator: Int): Float =
    (round(this * denominator) / denominator.toFloat())
