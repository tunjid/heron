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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.tunjid.composables.dragtodismiss.DragToDismissState
import com.tunjid.composables.dragtodismiss.dragToDismiss
import com.tunjid.treenav.compose.PanedNavHostScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.strings.Route

@Stable
class DragToPopState {
    var isDraggingToPop by mutableStateOf(false)
    internal val dragToDismissState = DragToDismissState(
        enabled = false,
    )
}

@Composable
fun Modifier.dragToPop(): Modifier {
    val state = LocalAppState.current.dragToPopState
    DisposableEffect(state) {
        state.dragToDismissState.enabled = true
        onDispose { state.dragToDismissState.enabled = false }
    }
    // TODO: This should not be necessary. Figure out why a frame renders with
    //  an offset of zero while the content in the transient primary container
    //  is still visible.
    val dragToDismissOffset by rememberUpdatedStateIf(
        value = state.dragToDismissState.offset.round(),
        predicate = {
            it != IntOffset.Zero
        }
    )
    return offset { dragToDismissOffset }
}

@Composable
internal fun PanedNavHostScope<ThreePane, Route>.DragToPopLayout(
    state: AppState,
    pane: ThreePane,
) {
    // Only place the DragToDismiss Modifier on the Primary pane
    if (pane == ThreePane.Primary) {
        Box(
            modifier = Modifier.dragToPopInternal(state)
        ) {
            Destination(pane)
        }
        // Place the transient primary screen above  the primary
        Destination(ThreePane.TransientPrimary)
    } else {
        Destination(pane)
    }
}

@Composable
private fun Modifier.dragToPopInternal(state: AppState): Modifier {
    val density = LocalDensity.current
    val dismissThreshold = remember { with(density) { 200.dp.toPx().let { it * it } } }

    return dragToDismiss(
        state = state.dragToPopState.dragToDismissState,
        dragThresholdCheck = { offset, _ ->
            offset.getDistanceSquared() > dismissThreshold
        },
        // Enable back preview
        onStart = {
            state.dragToPopState.isDraggingToPop = true
        },
        onCancelled = {
            // Dismiss back preview
            state.dragToPopState.isDraggingToPop = false
        },
        onDismissed = {
            // Dismiss back preview
            state.dragToPopState.isDraggingToPop = false

            // Pop navigation
            state.pop()
        }
    )
}
