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

package com.tunjid.heron.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import com.tunjid.heron.compose.di.Route as ComposeRoute
import com.tunjid.heron.compose.drafts.DraftsAction
import com.tunjid.heron.compose.drafts.DraftsState
import com.tunjid.heron.compose.drafts.DraftsStateHolder
import com.tunjid.heron.ui.preview.RoutePreview
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.asNoOpActionSuspendingStateMutator
import com.tunjid.treenav.strings.routeOf

@Preview
@Composable
internal fun ComposePreview() {
    val scope = rememberCoroutineScope()
    RoutePreview(
        route = routeOf(path = "/compose"),
        routeStateHolder = remember(scope) {
            ActualComposeViewModel(
                mutator = State.Immutable(
                    sharedElementPrefix = null,
                ).asNoOpActionSuspendingStateMutator(),
                scope = scope,
            )
        },
        // The drafts sheet's state holder is DI-provided at runtime; supply a no-op stub so the
        // preview can host it.
        additionalSheetStateHolderFactory = { type ->
            if (type == DraftsStateHolder::class) object :
                DraftsStateHolder,
                ActionSuspendingStateMutator<DraftsAction, DraftsState>
                by DraftsState.Immutable().asNoOpActionSuspendingStateMutator() {}
            else null
        },
        render = { route, paneScaffoldState ->
            ComposeRoute(
                route = route,
                paneScaffoldState = paneScaffoldState,
            )
        },
    )
}
