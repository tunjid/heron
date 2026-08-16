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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable

internal actual val WindowInsets.Companion.platformExtraStatusBars: WindowInsets
    @Composable get() = EmptyWindowInsets

// iOS derives every inset from UIView.safeAreaInsets; there is no visibility signal to ignore.
internal actual val WindowInsets.Companion.stableStatusBars: WindowInsets
    @Composable get() = statusBars

internal actual val WindowInsets.Companion.stableNavigationBars: WindowInsets
    @Composable get() = navigationBars

internal actual val WindowInsets.Companion.stableSystemBars: WindowInsets
    @Composable get() = systemBars
