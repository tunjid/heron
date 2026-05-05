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

package com.tunjid.heron.feed

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.timelineRecordUri
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.TimelineRequest
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.repository.recentConversations
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.feed.di.timelineRequest
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.timeline.state.timelineStateHolder
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.coroutines.isNoOp
import com.tunjid.heron.ui.coroutines.launchAndCollect
import com.tunjid.heron.ui.coroutines.launchAndCollectLatest
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first

internal typealias FeedStateHolder = ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualFeedViewModel
}

@AssistedInject
class ActualFeedViewModel(
    navActions: (NavigationMutation) -> Unit,
    writeQueue: WriteQueue,
    authRepository: AuthRepository,
    messageRepository: MessageRepository,
    recordRepository: RecordRepository,
    timelineRepository: TimelineRepository,
    profileRepository: ProfileRepository,
    userDataRepository: UserDataRepository,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    FeedStateHolder by scope.actionSuspendingStateMutator(
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
            actions.launchMutationsIn(
                productionScope = this,
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )

                    is Action.UpdateFeedGeneratorStatus -> action.flow.feedGeneratorStatusMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )

                    is Action.ScrollToTop -> action.flow.scrollToTopMutations(state)
                    is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations(state)

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
private fun Flow<Action.UpdateRecentLists>.recentListsMutations(
    state: State.SnapshotMutable,
    recordRepository: RecordRepository,
) = launchAndCollectLatest {
    recordRepository.recentLists.collect { lists ->
        state.recentLists = lists
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
    request: TimelineRequest.OfFeed,
    viewModelScope: CoroutineScope,
    timelineRepository: TimelineRepository,
    profileRepository: ProfileRepository,
) {
    val existingHolder = state.timelineStateHolder
    if (existingHolder != null && !existingHolder.isNoOp()) {
        feedStatusMutations(
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

    val timeline = timelineRepository.timeline(request).first()
    val createdHolder = viewModelScope.timelineStateHolder(
        initialItems = TimelineItem.LoadingItems,
        refreshOnStart = true,
        timeline = timeline,
        startNumColumns = 1,
        timelineRepository = timelineRepository,
    )
    state.timelineStateHolder = createdHolder

    if (timeline !is Timeline.Home.Feed) return

    feedStatusMutations(
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

@OptIn(ExperimentalUuidApi::class)
context(productionScope: CoroutineScope)
private fun Flow<Action.ScrollToTop>.scrollToTopMutations(state: State.SnapshotMutable) =
    launchAndCollect {
        state.scrollToTopRequestId = Uuid.random().toString()
    }

private suspend fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(
    state: State.SnapshotMutable,
) = collect { event ->
    state.messages -= event.message
}

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateFeedGeneratorStatus>.feedGeneratorStatusMutations(
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
private fun timelineCreatorMutations(
    state: State.SnapshotMutable,
    timeline: Timeline,
    profileRepository: ProfileRepository,
) = timeline.withFeedTimelineOrNull { feedTimeline ->
    profileRepository.profile(
        profileId = feedTimeline.feedGenerator.creator.did,
    ).launchAndCollect {
        state.creator = it
    }
}

context(productionScope: CoroutineScope)
private fun feedStatusMutations(
    state: State.SnapshotMutable,
    timeline: Timeline,
    timelineRepository: TimelineRepository,
) = timeline.withFeedTimelineOrNull { feedTimeline ->
    timelineRepository.preferences
        .distinctUntilChangedBy { it.timelinePreferences }
        .launchAndCollect { preferences ->
            val pinned =
                preferences.timelinePreferences.firstOrNull {
                    it.timelineRecordUri == feedTimeline.feedGenerator.uri
                }?.pinned

            state.feedStatus = when (pinned) {
                true -> Timeline.Home.Status.Pinned
                false -> Timeline.Home.Status.Saved
                null -> Timeline.Home.Status.None
            }
        }
}

internal inline fun <T> Timeline.withFeedTimelineOrNull(
    block: (Timeline.Home.Feed) -> T,
) =
    if (this is Timeline.Home.Feed) block(this)
    else null
