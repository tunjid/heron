/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.search

import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.SearchResult
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.treenav.pop
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

enum class ScreenLayout {
    Trends,
    AutoCompleteProfiles,
    GeneralSearchResults
}

typealias SearchResultStateHolder = ActionStateMutator<SearchState.LoadAround, StateFlow<SearchState>>

sealed class SearchState {
    data class Post(
        val currentQuery: SearchQuery.Post,
        val results: TiledList<SearchQuery.Post, SearchResult.Post> = emptyTiledList(),
    ) : SearchState()

    data class Profile(
        val currentQuery: SearchQuery.Profile,
        val results: TiledList<SearchQuery.Profile, SearchResult.Profile> = emptyTiledList(),
    ) : SearchState()

    data class LoadAround(
        val query: SearchQuery,
    )
}

@Serializable
data class State(
    val currentQuery: String = "",
    val layout: ScreenLayout = ScreenLayout.Trends,
    @Transient
    val searchStateHolders: List<SearchResultStateHolder> = emptyList(),
    @Transient
    val autoCompletedProfiles: List<SearchResult.Profile> = emptyList(),
    @Transient
    val messages: List<String> = emptyList(),
)

sealed class Action(val key: String) {

    sealed class Search : Action(key = "Search") {
        data class OnSearchQueryChanged(
            val query: String,
        ) : Search()

        data class OnSearchQueryConfirmed(
            val query: String,
        ) : Search()
    }

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data object Pop : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        }

        data class DelegateTo(
            val delegate: NavigationAction.Common,
        ) : Navigate(), NavigationAction by delegate
    }
}
