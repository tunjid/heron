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

package com.tunjid.heron.search


import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.SearchResult
import com.tunjid.heron.data.repository.AuthTokenRepository
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.data.repository.SearchRepository
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.data.utilities.cursorListTiler
import com.tunjid.heron.data.utilities.cursorTileInputs
import com.tunjid.heron.data.utilities.ensureValidAnchors
import com.tunjid.heron.data.utilities.isValidFor
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.tiler.toTiledList
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.datetime.Clock
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.milliseconds

typealias SearchStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Inject
class SearchStateHolderCreator(
    private val creator: (scope: CoroutineScope, route: Route) -> ActualSearchStateHolder,
) : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualSearchStateHolder = creator.invoke(scope, route)
}

@Inject
class ActualSearchStateHolder(
    navActions: (NavigationMutation) -> Unit,
    authTokenRepository: AuthTokenRepository,
    searchRepository: SearchRepository,
    writeQueue: WriteQueue,
    @Assisted
    scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER")
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope), SearchStateHolder by scope.actionStateFlowMutator(
    initialState = State(
        searchStateHolders = searchStateHolders(
            coroutineScope = scope,
            searchRepository = searchRepository,
        )
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
        loadProfileMutations(authTokenRepository),
        trendsMutations(searchRepository),
    ),
    actionTransform = transform@{ actions ->
        actions.toMutationStream(
            keySelector = Action::key
        ) {
            when (val action = type()) {
                is Action.Search -> action.flow.searchQueryMutations(
                    coroutineScope = scope,
                    searchRepository = searchRepository,
                )

                is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                    writeQueue = writeQueue,
                )

                is Action.Navigate -> action.flow.consumeNavigationActions(
                    navigationMutationConsumer = navActions
                )
            }
        }
    }
)

private fun loadProfileMutations(
    authTokenRepository: AuthTokenRepository,
): Flow<Mutation<State>> =
    authTokenRepository.signedInUser.mapToMutation {
        copy(signedInProfile = it)
    }

private fun trendsMutations(
    searchRepository: SearchRepository,
): Flow<Mutation<State>> =
    searchRepository.trends().mapToMutation {
        copy(trends = it)
    }

private fun Flow<Action.Search>.searchQueryMutations(
    coroutineScope: CoroutineScope,
    searchRepository: SearchRepository,
): Flow<Mutation<State>> {
    val shared = shareIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        replay = 1,
    )
    return merge(
        shared
            .mapToMutation { action ->
                when (action) {
                    is Action.Search.OnSearchQueryChanged -> copy(
                        currentQuery = action.query,
                        layout = ScreenLayout.AutoCompleteProfiles,
                    )

                    is Action.Search.OnSearchQueryConfirmed -> {
                        searchStateHolders.forEach {
                            val confirmedQuery = when (val searchState = it.state.value) {
                                is SearchState.Post -> when (searchState.currentQuery) {
                                    is SearchQuery.Post.Latest -> SearchQuery.Post.Top(
                                        query = currentQuery,
                                        isLocalOnly = action.isLocalOnly,
                                        data = defaultSearchQueryData()
                                    )

                                    is SearchQuery.Post.Top -> SearchQuery.Post.Top(
                                        query = currentQuery,
                                        isLocalOnly = action.isLocalOnly,
                                        data = defaultSearchQueryData()
                                    )
                                }

                                is SearchState.Profile -> SearchQuery.Profile(
                                    query = currentQuery,
                                    isLocalOnly = action.isLocalOnly,
                                    data = defaultSearchQueryData()
                                )
                            }
                            it.accept(SearchState.LoadAround(confirmedQuery))
                        }
                        copy(
                            layout = ScreenLayout.GeneralSearchResults,
                        )
                    }
                }
            },
        shared
            .filterIsInstance<Action.Search.OnSearchQueryChanged>()
            .debounce(300.milliseconds.inWholeMilliseconds)
            .flatMapLatest {
                searchRepository.autoCompleteProfileSearch(
                    query = SearchQuery.Profile(
                        query = it.query,
                        isLocalOnly = false,
                        data = defaultSearchQueryData(),
                    ),
                    cursor = Cursor.Pending
                )
            }
            .mapToMutation {
                copy(autoCompletedProfiles = it)
            }
    )
}

private fun Flow<Action.SendPostInteraction>.postInteractionMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        writeQueue.enqueue(Writable.Interaction(action.interaction))
    }

private fun searchStateHolders(
    coroutineScope: CoroutineScope,
    searchRepository: SearchRepository,
): List<SearchResultStateHolder> = listOf(
    SearchState.Post(
        currentQuery = SearchQuery.Post.Top(
            query = "",
            isLocalOnly = false,
            data = defaultSearchQueryData(),
        ),
        results = emptyTiledList(),
    ),
    SearchState.Post(
        currentQuery = SearchQuery.Post.Latest(
            query = "",
            isLocalOnly = false,
            data = defaultSearchQueryData(),
        ),
        results = emptyTiledList(),
    ),
    SearchState.Profile(
        currentQuery = SearchQuery.Profile(
            query = "",
            isLocalOnly = false,
            data = defaultSearchQueryData(),
        ),
        results = emptyTiledList(),
    ),
).map { searchState ->
    coroutineScope.actionStateFlowMutator(
        initialState = searchState,
        actionTransform = transform@{ actions ->
            actions.toMutationStream {
                when (state()) {
                    is SearchState.Post -> type().flow.searchMutations(
                        coroutineScope = coroutineScope,
                        updatePage = {
                            when (this) {
                                is SearchQuery.Post.Latest -> copy(data = it)
                                is SearchQuery.Post.Top -> copy(data = it)
                            }
                        },
                        cursorListLoader = searchRepository::postSearch,
                        searchStateResultsMutation = {
                            check(this is SearchState.Post)
                            if (it.isValidFor(currentQuery)) copy(results = it)
                            else this
                        }
                    )

                    is SearchState.Profile -> type().flow.searchMutations(
                        coroutineScope = coroutineScope,
                        updatePage = {
                            copy(data = it)
                        },
                        cursorListLoader = searchRepository::profileSearch,
                        searchStateResultsMutation = {
                            check(this is SearchState.Profile)
                            if (it.isValidFor(currentQuery)) copy(results = it)
                            else this
                        }
                    )
                }
            }
        }
    )
}


private inline fun <
        reified Query : SearchQuery,
        reified Item : SearchResult,
        > Flow<SearchState.LoadAround>.searchMutations(
    coroutineScope: CoroutineScope,
    noinline updatePage: Query.(CursorQuery.Data) -> Query,
    noinline cursorListLoader: (Query, Cursor) -> Flow<CursorList<Item>>,
    noinline searchStateResultsMutation: SearchState.(TiledList<Query, Item>) -> SearchState,
): Flow<Mutation<SearchState>> {
    val sharedQueries = map {
        it.query.takeIf { searchQuery -> searchQuery.query.isNotBlank() }
    }
        .filterIsInstance<Query>()
        .ensureValidAnchors()
        .shareIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            replay = 1,
        )
    val queryMutations = sharedQueries.mapToMutation<Query, SearchState> { query ->
        when (query) {
            is SearchQuery.Post -> {
                check(this is SearchState.Post)
                copy(currentQuery = query)
            }

            is SearchQuery.Profile -> {
                check(this is SearchState.Profile)
                copy(currentQuery = query)
            }

            else -> throw IllegalArgumentException()
        }
    }
    val refreshes = sharedQueries.distinctUntilChangedBy {
        // Refreshes are caused by different queries or different anchors
        it.query to it.data.cursorAnchor
    }
    val itemMutations = refreshes.flatMapLatest { refreshedQuery ->
        cursorTileInputs<Query, Item>(
            numColumns = flowOf(1),
            queries = sharedQueries,
            updatePage = updatePage
        )
            .toTiledList(
                cursorListTiler(
                    startingQuery = refreshedQuery,
                    updatePage = updatePage,
                    cursorListLoader = cursorListLoader,
                )
            )
    }
        .mapToMutation(searchStateResultsMutation)

    return merge(
        queryMutations,
        itemMutations,
    )
}

private fun defaultSearchQueryData() = CursorQuery.Data(
    page = 0,
    cursorAnchor = Clock.System.now(),
    limit = 15
)