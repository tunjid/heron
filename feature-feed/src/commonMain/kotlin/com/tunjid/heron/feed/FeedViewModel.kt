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
import com.tunjid.heron.data.core.models.UriLookup
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.domain.timeline.timelineStateHolder
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.feed.di.feedLookup
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias FeedStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Inject
class FeedStateHolderCreator(
    private val creator: (scope: CoroutineScope, route: Route) -> ActualFeedViewModel,
) : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualFeedViewModel = creator.invoke(scope, route)
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
) : ViewModel(viewModelScope = scope), FeedStateHolder by scope.actionStateFlowMutator(
    initialState = State(),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    actionTransform = transform@{ actions ->
        merge(
            timelineStateHolderMutations(
                lookup = route.feedLookup,
                scope = scope,
                timelineRepository = timelineRepository,
                profileRepository = profileRepository,
            ),
            actions.toMutationStream(
                keySelector = Action::key
            ) {
                when (val action = type()) {
                    is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                        writeQueue = writeQueue,
                    )

                    is Action.Navigate -> action.flow.consumeNavigationActions(
                        navigationMutationConsumer = navActions
                    )
                }
            }
        )
    }
)

private fun SuspendingStateHolder<State>.timelineStateHolderMutations(
    lookup: UriLookup.Timeline,
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
            )
        )
    )

    val timeline = timelineRepository.lookupTimeline(lookup)
        .first()
    val createdHolder = timelineStateHolder(
        timeline = timeline,
        startNumColumns = 1,
        scope = scope,
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
            )
        )
    )
}

private fun Flow<Action.SendPostInteraction>.postInteractionMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        writeQueue.enqueue(Writable.Interaction(action.interaction))
    }

private fun timelineCreatorMutations(
    timeline: Timeline,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> =
    when (timeline) {
        is Timeline.Home.Feed -> profileRepository.profile(
            profileId = timeline.feedGenerator.creatorId
        )

        is Timeline.Home.Following -> emptyFlow()
        is Timeline.Home.List -> profileRepository.profile(
            profileId = timeline.feedList.creatorId
        )

        is Timeline.Profile -> emptyFlow()
    }
        .mapToMutation {
            copy(creator = it)
        }
