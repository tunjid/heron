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

val PredictiveBackContentTransform: PaneScope<ThreePane, *>.() -> ContentTransform =
    PredictiveBackContentTransformFactory()::contentTransform

private class PredictiveBackContentTransformFactory {
    var wasPreviewedInPredictiveBack = false

    fun contentTransform(
        scope: PaneScope<ThreePane, *>,
    ): ContentTransform = with(scope) {
        ContentTransform(
            if (wasPreviewedInPredictiveBack) EnterTransition.None else fadeIn(),
            fadeOut(targetAlpha = if (inPredictiveBack) 0.9f else 0f),
        ).adaptTo(paneScope = this)
            .also {
                if (wasPreviewedInPredictiveBack) wasPreviewedInPredictiveBack = false
                else if (!isActive && inPredictiveBack) wasPreviewedInPredictiveBack = true
            }
    }
}
