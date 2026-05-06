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
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.data.repository.SearchRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.TimelineRequest
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.repository.recentConversations
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.list.di.timelineRequest
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.tiling.tilingMutations
import com.tunjid.heron.timeline.state.timelineStateHolder
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.coroutines.isNoOp
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal typealias ListStateHolder = ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualListViewModel
}

@AssistedInject
class ActualListViewModel(
    navActions: (NavigationMutation) -> Unit,
    writeQueue: WriteQueue,
    messageRepository: MessageRepository,
    timelineRepository: TimelineRepository,
    profileRepository: ProfileRepository,
    recordRepository: RecordRepository,
    searchRepository: SearchRepository,
    authRepository: AuthRepository,
    userDataRepository: UserDataRepository,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    ListStateHolder by scope.actionSuspendingStateMutator(
        initialState = State(route).toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { state, actions ->
            signedInProfileIdMutations(
                state = state,
                authRepository = authRepository,
            )
            loadPreferencesMutations(
                state = state,
                userDataRepository = userDataRepository,
            )
            timelineStateHolderMutations(
                state = state,
                request = route.timelineRequest,
                viewModelScope = scope,
                timelineRepository = timelineRepository,
                profileRepository = profileRepository,
            )
            listMemberStateHolderMutations(
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
                    is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations(state)

                    is Action.ToggleViewerState -> action.flow.toggleViewerStateMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.UpdateFeedListStatus -> action.flow.feedListStatusMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.Navigate -> action.flow.collect { navAction ->
                        navActions(navAction.navigationMutation)
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
                    is Action.AddListMember -> action.flow.addListMemberMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.SearchProfiles -> action.flow.searchMutations(
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
    )

context(productionScope: CoroutineScope)
private fun signedInProfileIdMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser.launchAndCollect { signedInProfile ->
    state.signedInProfileId = signedInProfile?.did
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
private suspend fun timelineStateHolderMutations(
    state: State.SnapshotMutable,
    request: TimelineRequest.OfList,
    viewModelScope: CoroutineScope,
    timelineRepository: TimelineRepository,
    profileRepository: ProfileRepository,
) {
    val existingHolder = state.stateHolders
        .filterIsInstance<ListScreenStateHolders.Timeline>()
        .firstOrNull()
        .takeUnless { it?.mutator.isNoOp() }

    if (existingHolder != null) {
        listStatusMutations(
            state = state,
            timeline = existingHolder.state.timeline,
            timelineRepository = timelineRepository,
        )
        timelineCreatorMutations(
            state = state,
            timeline = existingHolder.state.timeline,
            profileRepository = profileRepository,
        )
        return
    }

    val timeline = timelineRepository.timeline(request)
        .first()

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

    listStatusMutations(
        state = state,
        timeline = timeline,
        timelineRepository = timelineRepository,
    )
    timelineCreatorMutations(
        state = state,
        timeline = timeline,
        profileRepository = profileRepository,
    )
}

private suspend fun listMemberStateHolderMutations(
    state: State.SnapshotMutable,
    request: TimelineRequest.OfList,
    viewModelScope: CoroutineScope,
    timelineRepository: TimelineRepository,
    recordRepository: RecordRepository,
    authRepository: AuthRepository,
) {
    val existingHolder = state.stateHolders
        .filterIsInstance<ListScreenStateHolders.Members>()
        .firstOrNull()

    if (existingHolder != null) return

    val timeline = timelineRepository.timeline(request)
        .map { timeline ->
            if (timeline is Timeline.StarterPack) timeline.listTimeline else timeline
        }
        .filterIsInstance<Timeline.Home.List>()
        .first()

    val createdHolder = ListScreenStateHolders.Members(
        mutator = viewModelScope.actionSuspendingStateMutator(
            initialState = MemberState(
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
                authRepository.signedInUser.launchAndCollect {
                    memberState.signedInProfileId = it?.did
                }
                actions.tilingMutations(
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
    state.stateHolders += createdHolder
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SendPostInteraction>.postInteractionMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { Writable.Interaction(it.interaction) },
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
private fun Flow<Action.AddListMember>.addListMemberMutations(
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
private fun Flow<Action.BlockAccount>.blockAccountMutations(
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
private fun Flow<Action.MuteAccount>.muteAccountMutations(
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
private fun Flow<Action.DeleteRecord>.deleteRecordMutations(
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
private fun Flow<Action.SearchProfiles>.searchMutations(
    state: State.SnapshotMutable,
    searchRepository: SearchRepository,
) = debounce(SEARCH_DEBOUNCE_MILLIS).launchAndCollectLatest { action ->
    if (action.query.isBlank()) {
        state.suggestedProfiles = emptyList()
        return@launchAndCollectLatest
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
private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchAndCollect { event ->
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
private fun Flow<Action.ToggleViewerState>.toggleViewerStateMutations(
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
private fun Flow<Action.UpdateFeedListStatus>.feedListStatusMutations(
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
private fun Flow<Action.UpdateRecentLists>.recentListsMutations(
    state: State.SnapshotMutable,
    recordRepository: RecordRepository,
) = launchAndCollectLatest {
    recordRepository.recentLists.collect { lists ->
        state.recentLists = lists
    }
}

context(productionScope: CoroutineScope)
private fun listStatusMutations(
    state: State.SnapshotMutable,
    timeline: Timeline,
    timelineRepository: TimelineRepository,
) = timeline.withListTimelineOrNull { listTimeline ->
    timelineRepository.preferences
        .distinctUntilChangedBy { it.timelinePreferences }
        .launchAndCollect { preferences ->
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
private fun timelineCreatorMutations(
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
    }.launchAndCollect {
        state.creator = it
    }
}

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
