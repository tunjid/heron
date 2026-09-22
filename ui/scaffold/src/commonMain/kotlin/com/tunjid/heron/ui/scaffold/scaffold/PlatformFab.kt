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

package com.tunjid.heron.ui.scaffold.scaffold

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The rendered surface of [PaneFab]. All layout, visibility and shared element animation lives in
 * common [PaneFab]; only the pill surface is platform specific.
 *
 * Most platforms render [CommonFab] (the Material `FloatingActionButton`). iOS overrides this with a
 * native Liquid Glass surface on iOS 26+, and renders [CommonFab] on older systems.
 *
 * The iOS surface owns its own foreground (icon + label) rather than reusing [CommonFab], because
 * when the native glass is drawn as an interop overlay (so it can sample the Compose content behind
 * it), Compose can no longer paint on top of it.
 */
@Composable
internal expect fun PlatformFab(
    modifier: Modifier,
    expanded: Boolean,
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
)
