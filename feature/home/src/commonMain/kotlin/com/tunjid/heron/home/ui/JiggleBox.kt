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

package com.tunjid.heron.home.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.random.Random

/**
 * A composable that applies a jiggling animation to its content.
 * The animation is similar to the iOS app icon rearrangement effect.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param isJiggling A boolean to control whether the jiggling animation is active.
 * @param content The composable content to be animated.
 */
@Composable
fun JiggleBox(
    modifier: Modifier = Modifier,
    isJiggling: Boolean = true,
    content: @Composable () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jiggle_transition")

    // Animate the rotation angle.
    // The angle and duration are randomized slightly for each instance of JiggleBox
    // to create a more natural, less uniform-looking effect.
    val angle = infiniteTransition.animateFloat(
        initialValue = -JiggleAngle,
        targetValue = JiggleAngle,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = JiggleDurationMillis + Random.nextInt(JiggleVariationDeltaMillis),
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "jiggle_rotation",
    )

    val jiggleModifier =
        if (isJiggling) Modifier.graphicsLayer { rotationZ = angle.value }
        else Modifier

    Box(
        modifier = modifier
            .then(jiggleModifier),
    ) {
        content()
    }
}

private const val JiggleAngle = 1.2f
private const val JiggleDurationMillis = 180
private const val JiggleVariationDeltaMillis = 40
