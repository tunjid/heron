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

package com.tunjid.heron.list

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.ListMember
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.timelineRecordUri
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.ListMemberQuery
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.data.repository.SearchRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.TimelineRequest
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.data.utilities.writequeue.toSubscriptionWritable
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.list.di.timelineRequest
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.launchTilingMutations
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.timeline.state.timelineStateHolder
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.stateproduction.RouteStateHolder
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.isNoOp
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
import com.tunjid.tiler.distinctBy
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take

@Stable
internal interface ListStateHolder :
    RouteStateHolder,
    ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface ListViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualListViewModel
}

@Stable
class ActualListViewModel(
    mutator: ActionSuspendingStateMutator<Action, State>,
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    ListStateHolder,
    ActionSuspendingStateMutator<Action, State> by mutator {

    @AssistedInject
    constructor(
        navActions: (NavigationMutation) -> Unit,
        writeQueue: WriteQueue,
        timelineRepository: TimelineRepository,
        profileRepository: ProfileRepository,
        recordRepository: RecordRepository,
        searchRepository: SearchRepository,
        authRepository: AuthRepository,
        userDataRepository: UserDataRepository,
        @Assisted scope: CoroutineScope,
        @Assisted route: Route,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = State(route).toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            producer = { state, actions ->
                launchSignedInProfileIdMutations(
                    state = state,
                    authRepository = authRepository,
                )
                launchLoadPreferencesMutations(
                    state = state,
                    userDataRepository = userDataRepository,
                )
                launchTimelineStateHolderMutations(
                    state = state,
                    request = route.timelineRequest,
                    viewModelScope = scope,
                    timelineRepository = timelineRepository,
                    profileRepository = profileRepository,
                )
                launchListMemberStateHolderMutations(
                    state = state,
                    request = route.timelineRequest,
                    viewModelScope = scope,
                    timelineRepository = timelineRepository,
                    recordRepository = recordRepository,
                    authRepository = authRepository,
                )

                actions.launchMutationsIn(
                    productionScope = this,
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.TogglePublicationSubscription -> action.flow.launchTogglePublicationSubscriptionMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                        is Action.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(state)

                        is Action.ToggleViewerState -> action.flow.launchToggleViewerStateMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                        is Action.UpdateFeedListStatus -> action.flow.launchFeedListStatusMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                        is Action.Navigate -> action.flow.collect { navAction ->
                            navActions(navAction.navigationMutation)
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
                        is Action.AddListMember -> action.flow.launchAddListMemberMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                        is Action.SearchProfiles -> action.flow.launchSearchMutations(
                            state = state,
                            searchRepository = searchRepository,
                        )
                        is Action.CurrentPageChanged -> action.flow.collect { event ->
                            state.isOnProfilesTab =
                                state.stateHolders.getOrNull(event.currentPage) is ListScreenStateHolders.Members
                        }
                    }
                }
            },
        ),
        scope = scope,
    )
}

context(productionScope: CoroutineScope)
private fun launchSignedInProfileIdMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser.launchedCollect { signedInProfile ->
    state.signedInProfileId = signedInProfile?.did
}

context(productionScope: CoroutineScope)
private fun launchLoadPreferencesMutations(
    state: State.SnapshotMutable,
    userDataRepository: UserDataRepository,
) = userDataRepository.preferences.launchedCollect {
    state.preferences = it
}

context(productionScope: CoroutineScope)
private fun launchTimelineStateHolderMutations(
    state: State.SnapshotMutable,
    request: TimelineRequest.OfList,
    viewModelScope: CoroutineScope,
    timelineRepository: TimelineRepository,
    profileRepository: ProfileRepository,
) {
    val existingHolder = state.existingTimelineStateHolder()

    if (existingHolder != null) {
        launchListStatusMutations(
            state = state,
            timeline = existingHolder.state.timeline,
            timelineRepository = timelineRepository,
        )
        launchTimelineCreatorMutations(
            state = state,
            timeline = existingHolder.state.timeline,
            profileRepository = profileRepository,
        )
        return
    }

    timelineRepository.timeline(request)
        .take(1)
        .launchedCollect { timeline ->
            if (state.existingTimelineStateHolder() != null) return@launchedCollect

            val createdHolder = ListScreenStateHolders.Timeline(
                mutator = viewModelScope.timelineStateHolder(
                    initialItems = TimelineItem.LoadingItems,
                    refreshOnStart = true,
                    timeline = timeline,
                    startNumColumns = 1,
                    timelineRepository = timelineRepository,
                ),
            )
            state.stateHolders = listOf(createdHolder) + state.stateHolders
                .filterIsInstance<ListScreenStateHolders.Members>()

            launchListStatusMutations(
                state = state,
                timeline = timeline,
                timelineRepository = timelineRepository,
            )
            launchTimelineCreatorMutations(
                state = state,
                timeline = timeline,
                profileRepository = profileRepository,
            )
        }
}

context(productionScope: CoroutineScope)
private fun launchListMemberStateHolderMutations(
    state: State.SnapshotMutable,
    request: TimelineRequest.OfList,
    viewModelScope: CoroutineScope,
    timelineRepository: TimelineRepository,
    recordRepository: RecordRepository,
    authRepository: AuthRepository,
) {
    val existingHolder = state.existingMembersStateHolder()

    if (existingHolder != null) return

    timelineRepository.timeline(request)
        .map { timeline ->
            if (timeline is Timeline.StarterPack) timeline.listTimeline else timeline
        }
        .filterIsInstance<Timeline.Home.List>()
        .take(1)
        .launchedCollect { timeline ->
            if (state.existingMembersStateHolder() != null) return@launchedCollect

            val createdHolder = ListScreenStateHolders.Members(
                mutator = viewModelScope.actionSuspendingStateMutator(
                    state = MemberState(
                        signedInProfileId = null,
                        listUri = timeline.feedList.uri,
                        tilingData = TilingState.Data(
                            currentQuery = ListMemberQuery(
                                listUri = timeline.feedList.uri,
                                data = defaultQueryData(),
                            ),
                        ),
                    ).toSnapshotMutable(),
                    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
                    producer = { memberState, actions ->
                        authRepository.signedInUser.launchedCollect {
                            memberState.signedInProfileId = it?.did
                        }
                        actions.launchTilingMutations(
                            state = memberState,
                            updateQueryData = { copy(data = it) },
                            refreshQuery = { copy(data = data.reset()) },
                            cursorListLoader = recordRepository::listMembers,
                            onNewItems = { items ->
                                items.distinctBy(ListMember::uri)
                            },
                        )
                    },
                ),
            )
            state.stateHolders = state.stateHolders
                .filterIsInstance<ListScreenStateHolders.Timeline>() + createdHolder
        }
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
private fun Flow<Action.AddListMember>.launchAddListMemberMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = {
        Writable.FeedList.AddMember(
            create = ListMember.Create(
                subjectId = it.profileId,
                listUri = it.listUri,
            ),
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
    toWritable = {
        Writable.Restriction(
            Profile.Restriction.Block.Add(
                signedInProfileId = it.signedInProfileId,
                profileId = it.profileId,
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
    toWritable = {
        Writable.Restriction(
            Profile.Restriction.Mute.Add(
                signedInProfileId = it.signedInProfileId,
                profileId = it.profileId,
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
    toWritable = { Writable.RecordDeletion(recordUri = it.recordUri) },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.SearchProfiles>.launchSearchMutations(
    state: State.SnapshotMutable,
    searchRepository: SearchRepository,
) = debounce(SEARCH_DEBOUNCE_MILLIS).launchedCollectLatest { action ->
    if (action.query.isBlank()) {
        state.suggestedProfiles = emptyList()
        return@launchedCollectLatest
    }
    searchRepository.autoCompleteProfileSearch(
        query = SearchQuery.OfProfiles(
            query = action.query,
            isLocalOnly = false,
            data = CursorQuery.Data(
                page = 0,
                cursorAnchor = Clock.System.now(),
                limit = MAX_SUGGESTED_PROFILES.toLong(),
            ),
        ),
        cursor = Cursor.Initial,
    ).collect { profiles ->
        state.suggestedProfiles = profiles.map(ProfileWithViewerState::profile)
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchedCollect { event ->
    state.messages -= event.message
}

private fun Action.ToggleViewerState.toConnectionWritable(): Writable.Connection =
    Writable.Connection(
        when (val following = this.following) {
            null -> Profile.Connection.Follow(
                signedInProfileId = signedInProfileId,
                profileId = viewedProfileId,
                followedBy = followedBy,
            )
            else -> Profile.Connection.Unfollow(
                signedInProfileId = signedInProfileId,
                profileId = viewedProfileId,
                followUri = following,
                followedBy = followedBy,
            )
        },
    )

context(productionScope: CoroutineScope)
private fun Flow<Action.ToggleViewerState>.launchToggleViewerStateMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { it.toConnectionWritable() },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateFeedListStatus>.launchFeedListStatusMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { Writable.TimelineUpdate(it.update) },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun launchListStatusMutations(
    state: State.SnapshotMutable,
    timeline: Timeline,
    timelineRepository: TimelineRepository,
) = timeline.withListTimelineOrNull { listTimeline ->
    timelineRepository.preferences
        .distinctUntilChangedBy { it.timelinePreferences }
        .launchedCollect { preferences ->
            val pinned =
                preferences.timelinePreferences.firstOrNull {
                    it.timelineRecordUri == listTimeline.feedList.uri
                }?.pinned

            state.listStatus = when (pinned) {
                true -> Timeline.Home.Status.Pinned
                false -> Timeline.Home.Status.Saved
                null -> Timeline.Home.Status.None
            }
        }
}

context(productionScope: CoroutineScope)
private fun launchTimelineCreatorMutations(
    state: State.SnapshotMutable,
    timeline: Timeline,
    profileRepository: ProfileRepository,
) {
    when (timeline) {
        is Timeline.Home.Feed,
        is Timeline.Home.Following,
        is Timeline.Profile,
        -> return

        is Timeline.Home.List -> profileRepository.profile(
            profileId = timeline.feedList.creator.did,
        )

        is Timeline.StarterPack -> profileRepository.profile(
            profileId = timeline.starterPack.creator.did,
        )
    }.launchedCollect {
        state.creator = it
    }
}

private fun State.SnapshotMutable.existingTimelineStateHolder(): ListScreenStateHolders.Timeline? =
    stateHolders
        .filterIsInstance<ListScreenStateHolders.Timeline>()
        .firstOrNull()
        .takeUnless { it?.mutator.isNoOp() }

private fun State.SnapshotMutable.existingMembersStateHolder(): ListScreenStateHolders.Members? =
    stateHolders
        .filterIsInstance<ListScreenStateHolders.Members>()
        .firstOrNull()

private fun defaultQueryData() = CursorQuery.Data(
    page = 0,
    cursorAnchor = Clock.System.now(),
    limit = 15,
)

internal inline fun <T> Timeline.withListTimelineOrNull(
    block: (Timeline.Home.List) -> T,
) =
    if (this is Timeline.Home.List) block(this)
    else null

private const val SEARCH_DEBOUNCE_MILLIS = 300L
private const val MAX_SUGGESTED_PROFILES = 5
