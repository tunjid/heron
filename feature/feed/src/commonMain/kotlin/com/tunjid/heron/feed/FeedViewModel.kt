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
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.TimelineRequest
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.feed.di.timelineRequest
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.timeline.state.timelineStateHolder
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge

internal typealias FeedStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualFeedViewModel
}

@Inject
class ActualFeedViewModel(
    navActions: (NavigationMutation) -> Unit,
    writeQueue: WriteQueue,
    timelineRepository: TimelineRepository,
    profileRepository: ProfileRepository,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    FeedStateHolder by scope.actionStateFlowMutator(
        initialState = State(route),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        actionTransform = transform@{ actions ->
            merge(
                timelineStateHolderMutations(
                    request = route.timelineRequest,
                    scope = scope,
                    timelineRepository = timelineRepository,
                    profileRepository = profileRepository,
                ),
                actions.toMutationStream(
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                            writeQueue = writeQueue,
                        )
                        is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations()

                        is Action.Navigate -> action.flow.consumeNavigationActions(
                            navigationMutationConsumer = navActions,
                        )
                    }
                },
            )
        },
    )

private fun SuspendingStateHolder<State>.timelineStateHolderMutations(
    request: TimelineRequest.OfFeed,
    scope: CoroutineScope,
    timelineRepository: TimelineRepository,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> = flow {
    val existingHolder = state().timelineStateHolder
    if (existingHolder != null) return@flow emitAll(
        merge(
            existingHolder.state.mapToMutation { copy(timelineState = it) },
            timelineCreatorMutations(
                timeline = existingHolder.state.value.timeline,
                profileRepository = profileRepository,
            ),
        ),
    )

    val timeline = timelineRepository.timeline(request)
        .first()
    val createdHolder = scope.timelineStateHolder(
        refreshOnStart = true,
        timeline = timeline,
        startNumColumns = 1,
        timelineRepository = timelineRepository,
    )
    emit {
        copy(timelineStateHolder = createdHolder)
    }
    emitAll(
        merge(
            createdHolder.state.mapToMutation { copy(timelineState = it) },
            timelineCreatorMutations(
                timeline = timeline,
                profileRepository = profileRepository,
            ),
        ),
    )
}

private fun Flow<Action.SendPostInteraction>.postInteractionMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        writeQueue.enqueue(Writable.Interaction(action.interaction))
    }

private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(messages = messages - action.message)
    }

private fun timelineCreatorMutations(
    timeline: Timeline,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> =
    when (timeline) {
        is Timeline.Home.Feed -> profileRepository.profile(
            profileId = timeline.feedGenerator.creator.did,
        )

        is Timeline.Home.Following -> emptyFlow()
        is Timeline.Home.List -> profileRepository.profile(
            profileId = timeline.feedList.creator.did,
        )

        is Timeline.Profile -> emptyFlow()
        is Timeline.StarterPack -> emptyFlow()
    }
        .mapToMutation {
            copy(creator = it)
        }
