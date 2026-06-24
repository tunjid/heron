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

package com.tunjid.heron.home

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.sourceId
import com.tunjid.heron.data.core.models.uri
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.SearchRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.data.utilities.writequeue.toSubscriptionWritable
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.state.timelineStateHolder
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.timeline.utilities.writeStatusMessage
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.take

internal typealias HomeStateHolder = ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualHomeViewModel
}

@Stable
@AssistedInject
class ActualHomeViewModel(
    authRepository: AuthRepository,
    searchRepository: SearchRepository,
    timelineRepository: TimelineRepository,
    userDataRepository: UserDataRepository,
    writeQueue: WriteQueue,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER")
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    HomeStateHolder by scope.actionSuspendingStateMutator(
        state = State().toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { state, actions ->
            launchTimelineMutations(
                state = state,
                viewModelScope = scope,
                timelineRepository = timelineRepository,
                userDataRepository = userDataRepository,
            )
            launchTrendsMutations(
                state = state,
                searchRepository = searchRepository,
            )
            launchLoadProfileMutations(
                state = state,
                authRepository = authRepository,
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
                    is Action.UpdatePageWithUpdates -> action.flow.collect { event ->
                        if (state.sourceIdsToHasUpdates[event.sourceId] != event.hasUpdates) {
                            state.sourceIdsToHasUpdates += (event.sourceId to event.hasUpdates)
                        }
                    }
                    is Action.TogglePublicationSubscription -> action.flow.launchTogglePublicationSubscriptionMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(state)

                    is Action.RefreshCurrentTab -> action.flow.launchTabRefreshMutations(
                        state = state,
                    )

                    is Action.UpdateTimeline -> action.flow.launchSaveTimelinePreferencesMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )

                    is Action.SetCurrentTab -> action.flow.launchSetCurrentTabMutations(
                        state = state,
                        userDataRepository = userDataRepository,
                    )
                    is Action.SetTabLayout -> action.flow.launchSetTabLayoutMutations(
                        state = state,
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
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private fun launchLoadProfileMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser.launchedCollect {
    state.signedInProfile = it
}

context(productionScope: CoroutineScope)
private fun launchTimelineMutations(
    state: State.SnapshotMutable,
    viewModelScope: CoroutineScope,
    timelineRepository: TimelineRepository,
    userDataRepository: UserDataRepository,
) = combine(
    userDataRepository.preferences.take(1),
    timelineRepository.homeTimelines,
    ::Pair,
).launchedCollect { (preferences, homeTimelines) ->
    val tabUri = state.currentTabUri
        ?: preferences
            .local
            .lastViewedHomeTimelineUri
            .takeIf { uri ->
                homeTimelines.any { it.isPinned && it.uri == uri }
            }
        ?: homeTimelines.firstOrNull()?.uri

    state.currentTabUri = tabUri
    state.timelines = homeTimelines
    state.timelineStateHolders = homeTimelines.map { timeline ->
        val holder = state.timelineStateHolders
            .firstOrNull { it.state.timeline.sourceId == timeline.sourceId }
            ?.mutator
            ?: viewModelScope.timelineStateHolder(
                initialItems = TimelineItem.LoadingItems,
                refreshOnStart = preferences.local.refreshHomeTimelineOnLaunch,
                timeline = timeline,
                startNumColumns = 1,
                timelineRepository = timelineRepository,
            )

        if (timeline.isPinned)
            HomeScreenStateHolders.Pinned(holder)
        else
            HomeScreenStateHolders.Saved(holder)
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

@OptIn(ExperimentalUuidApi::class)
context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateTimeline>.launchSaveTimelinePreferencesMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchedCollectLatest { action ->
    when (action) {
        Action.UpdateTimeline.RequestUpdate -> {
            state.timelinePreferenceSaveRequestId = Uuid.random().toHexString()
        }
        is Action.UpdateTimeline.Update -> {
            val writable = Writable.TimelineUpdate(Timeline.Update.Bulk(action.timelines))
            val status = writeQueue.enqueue(writable)
            val memo = writable.writeStatusMessage(status)
            if (memo != null) state.messages += memo
            writeQueue.awaitDequeue(writable)
            state.tabLayout = when (state.tabLayout) {
                TabLayout.Collapsed.All -> state.tabLayout
                TabLayout.Collapsed.Selected -> state.tabLayout
                TabLayout.Expanded -> TabLayout.Collapsed.Selected
            }
        }
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
private fun Flow<Action.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchedCollect { event ->
    state.messages -= event.message
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SetCurrentTab>.launchSetCurrentTabMutations(
    state: State.SnapshotMutable,
    userDataRepository: UserDataRepository,
) = launchedCollectLatest { action ->
    // Write to memory in state immediately
    state.currentTabUri = action.currentTabUri
    // Wait until we're sure the user has settled on this tab
    delay(1400.milliseconds)
    // Write to disk
    userDataRepository.setLastViewedHomeTimelineUri(action.currentTabUri)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SetTabLayout>.launchSetTabLayoutMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.tabLayout = action.layout
}

context(productionScope: CoroutineScope)
private fun Flow<Action.RefreshCurrentTab>.launchTabRefreshMutations(
    state: State.SnapshotMutable,
) = launchedCollect {
    state.timelineStateHolders
        .firstOrNull { it.state.timeline.uri == state.currentTabUri }
        ?.accept
        ?.invoke(
            TimelineState.Action.Tile(
                tilingAction = TilingState.Action.Refresh,
            ),
        )
}
