/*
 *    Copyright 2026 Adetunji Dahunsi
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

package com.tunjid.heron.messages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import com.tunjid.heron.messages.di.Route as MessagesRoute
import com.tunjid.heron.ui.preview.RoutePreview
import com.tunjid.mutator.coroutines.asNoOpActionSuspendingStateMutator
import com.tunjid.treenav.strings.routeOf

@Preview
@Composable
internal fun MessagesPreview() {
    RoutePreview(
        route = routeOf(path = "/messages"),
        routeStateHolder = ActualMessagesViewModel(
            mutator = State.Immutable().asNoOpActionSuspendingStateMutator(),
            scope = rememberCoroutineScope(),
        ),
        render = { route, paneScaffoldState ->
            MessagesRoute(
                route = route,
                paneScaffoldState = paneScaffoldState,
            )
        },
    )
}
