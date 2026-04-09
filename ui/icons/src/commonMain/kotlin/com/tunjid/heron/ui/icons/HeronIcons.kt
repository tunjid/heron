/*
 *    Copyright 2026 Adetunji Dahunsi
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

package com.tunjid.heron.ui.icons

/**
 * Namespace for the heron app's bundled [androidx.compose.ui.graphics.vector.ImageVector] set.
 *
 * Icons are exposed as extension properties on this object (for example
 * `HeronIcons.Home`), each declared in its own file in this package. The set is
 * intentionally small — it includes only the icons the app actually uses — so the
 * app does not need to depend on `material-icons-extended`, which bloats every
 * platform binary.
 *
 * Logos for AT Protocol and related atmosphere services live under [Atmospheric]
 * (for example `HeronIcons.Atmospheric.Bluesky`).
 */
data object HeronIcons {
    /**
     * Namespace for logos of AT Protocol and related atmosphere services.
     */
    data object Atmospheric
}
