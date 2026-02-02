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
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.Scrollable2DState
import androidx.compose.foundation.gestures.rememberScrollable2DState
import androidx.compose.foundation.gestures.scrollable2D
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.tunjid.treenav.compose.NavigationEventStatus
import kotlin.math.min
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

@Stable
class DragToPop2State private constructor(
    private val enabled: (Offset) -> Boolean,
    private val dismissThresholdSquared: Float,
    private val input: DirectNavigationEventInput,
) {

    var offset by mutableStateOf(Offset.Zero)
        private set

    private var dismissOffset by mutableStateOf<IntOffset?>(null)
    private var isSeeking = false

    private val channel = Channel<NavigationEventStatus>(Channel.UNLIMITED)

    private val scrollable2DState = Scrollable2DState(::dispatchDelta)

    private val flingBehavior = object : FlingBehavior {
        override suspend fun ScrollScope.performFling(
            initialVelocity: Float,
        ): Float {
            onDragStopped()
            return 0f
        }
    }

    internal fun dispatchDelta(delta: Offset): Offset {
        if (!enabled(delta)) return Offset.Zero

        if (!isSeeking) {
            isSeeking = true
            channel.trySend(NavigationEventStatus.Seeking)
        }
        offset += delta
        return delta
    }

    internal suspend fun onDragStopped() {
        if (!isSeeking) return
        isSeeking = false

        if (offset.getDistanceSquared() > dismissThresholdSquared) {
            dismissOffset = offset.round()
            channel.trySend(NavigationEventStatus.Completed.Commited)
        } else {
            channel.trySend(NavigationEventStatus.Completed.Cancelled)
            Animatable(offset, Offset.VectorConverter).animateTo(Offset.Zero) {
                offset = value
            }
        }
    }

    suspend fun awaitEvents() {
        channel.consumeAsFlow()
            .collectLatest { status ->
                when (status) {
                    NavigationEventStatus.Completed.Cancelled -> {
                        input.backCancelled()
                    }

                    NavigationEventStatus.Completed.Commited -> {
                        input.backCompleted()
                    }

                    NavigationEventStatus.Seeking -> {
                        input.backStarted(navigationEvent(progress = 0f))

                        snapshotFlow { offset }.collectLatest { currentOffset ->
                            input.backProgressed(
                                navigationEvent(
                                    min(
                                        a = currentOffset.getDistanceSquared() / dismissThresholdSquared,
                                        b = 1f,
                                    ),
                                ),
                            )
                        }
                    }
                }
            }
    }

    private fun navigationEvent(
        progress: Float,
    ) = NavigationEvent(
        touchX = offset.x,
        touchY = offset.y,
        progress = progress,
        swipeEdge = NavigationEvent.EDGE_NONE,
    )

    companion object {
        fun Modifier.dragToPop2(
            state: DragToPop2State,
        ): Modifier = scrollable2D(
            state = state.scrollable2DState,
            flingBehavior = state.flingBehavior,
        )
            .offset {
                state.dismissOffset ?: state.offset.round()
            }

        @Composable
        fun rememberDragToPop2State(
            dismissThreshold: Dp = 200.dp,
            enabled: (Offset) -> Boolean,
        ): DragToPop2State {
            val updatedEnabled = rememberUpdatedState(enabled)
            val floatDismissThreshold = with(LocalDensity.current) {
                dismissThreshold.toPx().let { it * it }
            }

            val dispatcher = checkNotNull(
                LocalNavigationEventDispatcherOwner.current
                    ?.navigationEventDispatcher,
            )
            val input = remember(dispatcher) {
                DirectNavigationEventInput()
            }

            DisposableEffect(dispatcher) {
                dispatcher.addInput(input)
                onDispose {
                    dispatcher.removeInput(input)
                }
            }

            val dragToPopState = remember(input, floatDismissThreshold) {
                DragToPop2State(
                    dismissThresholdSquared = floatDismissThreshold,
                    input = input,
                    enabled = {
                        updatedEnabled.value(it)
                    },
                )
            }

            LaunchedEffect(dragToPopState) {
                dragToPopState.awaitEvents()
            }

            return dragToPopState
        }
    }
}
