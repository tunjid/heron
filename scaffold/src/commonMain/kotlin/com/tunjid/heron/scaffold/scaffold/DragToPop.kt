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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollable2D
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

/**
 * State class for handling drag-to-pop gestures.
 *
 * This class manages the state of a drag gesture intended to pop the backstack. It coordinates
 * gesture inputs, animations, and navigation events.
 */
@Stable
class DragToPopState private constructor(
    private val scope: CoroutineScope,
    private val input: DirectNavigationEventInput,
    private val dismissThresholdSquared: () -> Float,
    private val shouldDragToPop: DragToPopState.(Offset) -> Boolean,
) {

    /**
     * An identifier for a given drag gesture. It stays the same from the first pointer down
     * till all pointers are up.
     */
    var gestureId by mutableIntStateOf(0)
        private set

    /**
     * Whether a drag-to-pop gesture is currently in progress.
     */
    var isDraggingToPop by mutableStateOf(false)
        private set

    private val channel = Channel<NavigationEventStatus>(Channel.UNLIMITED)
    private var dismissOffset by mutableStateOf(Offset.Zero)

    private var resetAnimationJob: Job? = null

    private val scrollable2DState = Scrollable2DState(::dispatchDelta)

    private val flingBehavior = object : FlingBehavior {
        override suspend fun ScrollScope.performFling(
            initialVelocity: Float,
        ): Float {
            onDragStopped()
            return 0f
        }
    }

    private fun dispatchDelta(delta: Offset): Offset {
        if (!shouldDragToPop(delta)) return Offset.Zero

        if (!isDraggingToPop) {
            isDraggingToPop = true
            // Add delta as isDraggingToPop is only just being set
            dismissOffset += delta
            resetAnimationJob?.cancel()
            channel.trySend(NavigationEventStatus.Seeking)
        }
        // Consume the delta
        return delta
    }

    private fun onDragStopped() {
        if (!isDraggingToPop) return
        isDraggingToPop = false
        gestureId = 0

        if (dismissOffset.getDistanceSquared() > dismissThresholdSquared()) {
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

                        snapshotFlow(::dismissOffset).collectLatest { currentOffset ->
                            input.backProgressed(
                                navigationEvent(
                                    min(
                                        a = currentOffset.getDistanceSquared() / dismissThresholdSquared(),
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
        /**
         * Modifier that adds drag-to-pop behavior to a component.
         *
         * This modifier uses the provided [DragToPopState] to listen for drag gestures and
         * translate the content accordingly. It works by monitoring pointer input and updating
         * the state's offset.
         *
         * @param state The [DragToPopState] that manages the drag gesture and navigation events.
         */
        fun Modifier.dragToPop(
            state: DragToPopState,
        ): Modifier = scrollable2D(
            state = state.scrollable2DState,
            flingBehavior = state.flingBehavior,
        )
            // Observe the pointer independently of the
            // scroll gesture without consuming it
            // bc the nested scroll child may lock scroll in one axis.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    val pointerId = down.id
                    state.gestureId = down.hashCode()

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val change = event.changes.fastFirstOrNull { it.id == pointerId }

                        if (change == null || !change.pressed) break

                        val delta = change.position - change.previousPosition

                        if (delta != Offset.Zero && state.isDraggingToPop) {
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

        /**
         * Creates and remembers a [DragToPopState].
         *
         * This function initializes the state required for drag-to-pop functionality. It hooks into
         * the [LocalNavigationEventDispatcherOwner] to report navigation events.
         *
         * @param dismissThreshold The distance (in Dp) the user must drag to trigger the pop action.
         * @param shouldDragToPop A lambda that determines if a drag gesture should initiate the pop action.
         * It receives the drag delta as an [Offset] and returns a Boolean.
         * This lambda is a receiver of [DragToPopState], allowing access to state properties
         * like [DragToPopState.isDraggingToPop] for more sophisticated gesture processing.
         */
        @Composable
        fun rememberDragToPopState(
            dismissThreshold: Dp = 200.dp,
            shouldDragToPop: DragToPopState.(Offset) -> Boolean,
        ): DragToPopState {
            val updatedDragToPop = rememberUpdatedState(shouldDragToPop)
            val floatDismissThreshold = rememberUpdatedState(
                with(LocalDensity.current) {
                    dismissThreshold.toPx().let { it * it }
                },
            )

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
            ) {
                DragToPopState(
                    scope = scope,
                    input = input,
                    dismissThresholdSquared = floatDismissThreshold::value,
                    shouldDragToPop = { delta ->
                        updatedDragToPop.value(this, delta)
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
