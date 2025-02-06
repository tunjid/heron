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

package com.tunjid.heron.scaffold.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import com.tunjid.composables.accumulatedoffsetnestedscrollconnection.AccumulatedOffsetNestedScrollConnection
import com.tunjid.composables.accumulatedoffsetnestedscrollconnection.rememberAccumulatedOffsetNestedScrollConnection
import com.tunjid.heron.ui.UiTokens


@Composable
fun bottomNavigationNestedScrollConnection(): AccumulatedOffsetNestedScrollConnection {
    val navigationBarInsets = WindowInsets.navigationBars
    return rememberAccumulatedOffsetNestedScrollConnection(
        invert = true,
        maxOffset = maxOffset@{
            Offset(
                x = 0f,
                y = navigationBarInsets.run {
                    getTop(this@maxOffset) + getBottom(this@maxOffset)
                } + UiTokens.bottomNavHeight.toPx()
            )
        },
        minOffset = { Offset.Zero },
    )
}
