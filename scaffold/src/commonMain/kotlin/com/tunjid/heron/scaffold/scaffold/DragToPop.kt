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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.navigationevent.NavigationEvent
import com.tunjid.composables.dragtodismiss.DragToDismissState
import com.tunjid.composables.dragtodismiss.dragToDismiss
import com.tunjid.composables.dragtodismiss.rememberUpdatedDragToDismissState
import com.tunjid.treenav.compose.navigation3.ui.LocalNavigationEventDispatcherOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.min

@Composable
fun Modifier.dragToPop(): Modifier {
    val appState = LocalAppState.current
    val density = LocalDensity.current

    val dismissThreshold = remember(density) {
        with(density) { 200.dp.toPx().let { it * it } }
    }

    val dragToDismissState = rememberUpdatedDragToDismissState()

    val dispatcher = checkNotNull(
        LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    )

    LaunchedEffect(appState.dismissBehavior) {
        val offsetFlow = when (appState.dismissBehavior) {
            AppState.DismissBehavior.Gesture.Drag -> snapshotFlow(dragToDismissState::offset)
            AppState.DismissBehavior.Gesture.Slide,
            AppState.DismissBehavior.None -> null
        } ?: return@LaunchedEffect

        delay(10)
        dispatcher.dispatchOnStarted(
            dragToDismissState.navigationEvent(progress = 0f)
        )
        offsetFlow.collectLatest { offset ->
            dispatcher.dispatchOnProgressed(
                dragToDismissState.navigationEvent(
                    min(
                        a = 1f,
                        b = offset.getDistanceSquared() / dismissThreshold,
                    )
                )
            )
        }
    }

    return dragToDismiss(
        state = dragToDismissState,
        shouldDismiss = { offset, _ ->
            offset.getDistanceSquared() > dismissThreshold
        },
        // Enable back preview
        onStart = {
            appState.dismissBehavior = AppState.DismissBehavior.Gesture.Drag
        },
        onCancelled = cancelled@{ hasResetOffset ->
            if (hasResetOffset) return@cancelled

            // Dismiss back preview
            appState.dismissBehavior = AppState.DismissBehavior.None
            dispatcher.dispatchOnCancelled()
        },
        onDismissed = {
            // Dismiss back preview
            appState.dismissBehavior = AppState.DismissBehavior.None

            // Pop navigation
            dispatcher.dispatchOnCompleted()
        }
    )
        .offset { dragToDismissState.offset.round() }
}

private fun DragToDismissState.navigationEvent(
    progress: Float
) = NavigationEvent(
    touchX = offset.x,
    touchY = offset.y,
    progress = progress,
    swipeEdge = NavigationEvent.EDGE_LEFT,
)

