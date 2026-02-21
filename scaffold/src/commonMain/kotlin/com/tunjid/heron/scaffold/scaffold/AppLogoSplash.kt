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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.keyframes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.tunjid.heron.ui.UiTokens

@Composable
internal fun rememberSplashLogoAnimation(): LogoAnimation {
    val bodyColor = rememberUpdatedState(
        MaterialTheme.colorScheme.onSurface,
    )

    val animatable = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = keyframes {
                durationMillis = UiTokens.splashScreenDuration.inWholeMilliseconds.toInt()

                dipKeyframe using FastOutSlowInEasing
                raiseKeyFrame using FastOutSlowInEasing
            },
        )
    }
    return remember {
        SplashLogoAnimation(
            bodyColor = bodyColor::value,
            rotation = animatable::value,
        )
    }
}

internal class SplashLogoAnimation(
    val bodyColor: ColorProducer,
    val rotation: () -> Float,
) : LogoAnimation {
    override fun DrawScope.drawPart(
        part: HeronPart,
    ) {
        val bodyColor = bodyColor()
        val degrees = rotation()

        when (part) {
            HeronPart.Legs -> drawPath(
                path = part.path,
                color = bodyColor,
            )
            else -> withTransform(
                transformBlock = {
                    rotate(
                        degrees = degrees,
                        pivot = RotationPivot,
                    )
                },
                drawBlock = {
                    drawPath(
                        path = part.path,
                        color = HeadColor,
                    )
                },
            )
        }
    }
}

private val KeyframesSpec.KeyframesSpecConfig<Float>.dipKeyframe: KeyframesSpec.KeyframeEntity<Float>
    get() = (-35f) at (durationMillis * 0.45).toInt()

private val KeyframesSpec.KeyframesSpecConfig<Float>.raiseKeyFrame: KeyframesSpec.KeyframeEntity<Float>
    get() = 0f at (durationMillis * 0.90).toInt()

private val RotationPivot = Offset(
    x = 16.5f,
    y = 22f,
)
