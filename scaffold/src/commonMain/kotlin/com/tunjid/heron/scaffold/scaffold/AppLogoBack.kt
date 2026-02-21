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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

@Composable
internal fun rememberBackLogoAnimation(
    isArrow: Boolean,
): LogoAnimation {
    val bodyColor = rememberUpdatedState(
        MaterialTheme.colorScheme.onSurface,
    )
    val backgroundColor = rememberUpdatedState(
        ButtonDefaults.filledTonalButtonColors().containerColor,
    )

    val navigationEventDispatcher = LocalNavigationEventDispatcherOwner.current!!
        .navigationEventDispatcher

    val transitionState = navigationEventDispatcher
        .transitionState
        .collectAsState()

    val progressState = animateFloatAsState(
        targetValue = when (val value = transitionState.value) {
            NavigationEventTransitionState.Idle ->
                if (isArrow) BackArrowProgress
                else HeronLogoProgress
            is NavigationEventTransitionState.InProgress ->
                if (isArrow) BackArrowProgress
                else BackArrowProgress - value.latestEvent.progress
        },
        label = "LogoAnimation",
    )

    return remember {
        BackAnimation(
            backgroundColor = backgroundColor::value,
            bodyColor = bodyColor::value,
            progress = progressState::value,
        )
    }
}

internal class BackAnimation(
    val backgroundColor: ColorProducer,
    val bodyColor: ColorProducer,
    val progress: () -> Float,
) : LogoAnimation {

    override fun DrawScope.drawPart(
        part: HeronPart,
    ) {
        val backgroundColor = backgroundColor()
        val bodyColor = bodyColor()
        val progress = progress()

        // Fade: 1.0 -> 0.0
        val fadeAlpha = 1f - progress

        when (part) {
            HeronPart.Head -> {
                // Head center approx (15, 3) -> Canvas Center (17.5, 24)
                val startX = 15f
                val startY = 3f
                val endX = 17.5f
                val endY = 24f
                val headScale = 1f - (0.8f * progress)

                val circleCenter = Offset(
                    x = startX + (endX - startX) * progress,
                    y = startY + (endY - startY) * progress,
                )
                val circleRadius = 24f * progress
                val headColor = lerp(
                    start = HeadColor,
                    stop = backgroundColor,
                    fraction = progress,
                )

                // 1. Head: Simulate head morph to circle
                if (progress > 0) {
                    drawCircle(
                        color = headColor,
                        radius = circleRadius,
                        center = circleCenter,
                    )
                }

                withTransform(
                    transformBlock = {
                        // Move with the circle
                        translate(
                            left = (endX - startX) * progress,
                            top = (endY - startY) * progress,
                        )
                        scale(
                            scaleX = headScale,
                            scaleY = headScale,
                            pivot = Offset(
                                x = 15f,
                                y = 3f,
                            ),
                        )
                    },
                    drawBlock = {
                        drawPath(
                            path = part.path,
                            color = headColor,
                            alpha = fadeAlpha,
                        )
                    },
                )
            }
            HeronPart.Body -> {
                if (fadeAlpha > 0) drawPath(
                    path = part.path,
                    color = bodyColor,
                    alpha = fadeAlpha,
                )
            }
            HeronPart.Legs -> {
                // Leg: Rotates 0 -> -90
                val legRotation = -90f * progress

                // Leg: Translates (0,0) -> (4, -9)
                val legTx = 4f * progress
                val legTy = -9f * progress

                withTransform(
                    transformBlock = {
                        translate(
                            left = legTx,
                            top = legTy,
                        )
                        rotate(
                            degrees = legRotation,
                            pivot = Offset(
                                x = 16.5f,
                                y = 33.5f,
                            ),
                        )
                    },
                    drawBlock = {
                        drawPath(
                            path = part.path,
                            color = bodyColor,
                        )
                    },
                )
            }
            HeronPart.Beak -> {
                // Beak: Translates (0,0) -> (5, 12)
                val beakTx = 5f * progress
                val beakTy = 11f * progress

                // Top beak caret: Does not rotate
                val topCaretRot = 0f

                // Bottom beak caret: Rotates 0 -> 75
                val bottomCaretRot = 75f * progress
                // 4. Beak: Split into two instances for the Caret
                val beakTipPivot = Offset(
                    x = 0f,
                    y = 12.1f,
                )

                // Instance A: Top Caret
                withTransform(
                    transformBlock = {
                        translate(
                            left = beakTx,
                            top = beakTy,
                        )
                        rotate(
                            degrees = topCaretRot,
                            pivot = beakTipPivot,
                        )
                    },
                    drawBlock = {
                        drawPath(
                            path = part.path,
                            color = bodyColor,
                        )
                    },
                )

                // Instance B: Bottom Caret
                withTransform(
                    transformBlock = {
                        translate(
                            left = beakTx,
                            top = beakTy,
                        )
                        rotate(
                            degrees = bottomCaretRot,
                            pivot = beakTipPivot,
                        )
                    },
                    drawBlock = {
                        drawPath(
                            path = part.path,
                            color = bodyColor,
                        )
                    },
                )
            }
        }
    }
}

private const val BackArrowProgress = 1f
private const val HeronLogoProgress = 0f
