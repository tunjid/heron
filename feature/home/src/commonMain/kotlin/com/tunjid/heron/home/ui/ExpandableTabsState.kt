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

import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import com.tunjid.heron.home.TabLayout
import com.tunjid.heron.ui.UiTokens
import kotlinx.coroutines.flow.collectLatest

class ExpandableTabsState(
    private val transitionState: SeekableTransitionState<Boolean>,
    val transition: Transition<Boolean>,
    maxOffset: Float,
) {

    private val interactionSource = MutableInteractionSource()
    private val draggableState = AnchoredDraggableState(
        initialValue = transitionState.currentState,
        anchors = maxOffset.anchors(),
    )

    private val maxOffset
        get() = draggableState.anchors.maxPosition()

    val isExpanded
        get() = draggableState.currentValue

    val expansionProgress: Float
        get() = with(draggableState) {
            requireOffset() / maxOffset
        }

    private suspend fun animateTo(isExpanded: Boolean) {
        draggableState.animateTo(isExpanded)
    }

    private fun updateAnchors(
        maxOffset: Float,
    ) {
        if (maxOffset == this.maxOffset) return
        draggableState.updateAnchors(maxOffset.anchors())
    }

    private suspend fun onOffsetChanged(
        dragOffset: Float,
    ) = when (dragOffset) {
        0f -> {
            transitionState.snapTo(targetState = false)
        }
        maxOffset -> {
            transitionState.snapTo(targetState = true)
        }
        else -> {
            val isExpanding = !draggableState.settledValue

            val heightFraction = dragOffset / maxOffset
            val progress = if (isExpanding) heightFraction else 1f - heightFraction

            transitionState.seekTo(
                fraction = progress,
                targetState = isExpanding,
            )
        }
    }

    companion object Companion {
        @Composable
        fun rememberExpandableTabsState(
            tabLayout: TabLayout,
            onTabLayoutChanged: (TabLayout) -> Unit,
        ): ExpandableTabsState {
            val seekingState = remember { SeekableTransitionState(tabLayout.isExpanded) }
            val transition = rememberTransition(seekingState)

            val windowInfo = LocalWindowInfo.current
            val density = LocalDensity.current

            val statusBarHeight = UiTokens.statusBarHeight
            val toolbarHeight = UiTokens.toolbarHeight
            val tabsHeight = UiTokens.tabsHeight

            val maxOffset = remember(
                windowInfo,
                density,
                statusBarHeight + toolbarHeight + tabsHeight,
            ) {
                windowInfo.containerSize.height - with(density) {
                    statusBarHeight.toPx() + toolbarHeight.toPx() + tabsHeight.toPx()
                }
            }

            val state = remember(seekingState, transition) {
                ExpandableTabsState(seekingState, transition, maxOffset)
            }.also { it.updateAnchors(maxOffset) }

            LaunchedEffect(state) {
                snapshotFlow { state.draggableState.requireOffset() }
                    .collectLatest(state::onOffsetChanged)
            }

            val updatedOnTabLayoutChanged = rememberUpdatedState(onTabLayoutChanged)
            LaunchedEffect(state) {
                snapshotFlow { state.draggableState.settledValue }
                    .collectLatest { isExpanded ->
                        updatedOnTabLayoutChanged.value(
                            if (isExpanded) TabLayout.Expanded
                            else TabLayout.Collapsed.All,
                        )
                    }
            }

            LaunchedEffect(tabLayout) {
                state.animateTo(tabLayout.isExpanded)
            }

            return state
        }

        fun Modifier.expandable(
            state: ExpandableTabsState,
        ) = anchoredDraggable(
            state = state.draggableState,
            orientation = Orientation.Vertical,
            interactionSource = state.interactionSource,
        )

        fun <T> animationSpec(
            visibilityThreshold: T,
        ) = spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = visibilityThreshold,
        )
    }
}

private val TabLayout.isExpanded
    get() = this is TabLayout.Expanded

private fun Float.anchors() = DraggableAnchors {
    false at 0f
    true at this@anchors
}
