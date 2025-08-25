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
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelinePreference
import com.tunjid.heron.data.core.models.feedGeneratorUri
import com.tunjid.heron.data.core.models.labelVisibilitiesToDefinitions
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.ListMemberQuery
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.data.repository.SearchRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.search.di.query
import com.tunjid.heron.search.ui.SuggestedStarterPack
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.mapCursorList
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.tiling.tilingMutations
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.distinctBy
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.datetime.Clock

internal typealias SearchStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): SearchViewModel
}

@Inject
class SearchViewModel(
    navActions: (NavigationMutation) -> Unit,
    authRepository: AuthRepository,
    searchRepository: SearchRepository,
    profileRepository: ProfileRepository,
    timelineRepository: TimelineRepository,
    writeQueue: WriteQueue,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    SearchStateHolder by scope.actionStateFlowMutator(
        initialState = State(
            currentQuery = route.query,
            isQueryEditable = route.query.isBlank(),
            layout = when {
                route.query.isBlank() -> ScreenLayout.Suggested
                else -> ScreenLayout.GeneralSearchResults
            },
            searchStateHolders = scope.searchStateHolders(
                initialQuery = route.query,
                searchRepository = searchRepository,
                timelineRepository = timelineRepository,
            ),
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
                searchRepository = searchRepository,
            ),
            feedGeneratorUrisToStatusMutations(
                timelineRepository = timelineRepository,
            ),
        ),
        actionTransform = transform@{ actions ->
            actions.toMutationStream(
                keySelector = Action::key,
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

                    is Action.UpdateFeedGeneratorStatus -> action.flow.feedGeneratorStatusMutations(
                        writeQueue = writeQueue,
                    )

                    is Action.Navigate -> action.flow.consumeNavigationActions(
                        navigationMutationConsumer = navActions,
                    )
                }
            }
        },
    )

private fun loadProfileMutations(
    authRepository: AuthRepository,
): Flow<Mutation<State>> = authRepository.signedInUser.mapToMutation { signedInProfile ->
    val isSignedIn = signedInProfile != null
    copy(
        signedInProfile = signedInProfile,
        searchStateHolders = searchStateHolders.filter { mutator ->
            when (mutator.state.value) {
                is SearchState.OfFeedGenerators -> true
                is SearchState.OfPosts -> isSignedIn
                is SearchState.OfProfiles -> true
            }
        },
    )
}

private fun trendsMutations(
    searchRepository: SearchRepository,
): Flow<Mutation<State>> = searchRepository.trends().mapToMutation {
    copy(trends = it)
}

private fun suggestedStarterPackMutations(
    searchRepository: SearchRepository,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> = searchRepository.suggestedStarterPacks()
    .flatMapLatest { starterPacks ->
        val starterPackListUris = starterPacks.mapNotNull { it.list?.uri }
        val listMembersFlow = starterPackListUris.map { listUri ->
            profileRepository.listMembers(
                query = ListMemberQuery(
                    listUri = listUri,
                    data = CursorQuery.Data(
                        page = 0,
                        cursorAnchor = Clock.System.now(),
                        limit = 10,
                    ),
                ),
                cursor = Cursor.Initial,
            )
        }

        val starterPackWithMembersList = starterPacks.map { starterPack ->
            SuggestedStarterPack(
                starterPack = starterPack,
                members = emptyList(),
            )
        }

        listMembersFlow
            .merge()
            .scan(starterPackWithMembersList) { list, fetchedMembers ->
                val listUri = fetchedMembers.firstOrNull()?.listUri ?: return@scan list
                list.map { packWithMembers ->
                    if (packWithMembers.starterPack.list?.uri == listUri) {
                        packWithMembers.copy(
                            members = fetchedMembers,
                        )
                    } else {
                        packWithMembers
                    }
                }
            }
            .mapToMutation { copy(starterPacksWithMembers = it) }
    }

private fun suggestedFeedGeneratorMutations(
    searchRepository: SearchRepository,
): Flow<Mutation<State>> = searchRepository.suggestedFeeds()
    .mapToMutation { copy(feedGenerators = it) }

private fun feedGeneratorUrisToStatusMutations(
    timelineRepository: TimelineRepository,
): Flow<Mutation<State>> = timelineRepository.preferences()
    .distinctUntilChangedBy { it.timelinePreferences }
    .mapToMutation { preferences ->
        copy(
            feedGeneratorUrisToPinnedStatus = preferences.timelinePreferences
                .associateBy(
                    keySelector = TimelinePreference::feedGeneratorUri,
                    valueTransform = TimelinePreference::pinned,
                ),
        )
    }

private fun Flow<Action.FetchSuggestedProfiles>.suggestedProfilesMutations(
    searchRepository: SearchRepository,
): Flow<Mutation<State>> = flatMapLatest { action ->
    searchRepository.suggestedProfiles(
        category = action.category,
    )
        .mapLatest { suggestedProfiles ->
            action.category to suggestedProfiles
        }
        .mapToMutation { categoryToProfiles ->
            copy(
                categoriesToSuggestedProfiles = categoriesToSuggestedProfiles + categoryToProfiles,
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
                        if (action.query.isNotBlank()) {
                            ScreenLayout.AutoCompleteProfiles
                        } else {
                            ScreenLayout.Suggested
                        },
                    )

                    is Action.Search.OnSearchQueryConfirmed -> {
                        searchStateHolders.forEach {
                            val confirmedQuery = when (val searchState = it.state.value) {
                                is SearchState.OfPosts -> when (searchState.tilingData.currentQuery) {
                                    is SearchQuery.OfPosts.Latest -> SearchQuery.OfPosts.Latest(
                                        query = currentQuery,
                                        isLocalOnly = action.isLocalOnly,
                                        data = defaultSearchQueryData(),
                                    )

                                    is SearchQuery.OfPosts.Top -> SearchQuery.OfPosts.Top(
                                        query = currentQuery,
                                        isLocalOnly = action.isLocalOnly,
                                        data = defaultSearchQueryData(),
                                    )
                                }

                                is SearchState.OfProfiles -> SearchQuery.OfProfiles(
                                    query = currentQuery,
                                    isLocalOnly = action.isLocalOnly,
                                    data = defaultSearchQueryData(),
                                )

                                is SearchState.OfFeedGenerators -> SearchQuery.OfFeedGenerators(
                                    query = currentQuery,
                                    isLocalOnly = action.isLocalOnly,
                                    data = defaultSearchQueryData(),
                                )
                            }

                            it.accept(
                                SearchState.Tile(
                                    tilingAction = TilingState.Action.LoadAround(confirmedQuery),
                                ),
                            )
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
                    cursor = Cursor.Pending,
                )
            }
            .mapToMutation { profileWithViewerStates ->
                copy(
                    autoCompletedProfiles = profileWithViewerStates.map { profileWithViewerState ->
                        SearchResult.OfProfile(
                            profileWithViewerState = profileWithViewerState,
                            sharedElementPrefix = "auto-complete-results",
                        )
                    },
                )
            },
    )
}

private fun Flow<Action.ToggleViewerState>.toggleViewerStateMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = mapToManyMutations { action ->
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
            },
        ),
    )
}

private fun Flow<Action.SendPostInteraction>.postInteractionMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = mapToManyMutations { action ->
    writeQueue.enqueue(Writable.Interaction(action.interaction))
}

private fun Flow<Action.UpdateFeedGeneratorStatus>.feedGeneratorStatusMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = mapToManyMutations { action ->
    writeQueue.enqueue(Writable.TimelineUpdate(action.update))
}

private fun CoroutineScope.searchStateHolders(
    initialQuery: String,
    searchRepository: SearchRepository,
    timelineRepository: TimelineRepository,
): List<SearchResultStateHolder> = buildList {
    add(
        SearchState.OfPosts(
            tilingData = TilingState.Data(
                currentQuery = SearchQuery.OfPosts.Top(
                    query = initialQuery,
                    isLocalOnly = false,
                    data = defaultSearchQueryData(),
                ),
            ),
        ),
    )
    add(
        SearchState.OfPosts(
            tilingData = TilingState.Data(
                currentQuery = SearchQuery.OfPosts.Latest(
                    query = initialQuery,
                    isLocalOnly = false,
                    data = defaultSearchQueryData(),
                ),
            ),
        ),
    )
    if (initialQuery.isBlank()) {
        add(
            SearchState.OfProfiles(
                tilingData = TilingState.Data(
                    currentQuery = SearchQuery.OfProfiles(
                        query = initialQuery,
                        isLocalOnly = false,
                        data = defaultSearchQueryData(),
                    ),
                ),
            ),
        )
    }
    if (initialQuery.isBlank()) {
        add(
            SearchState.OfFeedGenerators(
                tilingData = TilingState.Data(
                    currentQuery = SearchQuery.OfFeedGenerators(
                        query = initialQuery,
                        isLocalOnly = false,
                        data = defaultSearchQueryData(),
                    ),
                ),
            ),
        )
    }
}.map { searchState: SearchState ->
    when (searchState) {
        is SearchState.OfPosts -> actionStateFlowMutator(
            initialState = searchState,
            actionTransform = transform@{ actions ->
                actions.toMutationStream {
                    type().flow.map { it.tilingAction }
                        .tilingMutations(
                            currentState = { this@transform.state() },
                            updateQueryData = {
                                when (this) {
                                    is SearchQuery.OfPosts.Latest -> copy(data = it)
                                    is SearchQuery.OfPosts.Top -> copy(data = it)
                                }
                            },
                            refreshQuery = {
                                when (this) {
                                    is SearchQuery.OfPosts.Latest -> copy(data = data.reset())
                                    is SearchQuery.OfPosts.Top -> copy(data = data.reset())
                                }
                            },
                            cursorListLoader = { query, cursor ->
                                combine(
                                    timelineRepository.labelers(),
                                    timelineRepository.preferences()
                                        .map { it.contentLabelPreferences },
                                    ::Pair,
                                )
                                    .distinctUntilChanged()
                                    .flatMapLatest { (labelers, contentLabelPreferences) ->
                                        searchRepository::postSearch.mapCursorList { post ->
                                            SearchResult.OfPost(
                                                post = post,
                                                sharedElementPrefix = searchState.tilingData.currentQuery.sourceId,
                                                labelVisibilitiesToDefinitions = post.labelVisibilitiesToDefinitions(
                                                    labelers = labelers,
                                                    labelPreferences = contentLabelPreferences,
                                                ),
                                            )
                                        }.invoke(query, cursor)
                                    }
                            },
                            onNewItems = { items ->
                                items.distinctBy { it.post.cid }
                            },
                            queryRefreshBy = {
                                it.query to it.data.cursorAnchor
                            },
                            onTilingDataUpdated = { copy(tilingData = it) },
                        )
                }
            },
        )

        is SearchState.OfProfiles -> actionStateFlowMutator(
            initialState = searchState,
            actionTransform = transform@{ actions ->
                actions.toMutationStream {
                    type().flow.map { it.tilingAction }
                        .tilingMutations(
                            currentState = { this@transform.state() },
                            updateQueryData = { copy(data = it) },
                            refreshQuery = { copy(data = data.reset()) },
                            cursorListLoader = searchRepository::profileSearch.mapCursorList {
                                SearchResult.OfProfile(
                                    profileWithViewerState = it,
                                    sharedElementPrefix = searchState.tilingData.currentQuery.sourceId,
                                )
                            },
                            onNewItems = { items ->
                                items.distinctBy { it.profileWithViewerState.profile.did }
                            },
                            queryRefreshBy = {
                                it.query to it.data.cursorAnchor
                            },
                            onTilingDataUpdated = { copy(tilingData = it) },
                        )
                }
            },
        )

        is SearchState.OfFeedGenerators -> actionStateFlowMutator(
            initialState = searchState,
            actionTransform = transform@{ actions ->
                actions.toMutationStream {
                    type().flow.map { it.tilingAction }
                        .tilingMutations(
                            currentState = { this@transform.state() },
                            updateQueryData = { copy(data = it) },
                            refreshQuery = { copy(data = data.reset()) },
                            cursorListLoader = searchRepository::feedGeneratorSearch.mapCursorList {
                                SearchResult.OfFeedGenerator(
                                    feedGenerator = it,
                                    sharedElementPrefix = searchState.tilingData.currentQuery.sourceId,
                                )
                            },
                            onNewItems = { items ->
                                items.distinctBy { it.feedGenerator.cid }
                            },
                            queryRefreshBy = {
                                it.query to it.data.cursorAnchor
                            },
                            onTilingDataUpdated = { copy(tilingData = it) },
                        )
                }
            },
        )
    }
}

private fun defaultSearchQueryData() = CursorQuery.Data(
    page = 0,
    cursorAnchor = Clock.System.now(),
    limit = 15,
)
