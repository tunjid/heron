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

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.Modifier

/**
 * Animates pane chrome (the fab, navigation rail and navigation bar) with its pane's navigation
 * transitions when there's no shared element match during the transition.
 *
 * It must be placed after Modifier.animateBounds, otherwise any placement animation in the
 * transition will be undone by its lookahead position.
 */
context(paneScaffoldState: PaneScaffoldState)
internal fun Modifier.paneChromeEnterExit(
    enterTransition: EnterTransition,
    exitTransition: ExitTransition,
) = this then with(paneScaffoldState) {
    Modifier.animateEnterExit(
        enter = enterTransition,
        exit = exitTransition,
    )
}

internal data object NavigationBarSharedElementKey

internal data object NavigationRailSharedElementKey

internal val NavBarEnterTransition: EnterTransition =
    slideInVertically(initialOffsetY = { it })

internal val NavBarExitTransition: ExitTransition =
    slideOutVertically(targetOffsetY = { it })

internal fun PaneScaffoldState.navRailEnterTransition(): EnterTransition =
    slideInHorizontally(initialOffsetX = ::navRailTransitionOffset)

internal fun PaneScaffoldState.navRailExitTransition(): ExitTransition =
    slideOutHorizontally(targetOffsetX = ::navRailTransitionOffset)

internal val NavigationRailBoundsTransform = BoundsTransform { _, _ -> snap() }

/**
 * The offset that slides the rail off its edge. [NavRailEdge] is absolute, as are horizontal
 * slide offsets.
 */
private fun PaneScaffoldState.navRailTransitionOffset(
    navRailWidth: Int,
) = when (navRailEdge) {
    NavRailEdge.Start -> -navRailWidth
    NavRailEdge.End -> navRailWidth
}
