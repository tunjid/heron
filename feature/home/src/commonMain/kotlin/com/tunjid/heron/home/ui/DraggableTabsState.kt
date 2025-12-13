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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import com.tunjid.heron.home.TabLayout
import com.tunjid.heron.ui.UiTokens
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class DraggableTabsState(
    private val transitionState: SeekableTransitionState<Boolean>,
    val transition: Transition<Boolean>,
    maxHeight: Float,
) {
    private var status by mutableStateOf<Status>(Status.Idling)
    private var maxHeight by mutableFloatStateOf(maxHeight)
    private var height by mutableFloatStateOf(MinHeight)

    private val draggableState = DraggableState {
        height = (height + it).restrictTo(min = MinHeight, max = maxHeight)
    }

    val targetState
        get() = transitionState.targetState

    val expansionProgress: Float
        get() = when (val isExpanding = transitionState.targetState) {
            transitionState.currentState -> if (isExpanding) FullyExpanded else FullyCollapsed
            else -> if (isExpanding) transitionState.fraction else 1f - transitionState.fraction
        }

    private suspend fun animateTo(tabLayout: TabLayout) {
        status = Status.Resetting
        println("Animating to $tabLayout")
        transitionState.animateTo(
            targetState = tabLayout.isExpanded,
            animationSpec = ProgressAnimationSpec,
        )
        height = if (tabLayout.isExpanded) maxHeight else MinHeight
        status = Status.Idling
    }

    companion object Companion {
        @Composable
        fun rememberDragToExpandState(
            tabLayout: TabLayout,
        ): DraggableTabsState {
            val seekingState = remember { SeekableTransitionState(tabLayout.isExpanded) }
            val transition = rememberTransition(seekingState)

            val windowInfo = LocalWindowInfo.current
            val density = LocalDensity.current

            val statusBarHeight = UiTokens.statusBarHeight
            val toolbarHeight = UiTokens.toolbarHeight
            val tabsHeight = UiTokens.tabsHeight

            val maxHeight =
                remember(windowInfo, density, statusBarHeight, toolbarHeight, tabsHeight) {
                    windowInfo.containerSize.height - with(density) {
                        statusBarHeight.toPx() + toolbarHeight.toPx() + tabsHeight.toPx()
                    }
                }

            val state = remember(seekingState, transition) {
                DraggableTabsState(seekingState, transition, maxHeight)
            }.also { it.maxHeight = maxHeight }

            LaunchedEffect(state) {
                snapshotFlow { state.height }
                    .filter { state.status.isExpandingOrNull != null }
                    .collectLatest { height ->
                        val isExpanding = state.status.isExpandingOrNull ?: return@collectLatest

                        val heightFraction = height / state.maxHeight
                        val progress = if (isExpanding) heightFraction else 1f - heightFraction

                        println("progress: $progress; e: $isExpanding")
                        if (progress > HeightChangeExpansionThreshold) state.animateTo(
                            tabLayout = if (isExpanding) TabLayout.Expanded else TabLayout.Collapsed.All,
                        )
                        else seekingState.seekTo(
                            fraction = progress,
                            targetState = isExpanding,
                        )
                    }
            }

            LaunchedEffect(tabLayout) {
                state.animateTo(tabLayout)
            }
            LaunchedEffect(state.status) {
                println("S: ${state.status}")
            }

            return state
        }

        fun Modifier.dragToExpand(
            state: DraggableTabsState,
        ) = drag(
            state = state,
            startStatus = Status.Dragging.Expanding,
        )

        fun Modifier.dragToCollapse(
            state: DraggableTabsState,
        ) = drag(
            state = state,
            startStatus = Status.Dragging.Collapsing,
        )

        private fun Modifier.drag(
            state: DraggableTabsState,
            startStatus: Status.Dragging,
        ) = draggable(
            state = state.draggableState,
            orientation = Orientation.Vertical,
            enabled = state.status.canDrag,
            onDragStarted = {
                state.status = startStatus
                state.transitionState.snapTo(startStatus !is Status.Dragging.Expanding)
            },
            onDragStopped = { velocity ->
                state.status = Status.Resetting
                val isExpanding = startStatus is Status.Dragging.Expanding
                val willExpand = state.expansionProgress > DragStopExpansionThreshold
                val target = if (willExpand == isExpanding) FullyExpanded else FullyCollapsed

                val animatable = Animatable(initialValue = state.transitionState.fraction)
                animatable.animateTo(
                    targetValue = target,
                    animationSpec = ProgressAnimationSpec,
                    initialVelocity = velocity,
                    block = {
                        launch {
                            state.transitionState.seekTo(
                                fraction = value.restrictTo(
                                    min = FullyCollapsed,
                                    max = FullyExpanded,
                                ),
                                targetState = isExpanding,
                            )
                        }
                    },
                )
                state.height = if (willExpand) state.maxHeight else MinHeight
                state.transitionState.snapTo(willExpand)
                state.status = Status.Idling
            },
        )

        fun <T> animationSpec(
            visibilityThreshold: T,
        ) = spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = visibilityThreshold,
        )

        private val ProgressAnimationSpec = animationSpec(visibilityThreshold = 0.05f)

        private fun Float.restrictTo(
            min: Float,
            max: Float,
        ) = max(
            a = min,
            b = min(this, max),
        )
    }
}

private sealed interface Status {
    sealed interface Dragging : Status {
        data object Expanding : Dragging
        data object Collapsing : Dragging
    }

    data object Idling : Status
    data object Resetting : Status
}

private val Status.isExpandingOrNull
    get() = when (this) {
        Status.Dragging.Collapsing -> false
        Status.Dragging.Expanding -> true
        Status.Idling,
        Status.Resetting,
            -> null
    }

private val Status.canDrag
    get() = when (this) {
        is Status.Dragging,
        Status.Idling,
            -> true
        Status.Resetting -> false
    }

private val TabLayout.isExpanded
    get() = this is TabLayout.Expanded

private const val FullyExpanded = 1f
private const val FullyCollapsed = 0f
private const val MinHeight = 0f
private const val DragStopExpansionThreshold = 0.3f
private const val HeightChangeExpansionThreshold = 0.8f
