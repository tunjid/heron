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
import com.tunjid.heron.data.repository.records.StandardPublicationDocumentsQuery
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.sharedElementPrefix
import com.tunjid.heron.standard.publication.di.PublicationRequest
import com.tunjid.heron.standard.publication.di.publicationRequest
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.ui.text.Memo
import com.tunjid.treenav.strings.Route
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val sharedElementPrefix: String? = null,
    @Transient
    val publication: StandardPublication? = null,
    @Transient
    override val tilingData: TilingState.Data<StandardPublicationDocumentsQuery, StandardDocument> = TilingState.Data(
        currentQuery = StandardPublicationDocumentsQuery(
            publicationUri = StandardPublicationUri(""),
            data = CursorQuery.Data(
                page = 0,
                cursorAnchor = Clock.System.now(),
            ),
        ),
    ),
    @Transient
    val messages: List<Memo> = emptyList(),
) : TilingState<StandardPublicationDocumentsQuery, StandardDocument> {
    val isRefreshing: Boolean
        get() = tilingData.status is TilingState.Status.Refreshing
}

fun State(
    route: Route,
) = State(
    sharedElementPrefix = route.sharedElementPrefix,
    tilingData = TilingState.Data(
        currentQuery = StandardPublicationDocumentsQuery(
            publicationUri = when (val request = route.publicationRequest) {
                is PublicationRequest.WithUri -> request.uri
                // Stub URI
                is PublicationRequest.WithProfile -> StandardPublicationUri("")
            },
            data = CursorQuery.Data(
                page = 0,
                cursorAnchor = Clock.System.now(),
            ),
        ),
    ),
)

sealed class Action(val key: String) {

    data class Tile(
        val tilingAction: TilingState.Action,
    ) : Action("Tile")

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

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
