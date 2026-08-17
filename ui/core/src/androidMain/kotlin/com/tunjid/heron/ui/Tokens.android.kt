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

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.runtime.Composable

internal actual val WindowInsets.Companion.platformExtraStatusBars: WindowInsets
    @Composable get() = EmptyWindowInsets

@OptIn(ExperimentalLayoutApi::class)
internal actual val WindowInsets.Companion.stableStatusBars: WindowInsets
    @Composable get() = statusBarsIgnoringVisibility

@OptIn(ExperimentalLayoutApi::class)
internal actual val WindowInsets.Companion.stableNavigationBars: WindowInsets
    @Composable get() = navigationBarsIgnoringVisibility

@OptIn(ExperimentalLayoutApi::class)
internal actual val WindowInsets.Companion.stableSystemBars: WindowInsets
    @Composable get() = systemBarsIgnoringVisibility
