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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val AvatarSize = 40.dp

@Stable
object UiTokens {

    val avatarSize = 40.dp

    val toolbarHeight = 64.dp

    val tabsHeight = 48.dp

    val appBarButtonSize = 40.dp

    val LikeRed: Color = Color(0xFFE0245E)

    val RepostGreen: Color = Color(0xFF17BF63)

    val BookmarkBlue: Color = Color(0xFF1D9BF0)

    const val fabSharedElementZIndex = 14f
    const val appBarSharedElementZIndex = 12f
    const val navigationBarSharedElementZIndex = 2f

    val statusBarHeight: Dp
        @Composable get() = WindowInsets.statusBars.asPaddingValues().run {
            calculateTopPadding() + calculateBottomPadding()
        }

    val navigationBarHeight: Dp
        @Composable get() = WindowInsets.navigationBars.asPaddingValues().run {
            calculateTopPadding() + calculateBottomPadding()
        }

    fun bottomNavHeight(
        isCompact: Boolean,
    ): Dp = if (isCompact) 48.dp else 80.dp

    @Composable
    fun bottomNavAndInsetPaddingValues(
        top: Dp = 0.dp,
        horizontal: Dp = 0.dp,
        extraBottom: Dp = 0.dp,
        isCompact: Boolean,
    ): PaddingValues {
        val padding = navigationBarHeight + bottomNavHeight(isCompact)
        return remember(top, horizontal, extraBottom, padding) {
            PaddingValues(
                top = top,
                start = horizontal,
                end = horizontal,
                bottom = padding + extraBottom,
            )
        }
    }

    fun Color.withDim(
        dimmed: Boolean,
    ) = if (dimmed) copy(alpha = 0.6f) else this
}
