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

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.EmbeddableRecordRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.timeline.utilities.writeStatusMessage
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal typealias ModerationStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualModerationViewModel
}

@AssistedInject
class ActualModerationViewModel(
    authRepository: AuthRepository,
    timelineRepository: TimelineRepository,
    embeddableRecordRepository: EmbeddableRecordRepository,
    userDataRepository: UserDataRepository,
    writeQueue: WriteQueue,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER")
    @Assisted route: Route,
) : ViewModel(viewModelScope = scope),
    ModerationStateHolder by scope.actionStateFlowMutator(
        initialState = State(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        inputs = listOf(
            adultContentAndGlobalLabelPreferenceMutations(
                timelineRepository = timelineRepository,
            ),
            subscribedLabelerMutations(
                embeddableRecordRepository = embeddableRecordRepository,
            ),
            loadPreferenceMutations(
                userDataRepository = userDataRepository,
            ),
        ),
        actionTransform = transform@{ actions ->
            actions.toMutationStream(
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.UpdateAdultLabelVisibility -> action.flow.updateGlobalLabelMutations(
                        writeQueue = writeQueue,
                    )
                    is Action.UpdateAdultContentPreferences -> action.flow.updateAdultContentPreferencesMutations(
                        writeQueue = writeQueue,
                    )
                    is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations()

                    is Action.Navigate -> action.flow.consumeNavigationActions(
                        navigationMutationConsumer = navActions,
                    )
                    is Action.UpdateMutedWord -> action.flow.updateMutedWordMutations(
                        writeQueue = writeQueue,
                    )
                    is Action.UpdateThreadGates -> action.flow.updateThreadGateMutations(
                        writeQueue = writeQueue,
                    )
                    Action.SignOut -> action.flow.mapToManyMutations {
                        authRepository.signOut()
                    }
                }
            }
        },
    )

fun adultContentAndGlobalLabelPreferenceMutations(
    timelineRepository: TimelineRepository,
): Flow<Mutation<State>> =
    timelineRepository.preferences
        .map { it.allowAdultContent to it.contentLabelPreferences }
        .distinctUntilChanged()
        .mapToMutation { (allowAdultContent, contentLabelPreferences) ->
            copy(
                adultContentEnabled = allowAdultContent,
                adultLabelItems = adultLabels(contentLabelPreferences),
            )
        }

fun subscribedLabelerMutations(
    embeddableRecordRepository: EmbeddableRecordRepository,
): Flow<Mutation<State>> =
    embeddableRecordRepository.subscribedLabelers
        .mapToMutation {
            copy(subscribedLabelers = it)
        }

private fun loadPreferenceMutations(
    userDataRepository: UserDataRepository,
): Flow<Mutation<State>> =
    userDataRepository.preferences
        .mapToMutation {
            copy(preferences = it)
        }

private fun Flow<Action.UpdateMutedWord>.updateMutedWordMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = mapToManyMutations {
    val writable = Writable.TimelineUpdate(
        Timeline.Update.OfMutedWord.ReplaceAll(
            mutedWordPreferences = it.mutedWordPreference,
        ),
    )
    val status = writeQueue.enqueue(writable)
    writable.writeStatusMessage(status)?.let {
        emit { copy(messages = messages + it) }
    }
}

private fun Flow<Action.UpdateThreadGates>.updateThreadGateMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = mapToManyMutations {
    val writable = Writable.TimelineUpdate(
        Timeline.Update.OfInteractionSettings(
            preference = it.preference,
        ),
    )
    val status = writeQueue.enqueue(writable)
    writable.writeStatusMessage(status)?.let {
        emit { copy(messages = messages + it) }
    }
}

private fun Flow<Action.UpdateAdultLabelVisibility>.updateGlobalLabelMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        val writable = Writable.TimelineUpdate(
            Timeline.Update.OfContentLabel.AdultLabelVisibilityChange(
                label = action.adultLabel,
                visibility = action.visibility,
            ),
        )
        val status = writeQueue.enqueue(writable)
        writable.writeStatusMessage(status)?.let {
            emit { copy(messages = messages + it) }
        }
    }

private fun Flow<Action.UpdateAdultContentPreferences>.updateAdultContentPreferencesMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        val writable = Writable.TimelineUpdate(
            Timeline.Update.OfAdultContent(
                enabled = action.adultContentEnabled,
            ),
        )
        val status = writeQueue.enqueue(writable)
        writable.writeStatusMessage(status)?.let {
            emit { copy(messages = messages + it) }
        }
    }

private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(messages = messages - action.message)
    }
