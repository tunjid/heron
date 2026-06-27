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

package com.tunjid.heron.moderation

import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.coroutines.RouteViewModel
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal typealias ModerationStateHolder = ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface ModerationViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualModerationViewModel
}

@AssistedInject
class ActualModerationViewModel(
    authRepository: AuthRepository,
    timelineRepository: TimelineRepository,
    recordRepository: RecordRepository,
    userDataRepository: UserDataRepository,
    writeQueue: WriteQueue,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Assisted route: Route,
) : RouteViewModel(scope, route),
    ModerationStateHolder by scope.actionSuspendingStateMutator(
        state = State().toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { state, actions ->
            launchAdultContentAndGlobalLabelPreferenceMutations(
                state = state,
                timelineRepository = timelineRepository,
            )
            launchSubscribedLabelerMutations(
                state = state,
                recordRepository = recordRepository,
            )
            launchLoadPreferenceMutations(
                state = state,
                userDataRepository = userDataRepository,
            )
            actions.launchMutationsIn(
                productionScope = this,
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.UpdateAdultLabelVisibility -> action.flow.launchUpdateGlobalLabelMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.UpdateAdultContentPreferences -> action.flow.launchUpdateAdultContentPreferencesMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(
                        state = state,
                    )

                    is Action.Navigate -> action.flow.collect {
                        navActions(it.navigationMutation)
                    }
                    is Action.UpdateThreadGates -> action.flow.launchUpdateThreadGateMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    Action.SignOut -> action.flow.collect {
                        authRepository.signOut()
                    }
                }
            }
        },
    )

context(productionScope: CoroutineScope)
fun launchAdultContentAndGlobalLabelPreferenceMutations(
    state: State.SnapshotMutable,
    timelineRepository: TimelineRepository,
) = timelineRepository.preferences
    .map { it.allowAdultContent to it.contentLabelPreferences }
    .distinctUntilChanged()
    .launchedCollect { (allowAdultContent, contentLabelPreferences) ->
        state.adultContentEnabled = allowAdultContent
        state.adultLabelItems = adultLabels(contentLabelPreferences)
    }

context(productionScope: CoroutineScope)
fun launchSubscribedLabelerMutations(
    state: State.SnapshotMutable,
    recordRepository: RecordRepository,
) = recordRepository.subscribedLabelers
    .launchedCollect {
        state.subscribedLabelers = it
    }

context(productionScope: CoroutineScope)
private fun launchLoadPreferenceMutations(
    state: State.SnapshotMutable,
    userDataRepository: UserDataRepository,
) = userDataRepository.preferences
    .launchedCollect {
        state.preferences = it
    }

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateThreadGates>.launchUpdateThreadGateMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = {
        Writable.TimelineUpdate(
            Timeline.Update.OfInteractionSettings(
                preference = it.preference,
            ),
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateAdultLabelVisibility>.launchUpdateGlobalLabelMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
        Writable.TimelineUpdate(
            Timeline.Update.OfContentLabel.AdultLabelVisibilityChange(
                label = action.adultLabel,
                visibility = action.visibility,
            ),
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateAdultContentPreferences>.launchUpdateAdultContentPreferencesMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
        Writable.TimelineUpdate(
            Timeline.Update.OfAdultContent(
                enabled = action.adultContentEnabled,
            ),
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.messages -= action.message
}
