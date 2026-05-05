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
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelinePreference
import com.tunjid.heron.data.core.models.timelineRecordUri
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.ListMemberQuery
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.data.repository.SearchRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.repository.recentConversations
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.search.di.query
import com.tunjid.heron.search.ui.suggestions.SuggestedStarterPack
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.mapCursorList
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.tiling.tilingMutations
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.coroutines.launchAndCollect
import com.tunjid.heron.ui.coroutines.launchAndCollectLatest
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
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
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): SearchViewModel
}

@AssistedInject
class SearchViewModel(
    navActions: (NavigationMutation) -> Unit,
    authRepository: AuthRepository,
    messageRepository: MessageRepository,
    recordRepository: RecordRepository,
    searchRepository: SearchRepository,
    timelineRepository: TimelineRepository,
    userDataRepository: UserDataRepository,
    writeQueue: WriteQueue,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    SearchStateHolder by scope.actionSuspendingStateMutator(
        initialState = State.Immutable(
            currentQuery = route.query,
            isQueryEditable = route.query.isBlank(),
            layout = when {
                route.query.isBlank() -> ScreenLayout.Suggested
                else -> ScreenLayout.GeneralSearchResults
            },
            searchStateHolders = route.searchStates()
                .mapNotNull { searchState ->
                    scope.searchStateHolder(searchState, searchRepository)
                },
        ).toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { state, actions ->
            loadProfileMutations(
                state = state,
                authRepository = authRepository,
            )
            searchStateHolderMutations(
                state = state,
                routeScope = scope,
                availableSearchStates = route.searchStates(),
                authRepository = authRepository,
                searchRepository = searchRepository,
            )
            trendsMutations(
                state = state,
                searchRepository = searchRepository,
            )
            suggestedStarterPackMutations(
                state = state,
                searchRepository = searchRepository,
                recordRepository = recordRepository,
            )
            suggestedFeedGeneratorMutations(
                state = state,
                searchRepository = searchRepository,
            )
            feedGeneratorUrisToStatusMutations(
                state = state,
                timelineRepository = timelineRepository,
            )
            loadPreferencesMutations(
                state = state,
                userDataRepository = userDataRepository,
            )
            actions.launchMutationsIn(
                productionScope = this,
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.Search -> action.flow.searchQueryMutations(
                        state = state,
                        coroutineScope = scope,
                        searchRepository = searchRepository,
                    )
                    is Action.FetchSuggestedProfiles -> action.flow.suggestedProfilesMutations(
                        state = state,
                        searchRepository = searchRepository,
                    )
                    is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations(state)
                    is Action.ToggleViewerState -> action.flow.toggleViewerStateMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.UpdateFeedGeneratorStatus -> action.flow.feedGeneratorStatusMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.Navigate -> action.flow.collect {
                        navActions(it.navigationMutation)
                    }
                    is Action.UpdateMutedWord -> action.flow.updateMutedWordMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.BlockAccount -> action.flow.blockAccountMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.MuteAccount -> action.flow.muteAccountMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.UpdateRecentConversations -> action.flow.recentConversationMutations(
                        state = state,
                        messageRepository = messageRepository,
                    )
                    is Action.UpdateRecentLists -> action.flow.recentListsMutations(
                        state = state,
                        recordRepository = recordRepository,
                    )
                    is Action.DeleteRecord -> action.flow.deleteRecordMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private fun loadProfileMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser.launchAndCollect { signedInProfile ->
    state.signedInProfile = signedInProfile
}

context(productionScope: CoroutineScope)
private fun searchStateHolderMutations(
    state: State.SnapshotMutable,
    availableSearchStates: List<SearchState>,
    routeScope: CoroutineScope,
    authRepository: AuthRepository,
    searchRepository: SearchRepository,
) = authRepository.signedInUser
    .map { it != null }
    .distinctUntilChanged()
    .launchAndCollect { isSignedIn ->
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
private fun Flow<Action.UpdateRecentConversations>.recentConversationMutations(
    state: State.SnapshotMutable,
    messageRepository: MessageRepository,
) = launchAndCollectLatest {
    messageRepository.recentConversations().collect { conversations ->
        state.recentConversations = conversations
    }
}

context(productionScope: CoroutineScope)
private fun loadPreferencesMutations(
    state: State.SnapshotMutable,
    userDataRepository: UserDataRepository,
) = userDataRepository.preferences.launchAndCollect {
    state.preferences = it
}

context(productionScope: CoroutineScope)
private fun trendsMutations(
    state: State.SnapshotMutable,
    searchRepository: SearchRepository,
) = searchRepository.trends().launchAndCollect {
    state.trends = it
}

context(productionScope: CoroutineScope)
private fun suggestedStarterPackMutations(
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
    .launchAndCollect {
        state.starterPacksWithMembers = it
    }

context(productionScope: CoroutineScope)
private fun suggestedFeedGeneratorMutations(
    state: State.SnapshotMutable,
    searchRepository: SearchRepository,
) = searchRepository.suggestedFeeds().launchAndCollect {
    state.feedGenerators = it
}

context(productionScope: CoroutineScope)
private fun feedGeneratorUrisToStatusMutations(
    state: State.SnapshotMutable,
    timelineRepository: TimelineRepository,
) = timelineRepository.preferences
    .distinctUntilChangedBy { it.timelinePreferences }
    .launchAndCollect { preferences ->
        state.timelineRecordUrisToPinnedStatus = preferences.timelinePreferences
            .associateBy(
                keySelector = TimelinePreference::timelineRecordUri,
                valueTransform = TimelinePreference::pinned,
            )
    }

context(productionScope: CoroutineScope)
private fun Flow<Action.FetchSuggestedProfiles>.suggestedProfilesMutations(
    state: State.SnapshotMutable,
    searchRepository: SearchRepository,
) = launchAndCollectLatest { action ->
    searchRepository.suggestedProfiles(
        category = action.category,
    ).collect { suggestedProfiles ->
        state.categoriesToSuggestedProfiles += (action.category to suggestedProfiles)
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.Search>.searchQueryMutations(
    state: State.SnapshotMutable,
    coroutineScope: CoroutineScope,
    searchRepository: SearchRepository,
) {
    val shared = shareIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        replay = 1,
    )
    shared.launchAndCollect { action ->
        when (action) {
            is Action.Search.OnSearchQueryChanged -> {
                state.currentQuery = action.query
                state.layout =
                    if (action.query.isNotBlank()) ScreenLayout.AutoCompleteProfiles
                    else ScreenLayout.Suggested
            }
            is Action.Search.OnSearchQueryConfirmed -> {
                state.searchStateHolders.forEach {
                    val confirmedQuery = when (val searchState = it.state) {
                        is SearchState.OfPosts -> when (searchState.tilingData.currentQuery) {
                            is SearchQuery.OfPosts.Latest -> SearchQuery.OfPosts.Latest(
                                query = state.currentQuery,
                                isLocalOnly = action.isLocalOnly,
                                data = defaultSearchQueryData(),
                            )
                            is SearchQuery.OfPosts.Top -> SearchQuery.OfPosts.Top(
                                query = state.currentQuery,
                                isLocalOnly = action.isLocalOnly,
                                data = defaultSearchQueryData(),
                            )
                        }
                        is SearchState.OfProfiles -> SearchQuery.OfProfiles(
                            query = state.currentQuery,
                            isLocalOnly = action.isLocalOnly,
                            data = defaultSearchQueryData(),
                        )
                        is SearchState.OfFeedGenerators -> SearchQuery.OfFeedGenerators(
                            query = state.currentQuery,
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
        .launchAndCollect { profileWithViewerStates ->
            state.autoCompletedProfiles = profileWithViewerStates.map(SearchResult::OfProfile)
        }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.ToggleViewerState>.toggleViewerStateMutations(
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
private fun Flow<Action.UpdateMutedWord>.updateMutedWordMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = {
        Writable.TimelineUpdate(
            Timeline.Update.OfMutedWord.ReplaceAll(
                mutedWordPreferences = it.mutedWordPreference,
            ),
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.BlockAccount>.blockAccountMutations(
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
private fun Flow<Action.MuteAccount>.muteAccountMutations(
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
private fun Flow<Action.DeleteRecord>.deleteRecordMutations(
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
private fun Flow<Action.SendPostInteraction>.postInteractionMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action -> Writable.Interaction(action.interaction) },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchAndCollect { event ->
    state.messages -= event.message
}

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateFeedGeneratorStatus>.feedGeneratorStatusMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action -> Writable.TimelineUpdate(action.update) },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateRecentLists>.recentListsMutations(
    state: State.SnapshotMutable,
    recordRepository: RecordRepository,
) = launchAndCollectLatest {
    recordRepository.recentLists.collect { lists ->
        state.recentLists = lists
    }
}

private fun Route.searchStates(): List<SearchState> = buildList {
    add(
        SearchState.OfPosts(
            tilingData = TilingState.Data(
                currentQuery = SearchQuery.OfPosts.Top(
                    query = query,
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
                    query = query,
                    isLocalOnly = false,
                    data = defaultSearchQueryData(),
                ),
            ),
        ),
    )
    if (query.isNotBlank()) {
        add(
            SearchState.OfProfiles(
                tilingData = TilingState.Data(
                    currentQuery = SearchQuery.OfProfiles(
                        query = query,
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
                        query = query,
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
        initialState = searchState,
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { holderState, actions ->
            actions.map { it.tilingAction }
                .tilingMutations(
                    currentState = { holderState },
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
        initialState = searchState,
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { holderState, actions ->
            actions.map { it.tilingAction }
                .tilingMutations(
                    currentState = { holderState },
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
        initialState = searchState,
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { holderState, actions ->
            actions.map { it.tilingAction }
                .tilingMutations(
                    currentState = { holderState },
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
