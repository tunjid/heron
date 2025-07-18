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


import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.domain.timeline.TilingState
import com.tunjid.heron.domain.timeline.TimelineLoadAction
import com.tunjid.heron.domain.timeline.tilingAction
import com.tunjid.heron.domain.timeline.update
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal typealias HomeStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualHomeViewModel
}

@Inject
class ActualHomeViewModel(
    authRepository: AuthRepository,
    timelineRepository: TimelineRepository,
    writeQueue: WriteQueue,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER")
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope), HomeStateHolder by scope.actionStateFlowMutator(
    initialState = State(),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
        timelineMutations(
            startNumColumns = 1,
            scope = scope,
            timelineRepository = timelineRepository,
        ),
        loadProfileMutations(
            authRepository
        ),
    ),
    actionTransform = transform@{ actions ->
        actions.toMutationStream(
            keySelector = Action::key
        ) {
            when (val action = type()) {
                is Action.UpdatePageWithUpdates -> action.flow.pageWithUpdateMutations()
                is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                    writeQueue = writeQueue,
                )

                is Action.RefreshCurrentTab -> action.flow.tabRefreshMutations(
                    stateHolder = this@transform,
                )

                is Action.UpdateTimeline -> action.flow.saveTimelinePreferencesMutations(
                    writeQueue = writeQueue,
                )

                is Action.SetCurrentTab -> action.flow.setCurrentTabMutations()
                is Action.SetPreferencesExpanded -> action.flow.setPreferencesExpanded()
                is Action.Navigate -> action.flow.consumeNavigationActions(
                    navigationMutationConsumer = navActions
                )
            }
        }
    }
)

private fun loadProfileMutations(
    authRepository: AuthRepository,
): Flow<Mutation<State>> =
    authRepository.signedInUser.mapToMutation {
        copy(signedInProfile = it)
    }

private fun timelineMutations(
    startNumColumns: Int,
    scope: CoroutineScope,
    timelineRepository: TimelineRepository,
): Flow<Mutation<State>> =
    timelineRepository.homeTimelines().mapToMutation { homeTimelines ->
        copy(
            currentSourceId = currentSourceId
                ?: homeTimelines
                    .firstOrNull()
                    ?.sourceId,
            timelines = homeTimelines,
            timelineStateHolders = timelineStateHolders.update(
                updatedTimelines = homeTimelines,
                scope = scope,
                refreshOnStart = false,
                startNumColumns = startNumColumns,
                timelineRepository = timelineRepository,
            )
        )
    }

private fun Flow<Action.UpdatePageWithUpdates>.pageWithUpdateMutations(): Flow<Mutation<State>> =
    mapToMutation { (sourceId, hasUpdates) ->
        copy(sourceIdsToHasUpdates = sourceIdsToHasUpdates + (sourceId to hasUpdates))
    }

@OptIn(ExperimentalUuidApi::class)
private fun Flow<Action.UpdateTimeline>.saveTimelinePreferencesMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapLatestToManyMutations {
        when (it) {
            Action.UpdateTimeline.RequestUpdate -> emit {
                copy(timelinePreferenceSaveRequestId = Uuid.random().toHexString())
            }

            is Action.UpdateTimeline.Update -> {
                val writable = Writable.TimelineUpdate(it.timelines)
                writeQueue.enqueue(writable)
                writeQueue.awaitDequeue(writable)
                emit {
                    copy(timelinePreferencesExpanded = false)
                }
            }
        }
    }


private fun Flow<Action.SendPostInteraction>.postInteractionMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        writeQueue.enqueue(Writable.Interaction(action.interaction))
    }

private fun Flow<Action.SetCurrentTab>.setCurrentTabMutations(
): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(currentSourceId = action.sourceId)
    }

private fun Flow<Action.SetPreferencesExpanded>.setPreferencesExpanded(
): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(timelinePreferencesExpanded = action.isExpanded)
    }

private fun Flow<Action.RefreshCurrentTab>.tabRefreshMutations(
    stateHolder: SuspendingStateHolder<State>,
): Flow<Mutation<State>> =
    mapToManyMutations {
        val currentState = stateHolder.state()
        (0..<currentState.timelineStateHolders.size)
            .map(currentState.timelineStateHolders::stateHolderAt)
            .firstOrNull { it.state.value.timeline.sourceId == currentState.currentSourceId }
            ?.tilingAction(
                tilingAction = TilingState.Action.Refresh,
                stateHolderAction = TimelineLoadAction::Tile,
            )
    }