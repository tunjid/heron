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

package com.tunjid.heron.scaffold.scaffold

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.composables.backpreview.backPreview
import com.tunjid.heron.ui.modifiers.animatedRoundedCornerClip
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.adaptTo

fun Modifier.predictiveBackPlacement(
    paneScaffoldState: PaneScaffoldState,
): Modifier = with(paneScaffoldState) {
    val shouldDrawBackground = paneState.pane == ThreePane.Primary &&
        inPredictiveBack &&
        isActive &&
        appState.dismissBehavior != AppState.DismissBehavior.Gesture.DragToPop

    ifTrue(shouldDrawBackground) {
        backPreview(appState.backPreviewState)
    }
        .animatedRoundedCornerClip(
            cornerRadius = if (shouldDrawBackground) 16.dp else 0.dp,
        )
}

fun predictiveBackContentTransformProvider(): PaneScope<ThreePane, *>.() -> ContentTransform =
    PredictiveBackContentTransformProvider()::contentTransform

private class PredictiveBackContentTransformProvider {
    private val previewedRouteIds = mutableSetOf<String>()

    fun contentTransform(
        scope: PaneScope<ThreePane, *>,
    ): ContentTransform = with(scope) {
        val routeId = paneState.currentDestination?.id
        val wasPreviewed = routeId in previewedRouteIds

        val isStillVisible = wasPreviewed && isActive

        ContentTransform(
            targetContentEnter =
            if (isStillVisible) EnterTransition.None
            else fadeIn(
                animationSpec = NavigationAnimationSpec,
            ),
            initialContentExit =
            if (isStillVisible) ExitTransition.None
            else fadeOut(
                animationSpec = NavigationAnimationSpec,
                targetAlpha = if (inPredictiveBack) PredictiveBackTargetAlpha else FullAlpha,
            ),
        ).adaptTo(paneScope = this)
            .also {
                if (wasPreviewed) previewedRouteIds.remove(routeId)
                else if (!isActive && inPredictiveBack && routeId != null) {
                    previewedRouteIds.clear()
                    previewedRouteIds.add(routeId)
                }
            }
    }
}

private val NavigationAnimationSpec = tween<Float>(700)

private const val PredictiveBackTargetAlpha = 0.9f
private const val FullAlpha = 0f
