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

package com.tunjid.heron.ui.modifiers

import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.DeferredTargetAnimation
import androidx.compose.animation.core.ExperimentalAnimatableApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.approachLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalAnimatableApi::class)
fun Modifier.animateConstraints(
    sizeAnimation: DeferredTargetAnimation<IntSize, AnimationVector2D>,
    coroutineScope: CoroutineScope
) =
    this.approachLayout(
        isMeasurementApproachInProgress = { lookaheadSize ->
            // Update the target of the size animation.
            sizeAnimation.updateTarget(lookaheadSize, coroutineScope)
            // Return true if the size animation has pending target change or is currently
            // running.
            !sizeAnimation.isIdle
        }
    ) { measurable, c ->
        // In the measurement approach, the goal is to gradually reach the destination size
        // (i.e. lookahead size). To achieve that, we use an animation to track the current
        // size, and animate to the destination size whenever it changes. Once the animation
        // finishes, the approach is complete.

        // First, update the target of the animation, and read the current animated size.
        val (width, height) = sizeAnimation.updateTarget(lookaheadSize, coroutineScope)
        // Then create fixed size constraints using the animated size
        val animatedConstraints = Constraints.fixed(width, height)
        // Measure child with animated constraints.
        val placeable = measurable.measure(animatedConstraints)
        val (w, h) = c.constrain(IntSize(placeable.width, placeable.height))
        layout(w, h) { placeable.place(0, 0) }
    }