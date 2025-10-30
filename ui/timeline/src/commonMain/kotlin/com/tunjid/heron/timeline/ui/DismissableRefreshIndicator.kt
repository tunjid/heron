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

package com.tunjid.heron.timeline.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.PositionalThreshold
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DismissableRefreshIndicator(
    modifier: Modifier = Modifier,
    state: PullToRefreshState,
    isRefreshing: Boolean,
    onDismissRequest: () -> Unit,
) {
    val threshold = PositionalThreshold
    Box(
        modifier = modifier,
    ) {
        PullToRefreshDefaults.LoadingIndicator(
            state = state,
            isRefreshing = isRefreshing,
            threshold = threshold,
        )
        if (isRefreshing) Box(
            modifier = Modifier
                .matchParentSize()
                .offset {
                    val indicatorHeight = LoadingIndicatorDefaults.ContainerHeight.toPx()
                    val indicatorTranslation = state.distanceFraction * threshold.toPx()
                    IntOffset(
                        x = 0,
                        y = (indicatorTranslation - indicatorHeight).roundToInt(),
                    )
                }
                .clickable { onDismissRequest() },
        )
    }
}
