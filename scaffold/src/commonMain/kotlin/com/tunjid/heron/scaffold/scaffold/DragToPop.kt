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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.navigationevent.NavigationEvent
import com.tunjid.composables.dragtodismiss.DragToDismissState
import com.tunjid.composables.dragtodismiss.dragToDismiss
import com.tunjid.treenav.compose.navigation3.ui.LocalNavigationEventDispatcherOwner
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.min

@Stable
internal class DragToPopState {
    var isDraggingToPop by mutableStateOf(false)
    internal val dragToDismissState = DragToDismissState(
        enabled = false,
    )
}

@Composable
fun Modifier.dragToPop(): Modifier {
    val state = LocalAppState.current
    val dragToPopState = state.dragToPopState

    val density = LocalDensity.current
    val dismissThreshold = remember { with(density) { 200.dp.toPx().let { it * it } } }

    val dispatcher = checkNotNull(
        LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    )
    LaunchedEffect(Unit) {
        snapshotFlow {
            state.dragToPopState.dragToDismissState.offset
        }
            .collectLatest {
                if (state.dragToPopState.isDraggingToPop) {
                    dispatcher.dispatchOnProgressed(
                        state.dragToPopState.dragToDismissState.navigationEvent(
                            min(
                                a = 1f,
                                b = state.dragToPopState.dragToDismissState.offset.getDistanceSquared() / dismissThreshold,
                            )
                        )
                    )
                }
            }
    }

    DisposableEffect(dragToPopState) {
        dragToPopState.dragToDismissState.enabled = true
        onDispose { dragToPopState.dragToDismissState.enabled = false }
    }
    // TODO: This should not be necessary. Figure out why a frame renders with
    //  an offset of zero while the content in the transient primary container
    //  is still visible.
    val dragToDismissOffset by rememberUpdatedStateIf(
        value = dragToPopState.dragToDismissState.offset.round(),
        predicate = {
            it != IntOffset.Zero
        }
    )

    return dragToDismiss(
        state = state.dragToPopState.dragToDismissState,
        dragThresholdCheck = { offset, _ ->
            offset.getDistanceSquared() > dismissThreshold
        },
        // Enable back preview
        onStart = {
            state.dragToPopState.isDraggingToPop = true
            dispatcher.dispatchOnStarted(
                state.dragToPopState.dragToDismissState.navigationEvent(0f)
            )
        },
        onCancelled = {
            // Dismiss back preview
            state.dragToPopState.isDraggingToPop = false
            dispatcher.dispatchOnCancelled()
        },
        onDismissed = {
            // Dismiss back preview
            state.dragToPopState.isDraggingToPop = false

            // Pop navigation
            dispatcher.dispatchOnCompleted()
        }
    )
        .offset { dragToDismissOffset }
}

private fun DragToDismissState.navigationEvent(
    progress: Float
) = NavigationEvent(
    touchX = offset.x,
    touchY = offset.y,
    progress = progress,
    swipeEdge = NavigationEvent.EDGE_LEFT,
)

