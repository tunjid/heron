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
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.SearchFilter
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelinePreference
import com.tunjid.heron.data.core.models.timelineRecordUri
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.ListMemberQuery
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.ProfileSearchQuery
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.TimelineQuery
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.repository.records.FeedGeneratorSearchQuery
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
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.state.timelineStateHolder
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.stateproduction.RouteStateHolder
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

@Stable
internal interface SearchStateHolder :
    RouteStateHolder,
    ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface SearchViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): SearchViewModel
}

@Stable
class SearchViewModel(
    mutator: ActionSuspendingStateMutator<Action, State>,
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    SearchStateHolder,
    ActionSuspendingStateMutator<Action, State> by mutator {

    @AssistedInject
    constructor(
        navActions: (NavigationMutation) -> Unit,
        authRepository: AuthRepository,
        profileRepository: ProfileRepository,
        recordRepository: RecordRepository,
        timelineRepository: TimelineRepository,
        userDataRepository: UserDataRepository,
        writeQueue: WriteQueue,
        @Assisted scope: CoroutineScope,
        @Assisted route: Route,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = State.Immutable(
                searchBarText = route.query.initialSearchBarText,
                query = route.query,
                layout = route.query.initialLayout,
                searchStateHolders = scope.searchScreenStateHolders(
                    query = route.query,
                    isSignedIn = true,
                    existing = emptyList(),
                    profileRepository = profileRepository,
                    recordRepository = recordRepository,
                    timelineRepository = timelineRepository,
                ),
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
                    query = route.query,
                    authRepository = authRepository,
                    profileRepository = profileRepository,
                    recordRepository = recordRepository,
                    timelineRepository = timelineRepository,
                )
                launchTrendsMutations(
                    state = state,
                    recordRepository = recordRepository,
                )
                launchSuggestedStarterPackMutations(
                    state = state,
                    recordRepository = recordRepository,
                )
                launchSuggestedFeedGeneratorMutations(
                    state = state,
                    recordRepository = recordRepository,
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
                            profileRepository = profileRepository,
                        )
                        is Action.Filter -> action.flow.launchFilterMutations(
                            state = state,
                        )
                        is Action.FetchSuggestedProfiles -> action.flow.launchSuggestedProfilesMutations(
                            state = state,
                            profileRepository = profileRepository,
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
                        is Action.UpdatePresentation -> action.flow.launchUpdatePresentationMutations(
                            state = state,
                        )
                    }
                }
            },
        ),
        scope = scope,
    )
}

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
    query: RouteQuery,
    routeScope: CoroutineScope,
    authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    recordRepository: RecordRepository,
    timelineRepository: TimelineRepository,
) = authRepository.signedInUser
    .map { it != null }
    .distinctUntilChanged()
    .launchedCollect { isSignedIn ->
        state.searchStateHolders = routeScope.searchScreenStateHolders(
            query = query,
            isSignedIn = isSignedIn,
            existing = state.searchStateHolders,
            profileRepository = profileRepository,
            recordRepository = recordRepository,
            timelineRepository = timelineRepository,
        )
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
    recordRepository: RecordRepository,
) = recordRepository.trends().launchedCollect {
    state.trends = it
}

context(productionScope: CoroutineScope)
private fun launchSuggestedStarterPackMutations(
    state: State.SnapshotMutable,
    recordRepository: RecordRepository,
) = recordRepository.suggestedStarterPacks()
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
                cursor = Cursor.Initial(),
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
    recordRepository: RecordRepository,
) = recordRepository.suggestedFeeds().launchedCollect {
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
    profileRepository: ProfileRepository,
) = launchedCollectLatest { action ->
    profileRepository.suggestedProfiles(
        category = action.category,
    ).collect { suggestedProfiles ->
        state.categoriesToSuggestedProfiles += (action.category to suggestedProfiles)
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.Search>.launchSearchQueryMutations(
    state: State.SnapshotMutable,
    profileRepository: ProfileRepository,
) {
    val shared = shareIn(
        scope = productionScope,
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
                val currentQuery = state.query.queryString(
                    searchBarText = state.searchBarText,
                )
                state.searchStateHolders.loadAround(
                    query = currentQuery,
                    filter = state.appliedFilter ?: SearchFilter(),
                )
                state.layout = ScreenLayout.GeneralSearchResults
            }
        }
    }
    shared
        .filterIsInstance<Action.Search.OnSearchQueryChanged>()
        .debounce(300.milliseconds)
        .flatMapLatest {
            profileRepository.autoCompleteProfileSearch(
                query = ProfileSearchQuery(
                    query = it.query,
                    data = defaultSearchQueryData(),
                ),
                cursor = Cursor.Initial(),
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
private fun Flow<Action.UpdatePresentation>.launchUpdatePresentationMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.preferredPresentation = action.presentation
}

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

private fun CoroutineScope.searchScreenStateHolders(
    query: RouteQuery,
    isSignedIn: Boolean,
    existing: List<SearchScreenStateHolders>,
    profileRepository: ProfileRepository,
    recordRepository: RecordRepository,
    timelineRepository: TimelineRepository,
): List<SearchScreenStateHolders> {
    val existingByKey = existing.associateBy(SearchScreenStateHolders::key)
    return buildList {
        if (isSignedIn) listOf(
            Timeline.Source.Search.Sort.Top,
            Timeline.Source.Search.Sort.Latest,
        ).forEach { sort ->
            add(
                existingByKey[sort.postTabKey]
                    ?: SearchScreenStateHolders.Posts(
                        sort = sort,
                        mutator = timelineStateHolder(
                            refreshOnStart = false,
                            timeline = Timeline.Search.stub(
                                search = Timeline.Source.Search.OneOff(
                                    query = query.initialQueryString,
                                    filter = SearchFilter(),
                                    sort = sort,
                                ),
                            ),
                            startNumColumns = 1,
                            timelineRepository = timelineRepository,
                        ),
                    ),
            )
        }
        if (query.supportsNonPostSearch) {
            add(
                existingByKey["profiles"]
                    ?: SearchScreenStateHolders.Profiles(
                        mutator = profileSearchStateHolder(
                            query = query,
                            profileRepository = profileRepository,
                        ),
                    ),
            )
            add(
                existingByKey["feed-generators"]
                    ?: SearchScreenStateHolders.Feeds(
                        mutator = feedGeneratorSearchStateHolder(
                            query = query,
                            recordRepository = recordRepository,
                        ),
                    ),
            )
        }
    }
}

private val Timeline.Source.Search.Sort.postTabKey: String
    get() = when (this) {
        Timeline.Source.Search.Sort.Top -> "top-posts"
        Timeline.Source.Search.Sort.Latest -> "latest-posts"
    }

private fun CoroutineScope.profileSearchStateHolder(
    query: RouteQuery,
    profileRepository: ProfileRepository,
): SearchResultStateHolder = actionSuspendingStateMutator(
    state = SearchState.OfProfiles(
        tilingData = TilingState.Data(
            currentQuery = ProfileSearchQuery(
                query = query.initialQueryString,
                data = defaultSearchQueryData(),
            ),
        ),
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    producer = { holderState, actions ->
        actions.map { it.tilingAction }
            .launchTilingMutations(
                state = holderState,
                updateQueryData = { copy(data = it) },
                refreshQuery = { copy(data = data.reset()) },
                cursorListLoader = profileRepository::profileSearch
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

private fun CoroutineScope.feedGeneratorSearchStateHolder(
    query: RouteQuery,
    recordRepository: RecordRepository,
): SearchResultStateHolder = actionSuspendingStateMutator(
    state = SearchState.OfFeedGenerators(
        tilingData = TilingState.Data(
            currentQuery = FeedGeneratorSearchQuery(
                query = query.initialQueryString,
                data = defaultSearchQueryData(),
            ),
        ),
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    producer = { holderState, actions ->
        actions.map { it.tilingAction }
            .launchTilingMutations(
                state = holderState,
                updateQueryData = { copy(data = it) },
                refreshQuery = { copy(data = data.reset()) },
                cursorListLoader = recordRepository::feedGeneratorSearch
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

/**
 * Dispatches a fresh [TilingState.Action.LoadAround] into each tab holder. The post tabs re-tile
 * against a new [Timeline.Source.Search] carried by the [TimelineQuery]; because the cursor anchor
 * is newer the tiler tears down and re-fetches from the new source, so no holder is rebuilt.
 */
private fun List<SearchScreenStateHolders>.loadAround(
    query: String,
    filter: SearchFilter,
) = forEach { holder ->
    when (holder) {
        is SearchScreenStateHolders.Posts -> holder.accept(
            TimelineState.Action.Tile(
                tilingAction = TilingState.Action.LoadAround(
                    TimelineQuery(
                        data = defaultSearchQueryData(),
                        source = Timeline.Source.Search.OneOff(
                            query = query,
                            filter = filter,
                            sort = holder.sort,
                        ),
                    ),
                ),
            ),
        )

        is SearchScreenStateHolders.Profiles -> holder.accept(
            SearchState.Tile(
                tilingAction = TilingState.Action.LoadAround(
                    ProfileSearchQuery(
                        query = query,
                        data = defaultSearchQueryData(),
                    ),
                ),
            ),
        )

        is SearchScreenStateHolders.Feeds -> holder.accept(
            SearchState.Tile(
                tilingAction = TilingState.Action.LoadAround(
                    FeedGeneratorSearchQuery(
                        query = query,
                        data = defaultSearchQueryData(),
                    ),
                ),
            ),
        )
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.Filter>.launchFilterMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    when (action) {
        Action.Filter.Begin ->
            state.draftFilter = state.appliedFilter ?: SearchFilter()

        is Action.Filter.Edit ->
            state.draftFilter = action.filter

        is Action.Filter.Clear -> {
            state.appliedFilter = null
            state.draftFilter = SearchFilter()
            state.reloadPostSearches()
        }

        Action.Filter.Apply -> {
            state.appliedFilter = state.draftFilter
            state.reloadPostSearches()
        }
    }
}

/**
 * Re-runs the post searches with the currently [State.appliedFilter], and shows the
 * results. Shared by [Action.Filter.Apply] and [Action.Filter.Clear] so both keep the
 * displayed posts in sync with the applied filter instead of leaving stale results.
 */
private fun State.SnapshotMutable.reloadPostSearches() {
    searchStateHolders.loadAround(
        query = query.queryString(searchBarText = searchBarText),
        filter = appliedFilter ?: SearchFilter(),
    )
    layout = ScreenLayout.GeneralSearchResults
}

private fun defaultSearchQueryData() = CursorQuery.Data(
    page = 0,
    cursorAnchor = Clock.System.now(),
    limit = 15,
)
