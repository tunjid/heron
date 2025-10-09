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

import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.tunjid.composables.dragtodismiss.DragToDismissState
import com.tunjid.composables.dragtodismiss.dragToDismiss
import com.tunjid.composables.dragtodismiss.rememberUpdatedDragToDismissState
import com.tunjid.treenav.compose.NavigationEventStatus
import kotlin.math.min
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow

@Stable
class DragToPopState private constructor(
    private val dismissThresholdSquared: Float,
    private val dragToDismissState: DragToDismissState,
    private val input: DirectNavigationEventInput,
) {

    private val channel = Channel<NavigationEventStatus>()
    private var dismissOffset by mutableStateOf<IntOffset?>(null)

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
                        input.backStarted(dragToDismissState.navigationEvent(progress = 0f))

                        snapshotFlow(dragToDismissState::offset).collectLatest {
                            input.backProgressed(
                                dragToDismissState.navigationEvent(
                                    min(
                                        a = dragToDismissState.offset.getDistanceSquared() / dismissThresholdSquared,
                                        b = 1f,
                                    ),
                                ),
                            )
                        }
                    }
                }
            }
    }

    companion object {
        fun Modifier.dragToPop(
            dragToPopState: DragToPopState,
        ): Modifier = dragToDismiss(
            state = dragToPopState.dragToDismissState,
            shouldDismiss = { offset, _ ->
                offset.getDistanceSquared() > dragToPopState.dismissThresholdSquared
            },
            // Enable back preview
            onStart = {
                dragToPopState.channel.trySend(NavigationEventStatus.Seeking)
            },
            onCancelled = cancelled@{ hasResetOffset ->
                if (hasResetOffset) return@cancelled
                dragToPopState.channel.trySend(NavigationEventStatus.Completed.Cancelled)
            },
            onDismissed = {
                dragToPopState.dismissOffset = dragToPopState.dragToDismissState.offset.round()
                dragToPopState.channel.trySend(NavigationEventStatus.Completed.Commited)
            },
        )
            .offset {
                dragToPopState.dismissOffset ?: dragToPopState.dragToDismissState.offset.round()
            }

        @Composable
        fun rememberDragToPopState(
            dismissThreshold: Dp = 200.dp,
        ): DragToPopState {
            val floatDismissThreshold = with(LocalDensity.current) {
                dismissThreshold.toPx().let { it * it }
            }

            val dragToDismissState = rememberUpdatedDragToDismissState()

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

            val dragToPopState = remember(dragToDismissState, input) {
                DragToPopState(
                    dismissThresholdSquared = floatDismissThreshold,
                    dragToDismissState = dragToDismissState,
                    input = input,
                )
            }

            LaunchedEffect(dragToPopState) {
                dragToPopState.awaitEvents()
            }

            return dragToPopState
        }
    }
}

private fun DragToDismissState.navigationEvent(
    progress: Float,
) = NavigationEvent(
    touchX = offset.x,
    touchY = offset.y,
    progress = progress,
    swipeEdge = NavigationEvent.EDGE_NONE,
)
