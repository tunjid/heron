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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.tunjid.heron.ui.UiTokens
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import com.tunjid.treenav.compose.UpdatedMovableSharedElementOf

@Composable
fun MovableElementSharedTransitionScope.AppLogo(modifier: Modifier, isRootDestination: Boolean) {
    UpdatedMovableSharedElementOf(
        sharedContentState = rememberSharedContentState(AppLogo),
        zIndexInOverlay = UiTokens.navigationIconZIndex,
        state = isRootDestination,
        modifier = modifier,
        sharedElement = { isRootDestination, innerModifier ->
            // This is written so only theme changes cause recompositions
            // All snapshot state is read in the DrawScope lambda
            val bodyColor = MaterialTheme.colorScheme.onSurface
            val backgroundColor = ButtonDefaults.filledTonalButtonColors().containerColor

            val heronPaths = remember(::HeronPaths)
            val progressState = logoMorphProgressState(isArrow = !isRootDestination)

            Canvas(
                modifier =
                    innerModifier.aspectRatio(
                        ratio = LogoViewportWidth / LogoViewportHeight,
                        matchHeightConstraintsFirst = true,
                    )
            ) {
                val progress = progressState.value

                // Leg: Rotates 0 -> -90
                val legRotation = -90f * progress

                // Leg: Translates (0,0) -> (4, -9)
                val legTx = 4f * progress
                val legTy = -9f * progress

                // Beak: Translates (0,0) -> (5, 12)
                val beakTx = 5f * progress
                val beakTy = 11f * progress

                // Top beak caret: Does not rotate
                val topCaretRot = 0f

                // Bottom beak caret: Rotates 0 -> 75
                val bottomCaretRot = 75f * progress

                // Head center approx (15, 3) -> Canvas Center (17.5, 24)
                val startX = 15f
                val startY = 3f
                val endX = 17.5f
                val endY = 24f

                val circleCenter =
                    Offset(
                        x = startX + (endX - startX) * progress,
                        y = startY + (endY - startY) * progress,
                    )
                val circleRadius = 24f * progress
                val headScale = 1f - (0.8f * progress)
                val headColor = lerp(start = HeadColor, stop = backgroundColor, fraction = progress)

                // Fade: 1.0 -> 0.0
                val fadeAlpha = 1f - progress

                scale(
                    scaleX = size.width / LogoViewportWidth,
                    scaleY = size.height / LogoViewportHeight,
                    pivot = Offset.Zero,
                ) {
                    // 1. Head: Simulate head morph to circle
                    if (progress > 0) {
                        drawCircle(color = headColor, radius = circleRadius, center = circleCenter)
                    }
                    // 2. Body & Head: Fade out and move
                    if (fadeAlpha > 0) {
                        drawPath(path = heronPaths.body, color = bodyColor, alpha = fadeAlpha)
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
                                    pivot = Offset(x = 15f, y = 3f),
                                )
                            },
                            drawBlock = {
                                drawPath(
                                    path = heronPaths.head,
                                    color = headColor,
                                    alpha = fadeAlpha,
                                )
                            },
                        )
                    }

                    // 3. Legs: Becomes the horizontal shaft
                    withTransform(
                        transformBlock = {
                            translate(left = legTx, top = legTy)
                            rotate(degrees = legRotation, pivot = Offset(x = 16.5f, y = 33.5f))
                        },
                        drawBlock = { drawPath(path = heronPaths.legs, color = bodyColor) },
                    )

                    // 4. Beak: Split into two instances for the Caret
                    val beakTipPivot = Offset(x = 0f, y = 12.1f)

                    // Instance A: Top Caret
                    withTransform(
                        transformBlock = {
                            translate(left = beakTx, top = beakTy)
                            rotate(degrees = topCaretRot, pivot = beakTipPivot)
                        },
                        drawBlock = { drawPath(path = heronPaths.beak, color = bodyColor) },
                    )

                    // Instance B: Bottom Caret
                    withTransform(
                        transformBlock = {
                            translate(left = beakTx, top = beakTy)
                            rotate(degrees = bottomCaretRot, pivot = beakTipPivot)
                        },
                        drawBlock = { drawPath(path = heronPaths.beak, color = bodyColor) },
                    )
                }
            }
        },
    )
}

@Composable
private fun logoMorphProgressState(isArrow: Boolean): State<Float> {
    val navigationEventDispatcher =
        LocalNavigationEventDispatcherOwner.current!!.navigationEventDispatcher

    val transitionState = navigationEventDispatcher.transitionState.collectAsState()

    return animateFloatAsState(
        targetValue =
            when (val value = transitionState.value) {
                NavigationEventTransitionState.Idle ->
                    if (isArrow) BackArrowProgress else HeronLogoProgress
                is NavigationEventTransitionState.InProgress ->
                    if (isArrow) BackArrowProgress
                    else BackArrowProgress - value.latestEvent.progress
            },
        label = "LogoAnimation",
    )
}

/** Class to hold the path definitions so they aren't reconstructed every frame. */
class HeronPaths {
    val beak: Path =
        Path().apply {
            fillType = PathFillType.EvenOdd
            moveTo(0f, 12.0766f)
            relativeLineTo(1.3261f, 1.7184f)
            relativeLineTo(9.8007f, -7.2465f)
            relativeLineTo(-1.8694f, -2.3891f)
            close()
        }

    val body: Path =
        Path().apply {
            fillType = PathFillType.EvenOdd
            moveTo(13.4049f, 14.2816f)
            lineTo(13.4119f, 14.2755f)
            cubicTo(13.41190f, 14.27550f, 20.63680f, 7.87230f, 20.94310f, 6.54070f)
            lineTo(16.4632f, 6.5407f)
            lineTo(16.4632f, 6.5407f)
            lineTo(16.458f, 6.5407f)
            lineTo(5.8086f, 15.615f)
            lineTo(5.8138f, 15.622f)
            cubicTo(4.00430f, 17.10430f, 2.06120f, 19.55940f, 2.06120f, 22.10450f)
            cubicTo(2.06120f, 25.91590f, 5.44320f, 28.92270f, 8.93890f, 29.77750f)
            lineTo(29.6347f, 37.4259f)
            lineTo(29.6365f, 37.4215f)
            lineTo(34.7143f, 39.6043f)
            cubicTo(30.70480f, 29.20030f, 22.97650f, 19.83350f, 13.40490f, 14.28160f)
            moveTo(12.8859f, 9.589f)
            lineTo(12.8859f, 9.589f)
            cubicTo(12.88510f, 9.58810f, 12.88420f, 9.58810f, 12.88420f, 9.58720f)
            cubicTo(12.88420f, 9.58810f, 12.88510f, 9.58810f, 12.88590f, 9.5890f)
            moveTo(8.6846f, 25.4709f)
            cubicTo(7.68230f, 24.89450f, 6.94720f, 23.86080f, 6.72420f, 22.71340f)
            cubicTo(6.50110f, 21.56690f, 6.79530f, 20.3270f, 7.5070f, 19.40790f)
            cubicTo(8.2360f, 18.46580f, 8.91290f, 17.86480f, 10.22420f, 17.49910f)
            cubicTo(16.88330f, 21.07340f, 23.36790f, 26.84830f, 27.45810f, 33.15710f)
            cubicTo(27.45810f, 33.15710f, 9.11340f, 25.71760f, 8.68460f, 25.47090f)
        }

    val head: Path =
        Path().apply {
            fillType = PathFillType.EvenOdd
            moveTo(11.1272f, 6.5483f)
            lineTo(16.4585f, 6.5404f)
            lineTo(20.9436f, 6.5404f)
            cubicTo(21.27770f, 4.64660f, 20.60420f, 2.19050f, 19.09240f, 1.03610f)
            cubicTo(17.70120f, -0.02670f, 15.75990f, -0.29810f, 14.13780f, 0.34340f)
            cubicTo(12.25280f, 1.0890f, 10.77490f, 2.77830f, 9.2570f, 4.15920f)
            lineTo(11.1272f, 6.5483f)
            close()
        }

    val legs: Path =
        Path().apply {
            fillType = PathFillType.EvenOdd
            moveTo(14.625f, 47.9987f)
            relativeLineTo(3.6285f, 0f)
            relativeLineTo(0f, -28.8737f)
            relativeLineTo(-3.6285f, 0f)
            close()
        }
}

private object AppLogo

private val HeadColor = Color(color = 0xFF607B8B)
private const val LogoViewportWidth = 35f
private const val LogoViewportHeight = 48f

private const val BackArrowProgress = 1f
private const val HeronLogoProgress = 0f
