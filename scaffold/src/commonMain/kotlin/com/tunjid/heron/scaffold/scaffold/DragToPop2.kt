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
import androidx.compose.foundation.gestures.Scrollable2DState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.util.fastFirstOrNull
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.tunjid.treenav.compose.NavigationEventStatus
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch

@Stable
class DragToPop2State private constructor(
    private val enabled: (Offset) -> Boolean,
    private val dismissThresholdSquared: Float,
    private val scope: CoroutineScope,
    private val input: DirectNavigationEventInput,
) {

    private var dismissOffset by mutableStateOf(Offset.Zero)
    private var isSeeking = false

    private var resetAnimationJob: Job? = null

    private val channel = Channel<NavigationEventStatus>(Channel.UNLIMITED)

    private val scrollable2DState = Scrollable2DState(::dispatchDelta)

    private fun dispatchDelta(delta: Offset): Offset {
        if (!enabled(delta)) return Offset.Zero

        if (!isSeeking) {
            isSeeking = true
            resetAnimationJob?.cancel()
            channel.trySend(NavigationEventStatus.Seeking)
        }
        return delta
    }

    private fun onDragStopped() {
        if (!isSeeking) return
        isSeeking = false

        if (dismissOffset.getDistanceSquared() > dismissThresholdSquared) {
            channel.trySend(NavigationEventStatus.Completed.Commited)
        } else {
            channel.trySend(NavigationEventStatus.Completed.Cancelled)

            resetAnimationJob = scope.launch {
                Animatable(
                    initialValue = dismissOffset,
                    typeConverter = Offset.VectorConverter,
                ).animateTo(Offset.Zero) {
                    dismissOffset = value
                }
            }
        }
    }

    private suspend fun awaitEvents() {
        channel.consumeAsFlow()
            .transformWhile { status ->
                emit(status)
                status != NavigationEventStatus.Completed.Commited
            }
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

                        snapshotFlow { dismissOffset }.collectLatest { currentOffset ->
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
        touchX = dismissOffset.x,
        touchY = dismissOffset.y,
        progress = progress,
        swipeEdge = NavigationEvent.EDGE_NONE,
    )

    companion object {
        fun Modifier.dragToPop2(
            state: DragToPop2State,
        ): Modifier = scrollable2D(
            state = state.scrollable2DState,
        )
            // Observe the pointer independently of the
            // scroll gesture without consuming it
            // bc the nested scroll child may scroll in 1D.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    val pointerId = down.id

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val change = event.changes.fastFirstOrNull { it.id == pointerId }

                        if (change == null || !change.pressed) break

                        val delta = change.position - change.previousPosition

                        if (delta != Offset.Zero && state.isSeeking) {
                            state.dismissOffset += delta
                        }
                    }

                    // Pointer up, gesture cancelled
                    state.onDragStopped()
                }
            }
            .offset {
                state.dismissOffset.round()
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
            val scope = rememberCoroutineScope()
            val input = remember(dispatcher) {
                DirectNavigationEventInput()
            }

            DisposableEffect(dispatcher) {
                dispatcher.addInput(input)
                onDispose {
                    dispatcher.removeInput(input)
                }
            }

            val dragToPopState = remember(
                key1 = input,
                key2 = scope,
                key3 = floatDismissThreshold,
            ) {
                DragToPop2State(
                    dismissThresholdSquared = floatDismissThreshold,
                    scope = scope,
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
