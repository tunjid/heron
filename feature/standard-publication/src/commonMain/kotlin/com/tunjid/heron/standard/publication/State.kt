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

package com.tunjid.heron.standard.publication

import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.StandardSubscriptionUri
import com.tunjid.heron.data.repository.records.StandardPublicationDocumentsQuery
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.heron.scaffold.navigation.sharedElementPrefix
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.treenav.strings.Route
import kotlin.time.Clock
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val sharedElementPrefix: String? = null,
    @Transient
    val publication: StandardPublication? = null,
    @Transient
    val documentsTilingStateHolder: DocumentsStateHolder? = null,
    @Transient
    val messages: List<Memo> = emptyList(),
)

val State.isRefreshing
    get() = documentsTilingStateHolder
        ?.state
        ?.value
        ?.tilingData
        ?.status is TilingState.Status.Refreshing

fun State(
    route: Route,
): State = State(
    publication = route.model<StandardPublication>(),
    sharedElementPrefix = route.sharedElementPrefix,
)

typealias DocumentsStateHolder = ActionStateMutator<TilingState.Action, StateFlow<DocumentsTilingState>>

@Serializable
data class DocumentsTilingState(
    val publicationUri: StandardPublicationUri,
    @Transient
    override val tilingData: TilingState.Data<StandardPublicationDocumentsQuery, StandardDocument> = TilingState.Data(
        currentQuery = StandardPublicationDocumentsQuery(
            publicationUri = publicationUri,
            data = CursorQuery.Data(
                page = 0,
                cursorAnchor = Clock.System.now(),
            ),
        ),
    ),
) : TilingState<StandardPublicationDocumentsQuery, StandardDocument>

sealed class Action(val key: String) {

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

    sealed class TogglePublicationSubscription : Action(key = "TogglePublicationSubscription") {
        data class Subscribe(
            val publicationUri: StandardPublicationUri,
        ) : TogglePublicationSubscription()

        data class Unsubscribe(
            val subscriptionUri: StandardSubscriptionUri,
        ) : TogglePublicationSubscription()
    }

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop

        data class To(
            val delegate: NavigationAction.Destination,
        ) : Navigate(),
            NavigationAction by delegate
    }
}
