package com.tunjid.heron.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset

@Composable
fun <T> AnimatedVerticallySlidingContent(
    modifier: Modifier = Modifier,
    targetState: T,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        modifier = modifier,
        targetState = targetState,
        transitionSpec = {
            ContentTransform
        },
    ) { currentState ->
        content(currentState)
    }
}

private const val TransitionDurationMillis = 600
private val TransitionAnimationSpec = tween<IntOffset>(TransitionDurationMillis)

private val ContentTransform = slideInVertically(
    animationSpec = TransitionAnimationSpec,
    initialOffsetY = { it },
) togetherWith slideOutVertically(
    animationSpec = TransitionAnimationSpec,
    targetOffsetY = { -it },
)
