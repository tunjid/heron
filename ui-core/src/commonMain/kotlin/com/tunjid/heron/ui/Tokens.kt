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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val AvatarSize = 40.dp

@Stable
object UiTokens {

    val avatarSize = 40.dp

    val toolbarHeight = 64.dp

    val tabsHeight = 48.dp

    val bottomNavHeight: Dp = 80.dp

    val statusBarHeight: Dp
        @Composable get() = WindowInsets.statusBars.asPaddingValues().run {
            calculateTopPadding() + calculateBottomPadding()
        }

    val navigationBarHeight: Dp
        @Composable get() = WindowInsets.navigationBars.asPaddingValues().run {
            calculateTopPadding() + calculateBottomPadding()
        }
}
