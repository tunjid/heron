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

package com.tunjid.heron.ui.preview

import androidx.compose.runtime.Composable
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState

/**
 * Builds a [PaneScaffoldState] suitable for `@Preview` and UI test conditions, so a screen's
 * top level `Route` composable can be rendered without a real [com.tunjid.treenav.compose.PaneScope].
 */
@Composable
fun rememberPanePreviewScaffoldState(): PaneScaffoldState {
    TODO("Not yet implemented")
}
