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

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.composables.backpreview.backPreview
import com.tunjid.heron.ui.modifiers.animatedRoundedCornerClip
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.treenav.compose.Adaptation
import com.tunjid.treenav.compose.NavigationEventStatus
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.threepane.ThreePane

fun Modifier.predictiveBackPlacement(
    paneScaffoldState: PaneScaffoldState,
): Modifier = with(paneScaffoldState) {
    val shouldDrawBackground = paneState.pane == ThreePane.Primary &&
        inPredictiveBack &&
        isActive &&
        appState.dismissBehavior != AppState.DismissBehavior.Gesture.DragToPop

    ifTrue(shouldDrawBackground) {
        backPreview(backPreviewState)
    }
        .animatedRoundedCornerClip(
            cornerRadius = if (shouldDrawBackground) 16.dp else 0.dp,
        )
}

interface NavigationContentTransformer {
    fun contentTransform(
        scope: PaneScope<ThreePane, *>,
    ): ContentTransform
}

@Stable
internal object PredictiveBackContentTransformer : NavigationContentTransformer {
    override fun contentTransform(
        scope: PaneScope<ThreePane, *>,
    ): ContentTransform = with(scope) {
        ContentTransform(
            targetContentEnter =
            fadeIn(
                animationSpec = NavigationAnimationSpec,
                initialAlpha = if (isStillVisible) Opaque else Transparent,
            ),
            initialContentExit =
            fadeOut(
                animationSpec = NavigationAnimationSpec,
                targetAlpha = if (inPredictiveBack) PredictiveBackTargetAlpha else Transparent,
            ),
        )
    }
}

private val PaneScope<ThreePane, *>.isStillVisible: Boolean
    get() {
        if (!isActive) return false
        return navigationEventStatus is NavigationEventStatus.Completed.Cancelled ||
            paneState.adaptations.any { adaptation ->
                adaptation is Adaptation.Same ||
                    (adaptation is Adaptation.Swap<*> && adaptation.to == paneState.pane)
            }
    }

private val NavigationAnimationSpec = tween<Float>(400)

private const val PredictiveBackTargetAlpha = 0.9f
private const val Transparent = 0f
private const val Opaque = 1f
