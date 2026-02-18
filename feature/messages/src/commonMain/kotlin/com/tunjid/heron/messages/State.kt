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

package com.tunjid.heron.messages

import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.repository.ConversationQuery
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.ui.text.Memo
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    @Transient val signedInProfile: Profile? = null,
    override val tilingData: TilingState.Data<ConversationQuery, Conversation> =
        TilingState.Data(
            currentQuery =
                ConversationQuery(
                    data = CursorQuery.Data(page = 0, cursorAnchor = Clock.System.now(), limit = 15)
                )
        ),
    @Transient val isSearching: Boolean = false,
    @Transient val searchQuery: String = "",
    @Transient val messages: List<Memo> = emptyList(),
    @Transient val autoCompletedProfiles: List<ProfileWithViewerState> = emptyList(),
) : TilingState<ConversationQuery, Conversation>

sealed class Action(val key: String) {

    data class Tile(val tilingAction: TilingState.Action) : Action(key = "Tile")

    data class SnackbarDismissed(val message: Memo) : Action(key = "SnackbarDismissed")

    data class SetIsSearching(val isSearching: Boolean) : Action(key = "SetIsSearching")

    data class SearchQueryChanged(val query: String) : Action(key = "SearchQueryChanged")

    data class ResolveConversation(val with: Profile) : Action(key = "ResolveConversation")

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data class To(val delegate: NavigationAction.Destination) :
            Navigate(), NavigationAction by delegate
    }
}

const val ConversationSearchResult = "ConversationSearchResult"
