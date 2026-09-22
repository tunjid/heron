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

/**
 * The bottom navigation surface. Most platforms render [CommonNavigationBar] (the Material
 * `NavigationBar`). iOS renders a native Liquid Glass `UITabBar` on iOS 26+, which is self contained
 * and so is not wrapped in a Compose surface, and renders [CommonNavigationBar] on older systems.
 *
 * The nav items already live on [AppScaffoldState], so this reads them directly rather than taking
 * them as parameters.
 */
@Composable
internal expect fun AppScaffoldState.PlatformNavigationBar(
    modifier: Modifier,
    onNavItemReselected: () -> Boolean,
)
