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

package com.tunjid.heron.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import com.tunjid.heron.conversation.di.Route as ConversationRoute
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.repository.MessageQuery
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.ui.preview.RoutePreview
import com.tunjid.mutator.coroutines.asNoOpActionSuspendingStateMutator
import com.tunjid.treenav.strings.routeOf
import kotlin.time.Instant

@Preview
@Composable
internal fun ConversationRoutePreview() {
    val scope = rememberCoroutineScope()
    val conversationId = ConversationId("conv123")
    RoutePreview(
        route = routeOf(path = "/messages/conv123"),
        routeStateHolder = remember(scope) {
            ActualConversationViewModel(
                mutator = State.Immutable(
                    sharedElementPrefix = "preview",
                    id = conversationId,
                    tilingData = TilingState.Data(
                        currentQuery = MessageQuery(
                            conversationId = conversationId,
                            data = CursorQuery.Data(
                                page = 0,
                                cursorAnchor = Instant.DISTANT_PAST,
                            ),
                        ),
                    ),
                ).asNoOpActionSuspendingStateMutator(),
                scope = scope,
            )
        },
        render = { route, paneScaffoldState ->
            ConversationRoute(
                route = route,
                paneScaffoldState = paneScaffoldState,
            )
        },
    )
}
