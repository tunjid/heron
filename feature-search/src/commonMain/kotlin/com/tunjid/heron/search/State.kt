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

import com.tunjid.heron.data.core.models.SearchResult
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.treenav.pop
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

enum class ScreenLayout {
    Trends,
    AutoCompleteProfiles,
    GeneralSearchResults
}

@Serializable
sealed class SearchState {

    @Serializable
    data class Post(
        val currentQuery: SearchQuery.Post,
        @Transient
        val results: TiledList<SearchQuery.Post, SearchResult.Post> = emptyTiledList(),
    ) : SearchState()

    @Serializable
    data class Profile(
        val currentQuery: SearchQuery.Profile,
        @Transient
        val results: TiledList<SearchQuery.Profile, SearchResult.Profile> = emptyTiledList(),
    ) : SearchState()
}

@Serializable
data class State(
    val currentQuery: String = "",
    val topPostsState: SearchState.Post = SearchState.Post(
        currentQuery = SearchQuery.Post.Top(
            query = "",
            isLocalOnly = false,
            data = CursorQuery.Data(
                page = 0,
                firstRequestInstant = Clock.System.now(),
                limit = 15
            ),
        ),
        results = emptyTiledList(),
    ),
    val latestPostsState: SearchState.Post = SearchState.Post(
        currentQuery = SearchQuery.Post.Latest(
            query = "",
            isLocalOnly = false,
            data = CursorQuery.Data(
                page = 0,
                firstRequestInstant = Clock.System.now(),
                limit = 15
            ),
        ),
        results = emptyTiledList(),
    ),
    val profilesState: SearchState.Profile = SearchState.Profile(
        currentQuery = SearchQuery.Profile(
            query = "",
            isLocalOnly = false,
            data = CursorQuery.Data(
                page = 0,
                firstRequestInstant = Clock.System.now(),
                limit = 15
            ),
        ),
        results = emptyTiledList(),
    ),
    @Transient
    val messages: List<String> = emptyList(),
)


sealed class Action(val key: String) {

    data class OnSearchQueryChanged(
        val query: String,
    ): Action("OnSearchQueryChanged")

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data object Pop : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        }
    }
}