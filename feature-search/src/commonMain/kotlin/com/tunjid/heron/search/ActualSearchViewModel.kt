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
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.ListMemberQuery
import com.tunjid.heron.data.repository.ProfileRepository
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
import com.tunjid.heron.search.ui.SuggestedStarterPack
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.emptyTiledList
import com.tunjid.tiler.map
import com.tunjid.tiler.toTiledList
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.milliseconds

internal typealias SearchStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualSearchViewModel
}

@Inject
class ActualSearchViewModel(
    navActions: (NavigationMutation) -> Unit,
    authRepository: AuthRepository,
    searchRepository: SearchRepository,
    profileRepository: ProfileRepository,
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
        loadProfileMutations(authRepository),
        trendsMutations(searchRepository),
        suggestedStarterPackMutations(
            searchRepository = searchRepository,
            profileRepository = profileRepository,
        ),
        suggestedFeedGeneratorMutations(
            searchRepository = searchRepository
        ),
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

                is Action.FetchSuggestedProfiles -> action.flow.suggestedProfilesMutations(
                    searchRepository = searchRepository,
                )

                is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                    writeQueue = writeQueue,
                )

                is Action.ToggleViewerState -> action.flow.toggleViewerStateMutations(
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
    authRepository: AuthRepository,
): Flow<Mutation<State>> =
    authRepository.signedInUser.mapToMutation {
        copy(signedInProfile = it)
    }

private fun trendsMutations(
    searchRepository: SearchRepository,
): Flow<Mutation<State>> =
    searchRepository.trends().mapToMutation {
        copy(trends = it)
    }

private fun suggestedStarterPackMutations(
    searchRepository: SearchRepository,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> =
    searchRepository.suggestedStarterPacks()
        .flatMapLatest { starterPacks ->
            val starterPackListUris = starterPacks.mapNotNull { it.list?.uri }
            val listMembersFlow = starterPackListUris.map { listUri ->
                profileRepository.listMembers(
                    query = ListMemberQuery(
                        listUri = listUri,
                        data = CursorQuery.Data(
                            page = 0,
                            cursorAnchor = Clock.System.now(),
                            limit = 10
                        )
                    ),
                    cursor = Cursor.Initial
                )
            }

            val starterPackWithMembersList = starterPacks.map { starterPack ->
                SuggestedStarterPack(
                    starterPack = starterPack,
                    members = emptyList()
                )
            }

            listMembersFlow
                .merge()
                .scan(starterPackWithMembersList) { list, fetchedMembers ->
                    val listUri = fetchedMembers.firstOrNull()?.listUri ?: return@scan list
                    list.map { packWithMembers ->
                        if (packWithMembers.starterPack.list?.uri == listUri) packWithMembers.copy(
                            members = fetchedMembers
                        )
                        else packWithMembers
                    }
                }
                .mapToMutation { copy(starterPacksWithMembers = it) }
        }

private fun suggestedFeedGeneratorMutations(
    searchRepository: SearchRepository,
): Flow<Mutation<State>> =
    searchRepository.suggestedFeeds()
        .mapToMutation { copy(feedGenerators = it) }


private fun Flow<Action.FetchSuggestedProfiles>.suggestedProfilesMutations(
    searchRepository: SearchRepository,
): Flow<Mutation<State>> =
    flatMapLatest { action ->
        searchRepository.suggestedProfiles(
            category = action.category
        )
            .mapLatest { suggestedProfiles ->
                action.category to suggestedProfiles
            }
            .mapToMutation { categoryToProfiles ->
                copy(
                    categoriesToSuggestedProfiles = categoriesToSuggestedProfiles + categoryToProfiles
                )
            }
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
                        layout =
                            if (action.query.isNotBlank()) ScreenLayout.AutoCompleteProfiles
                            else ScreenLayout.Suggested,
                    )

                    is Action.Search.OnSearchQueryConfirmed -> {
                        searchStateHolders.forEach {
                            val confirmedQuery = when (val searchState = it.state.value) {
                                is SearchState.OfPosts -> when (searchState.currentQuery) {
                                    is SearchQuery.OfPosts.Latest -> SearchQuery.OfPosts.Latest(
                                        query = currentQuery,
                                        isLocalOnly = action.isLocalOnly,
                                        data = defaultSearchQueryData()
                                    )

                                    is SearchQuery.OfPosts.Top -> SearchQuery.OfPosts.Top(
                                        query = currentQuery,
                                        isLocalOnly = action.isLocalOnly,
                                        data = defaultSearchQueryData()
                                    )
                                }

                                is SearchState.OfProfiles -> SearchQuery.OfProfiles(
                                    query = currentQuery,
                                    isLocalOnly = action.isLocalOnly,
                                    data = defaultSearchQueryData()
                                )

                                is SearchState.OfFeedGenerators -> SearchQuery.OfFeedGenerators(
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
                    query = SearchQuery.OfProfiles(
                        query = it.query,
                        isLocalOnly = false,
                        data = defaultSearchQueryData(),
                    ),
                    cursor = Cursor.Pending
                )
            }
            .mapToMutation { profileWithViewerStates ->
                copy(
                    autoCompletedProfiles = profileWithViewerStates.map { profileWithViewerState ->
                        SearchResult.OfProfile(
                            profileWithViewerState = profileWithViewerState,
                            sharedElementPrefix = "auto-complete-results"
                        )
                    }
                )
            }
    )
}

private fun Flow<Action.ToggleViewerState>.toggleViewerStateMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        writeQueue.enqueue(
            Writable.Connection(
                when (val following = action.following) {
                    null -> Profile.Connection.Follow(
                        signedInProfileId = action.signedInProfileId,
                        profileId = action.viewedProfileId,
                        followedBy = action.followedBy,
                    )

                    else -> Profile.Connection.Unfollow(
                        signedInProfileId = action.signedInProfileId,
                        profileId = action.viewedProfileId,
                        followUri = following,
                        followedBy = action.followedBy,
                    )
                }
            )
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
    SearchState.OfPosts(
        currentQuery = SearchQuery.OfPosts.Top(
            query = "",
            isLocalOnly = false,
            data = defaultSearchQueryData(),
        ),
        results = emptyTiledList(),
    ),
    SearchState.OfPosts(
        currentQuery = SearchQuery.OfPosts.Latest(
            query = "",
            isLocalOnly = false,
            data = defaultSearchQueryData(),
        ),
        results = emptyTiledList(),
    ),
    SearchState.OfProfiles(
        currentQuery = SearchQuery.OfProfiles(
            query = "",
            isLocalOnly = false,
            data = defaultSearchQueryData(),
        ),
        results = emptyTiledList(),
    ),
    SearchState.OfFeedGenerators(
        currentQuery = SearchQuery.OfFeedGenerators(
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
                    is SearchState.OfPosts -> type().flow.searchMutations(
                        coroutineScope = coroutineScope,
                        updatePage = {
                            when (this) {
                                is SearchQuery.OfPosts.Latest -> copy(data = it)
                                is SearchQuery.OfPosts.Top -> copy(data = it)
                            }
                        },
                        cursorListLoader = searchRepository::postSearch,
                        searchStateResultsMutation = { posts ->
                            check(this is SearchState.OfPosts)

                            if (posts.isValidFor(currentQuery)) copy(
                                results = posts
                                    .distinctBy(Post::cid)
                                    .map { post ->
                                        SearchResult.OfPost(
                                            post = post,
                                            sharedElementPrefix = currentQuery.sourceId,
                                        )
                                    }
                            )
                            else this
                        }
                    )

                    is SearchState.OfProfiles -> type().flow.searchMutations(
                        coroutineScope = coroutineScope,
                        updatePage = {
                            copy(data = it)
                        },
                        cursorListLoader = searchRepository::profileSearch,
                        searchStateResultsMutation = { profileWithViewerStates ->
                            check(this is SearchState.OfProfiles)
                            if (profileWithViewerStates.isValidFor(currentQuery)) copy(
                                results = profileWithViewerStates
                                    .distinctBy { it.profile.did }
                                    .map { profileWithViewerState ->
                                        SearchResult.OfProfile(
                                            profileWithViewerState = profileWithViewerState,
                                            sharedElementPrefix = currentQuery.sourceId,
                                        )
                                    }
                            )
                            else this
                        }
                    )

                    is SearchState.OfFeedGenerators -> type().flow.searchMutations(
                        coroutineScope = coroutineScope,
                        updatePage = {
                            copy(data = it)
                        },
                        cursorListLoader = searchRepository::feedGeneratorSearch,
                        searchStateResultsMutation = { feedGenerators ->
                            check(this is SearchState.OfFeedGenerators)
                            if (feedGenerators.isValidFor(currentQuery)) copy(
                                results = feedGenerators
                                    .distinctBy(FeedGenerator::cid)
                                    .map { feedGenerator ->
                                        SearchResult.OfFeedGenerator(
                                            feedGenerator = feedGenerator,
                                            sharedElementPrefix = currentQuery.sourceId,
                                        )
                                    }
                            )
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
        reified Item,
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
            is SearchQuery.OfPosts -> {
                check(this is SearchState.OfPosts)
                copy(currentQuery = query)
            }

            is SearchQuery.OfProfiles -> {
                check(this is SearchState.OfProfiles)
                copy(currentQuery = query)
            }

            is SearchQuery.OfFeedGenerators -> {
                check(this is SearchState.OfFeedGenerators)
                copy(currentQuery = query)
            }
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