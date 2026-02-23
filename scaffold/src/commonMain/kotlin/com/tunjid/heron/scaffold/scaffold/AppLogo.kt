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

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.modifiers.blurEffect
import com.tunjid.treenav.compose.UpdatedMovableSharedElementOf

@Composable
fun PaneScaffoldState.AppLogo(
    modifier: Modifier,
    presentation: LogoPresentation,
) {
    UpdatedMovableSharedElementOf(
        sharedContentState = rememberSharedContentState(AppLogo),
        zIndexInOverlay = UiTokens.navigationIconZIndex,
        clipInOverlayDuringTransition = NoPathOverlayClip,
        state = presentation,
        modifier = modifier,
        sharedElement = { currentPresentation, innerModifier ->
            // Keep this in composition so it transitions
            // smoothly from the splash animation
            val backLogoAnimation = rememberBackLogoAnimation(
                isArrow = currentPresentation is LogoPresentation.Destination.Poppable,
            )
            val animation = when (currentPresentation) {
                is LogoPresentation.Destination -> backLogoAnimation
                is LogoPresentation.Splash -> rememberSplashLogoAnimation()
            }
            Canvas(
                modifier = innerModifier
                    // Always draw logo in a square
                    .aspectRatio(1f)
                    .graphicsLayer {
                        if (currentPresentation !is LogoPresentation.Destination.Root) return@graphicsLayer

                        val animationProgress = backLogoAnimation.progress()
                        // Apply the blur until the transition stops
                        if (animationProgress == 0f && !isTransitionActive) return@graphicsLayer

                        blurEffect(
                            shape = CircleShape,
                            radius = UiTokens::appBarBlurRadius,
                            clip = { false },
                            progress = {
                                lerp(
                                    start = 0f,
                                    stop = currentPresentation.blurProgress(),
                                    fraction = 1f - animationProgress,
                                )
                            },
                        )
                    },
            ) {
                val scaleX = LogoAspectRatio * size.width / LogoViewportWidth
                val scaleY = size.height / LogoViewportHeight

                val width = size.width
                val actualWidth = LogoViewportWidth * scaleX

                translate(
                    left = (width - actualWidth) / 2,
                ) {
                    scale(
                        scaleX = scaleX,
                        scaleY = scaleY,
                        pivot = Offset.Zero,
                    ) {
                        HeronPart.entries.forEach { part ->
                            with(animation) {
                                drawPart(part)
                            }
                        }
                    }
                }
            }
        },
    )
}

@Stable
sealed class LogoPresentation {
    data object Splash : LogoPresentation()
    sealed class Destination : LogoPresentation() {
        @Stable
        class Root(
            val blurProgress: () -> Float = { 0f },
        ) : Destination()

        data object Poppable : Destination()
    }
}

internal sealed interface LogoAnimation {
    fun DrawScope.drawPart(
        part: HeronPart,
    )
}

/**
 * Class to hold the path definitions so they aren't reconstructed every frame.
 */
internal enum class HeronPart(
    val path: Path,
) {
    Head(
        path = Path().apply {
            fillType = PathFillType.EvenOdd
            moveTo(11.1272f, 6.5483f)
            lineTo(16.4585f, 6.5404f)
            lineTo(20.9436f, 6.5404f)
            cubicTo(21.27770f, 4.64660f, 20.60420f, 2.19050f, 19.09240f, 1.03610f)
            cubicTo(17.70120f, -0.02670f, 15.75990f, -0.29810f, 14.13780f, 0.34340f)
            cubicTo(12.25280f, 1.0890f, 10.77490f, 2.77830f, 9.2570f, 4.15920f)
            lineTo(11.1272f, 6.5483f)
            close()
        },
    ),
    Beak(
        path = Path().apply {
            fillType = PathFillType.EvenOdd
            moveTo(0f, 12.0766f)
            relativeLineTo(1.3261f, 1.7184f)
            relativeLineTo(9.8007f, -7.2465f)
            relativeLineTo(-1.8694f, -2.3891f)
            close()
        },
    ),
    Body(
        path = Path().apply {
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
        },
    ),
    Legs(
        path = Path().apply {
            fillType = PathFillType.EvenOdd
            moveTo(14.625f, 47.9987f)
            relativeLineTo(3.6285f, 0f)
            relativeLineTo(0f, -28.8737f)
            relativeLineTo(-3.6285f, 0f)
            close()
        },
    ),
}

private object AppLogo

private object NoPathOverlayClip : OverlayClip {
    override fun getClipPath(
        sharedContentState: SharedTransitionScope.SharedContentState,
        bounds: Rect,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Path? = null
}

internal val HeadColor = Color(color = 0xFF607B8B)
private const val LogoViewportWidth = 35f
private const val LogoViewportHeight = 48f

private const val LogoAspectRatio = LogoViewportWidth / LogoViewportHeight
