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

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelinePreference
import com.tunjid.heron.data.core.models.timelineRecordUri
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.ListMemberQuery
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.data.repository.SearchRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.data.utilities.writequeue.toSubscriptionWritable
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.search.di.query
import com.tunjid.heron.search.ui.suggestions.SuggestedStarterPack
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.launchTilingMutations
import com.tunjid.heron.tiling.mapCursorList
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.coroutines.RouteViewModel
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
import com.tunjid.tiler.distinctBy
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn

internal typealias SearchStateHolder = ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface SearchViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): SearchViewModel
}

@Stable
@AssistedInject
class SearchViewModel(
    navActions: (NavigationMutation) -> Unit,
    authRepository: AuthRepository,
    recordRepository: RecordRepository,
    searchRepository: SearchRepository,
    timelineRepository: TimelineRepository,
    userDataRepository: UserDataRepository,
    writeQueue: WriteQueue,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : RouteViewModel(scope, route),
    SearchStateHolder by scope.actionSuspendingStateMutator(
        state = State.Immutable(
            searchBarText = route.query.initialSearchBarText,
            query = route.query,
            layout = route.query.initialLayout,
            searchStateHolders = route.searchStates()
                .mapNotNull { searchState ->
                    scope.searchStateHolder(searchState, searchRepository)
                },
        ).toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { state, actions ->
            launchLoadProfileMutations(
                state = state,
                authRepository = authRepository,
            )
            launchSearchStateHolderMutations(
                state = state,
                routeScope = scope,
                availableSearchStates = route.searchStates(),
                authRepository = authRepository,
                searchRepository = searchRepository,
            )
            launchTrendsMutations(
                state = state,
                searchRepository = searchRepository,
            )
            launchSuggestedStarterPackMutations(
                state = state,
                searchRepository = searchRepository,
                recordRepository = recordRepository,
            )
            launchSuggestedFeedGeneratorMutations(
                state = state,
                searchRepository = searchRepository,
            )
            launchFeedGeneratorUrisToStatusMutations(
                state = state,
                timelineRepository = timelineRepository,
            )
            launchLoadPreferencesMutations(
                state = state,
                userDataRepository = userDataRepository,
            )
            actions.launchMutationsIn(
                productionScope = this,
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.Search -> action.flow.launchSearchQueryMutations(
                        state = state,
                        coroutineScope = scope,
                        searchRepository = searchRepository,
                    )
                    is Action.FetchSuggestedProfiles -> action.flow.launchSuggestedProfilesMutations(
                        state = state,
                        searchRepository = searchRepository,
                    )
                    is Action.TogglePublicationSubscription -> action.flow.launchTogglePublicationSubscriptionMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(state)
                    is Action.ToggleViewerState -> action.flow.launchToggleViewerStateMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.UpdateFeedGeneratorStatus -> action.flow.launchFeedGeneratorStatusMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.Navigate -> action.flow.collect {
                        navActions(it.navigationMutation)
                    }
                    is Action.BlockAccount -> action.flow.launchBlockAccountMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.MuteAccount -> action.flow.launchMuteAccountMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.DeleteRecord -> action.flow.launchDeleteRecordMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private fun launchLoadProfileMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser.launchedCollect { signedInProfile ->
    state.signedInProfile = signedInProfile
}

context(productionScope: CoroutineScope)
private fun launchSearchStateHolderMutations(
    state: State.SnapshotMutable,
    availableSearchStates: List<SearchState>,
    routeScope: CoroutineScope,
    authRepository: AuthRepository,
    searchRepository: SearchRepository,
) = authRepository.signedInUser
    .map { it != null }
    .distinctUntilChanged()
    .launchedCollect { isSignedIn ->
        val existingHolders = state.searchStateHolders
            .associateBy { it.state.key }

        state.searchStateHolders = availableSearchStates.mapNotNull { searchState ->
            when (searchState) {
                is SearchState.OfPosts -> when {
                    isSignedIn -> existingHolders[searchState.key]
                        ?: routeScope.searchStateHolder(
                            searchState = searchState,
                            searchRepository = searchRepository,
                        )
                    else -> null
                }
                is SearchState.OfFeedGenerators,
                is SearchState.OfProfiles,
                -> existingHolders[searchState.key]
                    ?: routeScope.searchStateHolder(
                        searchState = searchState,
                        searchRepository = searchRepository,
                    )
            }
        }
    }

context(productionScope: CoroutineScope)
private fun launchLoadPreferencesMutations(
    state: State.SnapshotMutable,
    userDataRepository: UserDataRepository,
) = userDataRepository.preferences.launchedCollect {
    state.preferences = it
}

context(productionScope: CoroutineScope)
private fun launchTrendsMutations(
    state: State.SnapshotMutable,
    searchRepository: SearchRepository,
) = searchRepository.trends().launchedCollect {
    state.trends = it
}

context(productionScope: CoroutineScope)
private fun launchSuggestedStarterPackMutations(
    state: State.SnapshotMutable,
    searchRepository: SearchRepository,
    recordRepository: RecordRepository,
) = searchRepository.suggestedStarterPacks()
    .flatMapLatest { starterPacks ->
        val starterPackListUris = starterPacks.mapNotNull { it.list?.uri }
        val listMembersFlow = starterPackListUris.map { listUri ->
            recordRepository.listMembers(
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
                    if (packWithMembers.starterPack.list?.uri == listUri) packWithMembers.copy(
                        members = fetchedMembers,
                    )
                    else packWithMembers
                }
            }
    }
    .launchedCollect {
        state.starterPacksWithMembers = it
    }

context(productionScope: CoroutineScope)
private fun launchSuggestedFeedGeneratorMutations(
    state: State.SnapshotMutable,
    searchRepository: SearchRepository,
) = searchRepository.suggestedFeeds().launchedCollect {
    state.feedGenerators = it
}

context(productionScope: CoroutineScope)
private fun launchFeedGeneratorUrisToStatusMutations(
    state: State.SnapshotMutable,
    timelineRepository: TimelineRepository,
) = timelineRepository.preferences
    .distinctUntilChangedBy { it.timelinePreferences }
    .launchedCollect { preferences ->
        state.timelineRecordUrisToPinnedStatus = preferences.timelinePreferences
            .associateBy(
                keySelector = TimelinePreference::timelineRecordUri,
                valueTransform = TimelinePreference::pinned,
            )
    }

context(productionScope: CoroutineScope)
private fun Flow<Action.FetchSuggestedProfiles>.launchSuggestedProfilesMutations(
    state: State.SnapshotMutable,
    searchRepository: SearchRepository,
) = launchedCollectLatest { action ->
    searchRepository.suggestedProfiles(
        category = action.category,
    ).collect { suggestedProfiles ->
        state.categoriesToSuggestedProfiles += (action.category to suggestedProfiles)
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.Search>.launchSearchQueryMutations(
    state: State.SnapshotMutable,
    coroutineScope: CoroutineScope,
    searchRepository: SearchRepository,
) {
    val shared = shareIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        replay = 1,
    )
    shared.launchedCollect { action ->
        when (action) {
            is Action.Search.OnSearchQueryChanged -> {
                state.searchBarText = action.query
                state.layout = state.query.layoutFor(action)
            }
            is Action.Search.OnSearchQueryConfirmed -> {
                state.searchStateHolders.forEach {
                    val currentQuery = state.query.queryString(
                        searchBarText = state.searchBarText,
                    )
                    val confirmedQuery = when (val searchState = it.state) {
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
                state.layout = ScreenLayout.GeneralSearchResults
            }
        }
    }
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
                cursor = Cursor.Initial,
            )
        }
        .launchedCollect { profileWithViewerStates ->
            state.autoCompletedProfiles = profileWithViewerStates.map(SearchResult::OfProfile)
        }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.ToggleViewerState>.launchToggleViewerStateMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
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
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.BlockAccount>.launchBlockAccountMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
        Writable.Restriction(
            Profile.Restriction.Block.Add(
                signedInProfileId = action.signedInProfileId,
                profileId = action.profileId,
            ),
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.MuteAccount>.launchMuteAccountMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
        Writable.Restriction(
            Profile.Restriction.Mute.Add(
                signedInProfileId = action.signedInProfileId,
                profileId = action.profileId,
            ),
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.DeleteRecord>.launchDeleteRecordMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
        Writable.RecordDeletion(
            recordUri = action.recordUri,
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.TogglePublicationSubscription>.launchTogglePublicationSubscriptionMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { it.publication.toSubscriptionWritable() },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchedCollect { event ->
    state.messages -= event.message
}

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateFeedGeneratorStatus>.launchFeedGeneratorStatusMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action -> Writable.TimelineUpdate(action.update) },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

private fun Route.searchStates(): List<SearchState> = buildList {
    add(
        SearchState.OfPosts(
            tilingData = TilingState.Data(
                currentQuery = SearchQuery.OfPosts.Top(
                    query = query.initialQueryString,
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
                    query = query.initialQueryString,
                    isLocalOnly = false,
                    data = defaultSearchQueryData(),
                ),
            ),
        ),
    )
    if (query.supportsNonPostSearch) {
        add(
            SearchState.OfProfiles(
                tilingData = TilingState.Data(
                    currentQuery = SearchQuery.OfProfiles(
                        query = query.initialQueryString,
                        isLocalOnly = false,
                        data = defaultSearchQueryData(),
                    ),
                ),
            ),
        )
        add(
            SearchState.OfFeedGenerators(
                tilingData = TilingState.Data(
                    currentQuery = SearchQuery.OfFeedGenerators(
                        query = query.initialQueryString,
                        isLocalOnly = false,
                        data = defaultSearchQueryData(),
                    ),
                ),
            ),
        )
    }
}

private fun CoroutineScope.searchStateHolder(
    searchState: SearchState,
    searchRepository: SearchRepository,
): SearchResultStateHolder? = when (searchState) {
    is SearchState.OfPosts -> actionSuspendingStateMutator(
        state = searchState,
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { holderState, actions ->
            actions.map { it.tilingAction }
                .launchTilingMutations(
                    state = holderState,
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
                        searchRepository::postSearch.mapCursorList { post ->
                            SearchResult.OfPost(
                                timelineItem = post,
                            )
                        }.invoke(query, cursor)
                    },
                    onNewItems = { items ->
                        items.distinctBy { it.timelineItem.id }
                    },
                    queryRefreshBy = {
                        it.query to it.data.cursorAnchor
                    },
                )
        },
    )

    is SearchState.OfProfiles -> actionSuspendingStateMutator(
        state = searchState,
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { holderState, actions ->
            actions.map { it.tilingAction }
                .launchTilingMutations(
                    state = holderState,
                    updateQueryData = { copy(data = it) },
                    refreshQuery = { copy(data = data.reset()) },
                    cursorListLoader = searchRepository::profileSearch
                        .mapCursorList(SearchResult::OfProfile),
                    onNewItems = { items ->
                        items.distinctBy { it.profileWithViewerState.profile.did }
                    },
                    queryRefreshBy = {
                        it.query to it.data.cursorAnchor
                    },
                )
        },
    )

    is SearchState.OfFeedGenerators -> actionSuspendingStateMutator(
        state = searchState,
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { holderState, actions ->
            actions.map { it.tilingAction }
                .launchTilingMutations(
                    state = holderState,
                    updateQueryData = { copy(data = it) },
                    refreshQuery = { copy(data = data.reset()) },
                    cursorListLoader = searchRepository::feedGeneratorSearch
                        .mapCursorList(SearchResult::OfFeedGenerator),
                    onNewItems = { items ->
                        items.distinctBy { it.feedGenerator.cid }
                    },
                    queryRefreshBy = {
                        it.query to it.data.cursorAnchor
                    },
                )
        },
    )
}

private fun defaultSearchQueryData() = CursorQuery.Data(
    page = 0,
    cursorAnchor = Clock.System.now(),
    limit = 15,
)
