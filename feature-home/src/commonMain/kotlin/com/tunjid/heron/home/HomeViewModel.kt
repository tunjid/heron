/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.home


import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.repository.TimelineQuery
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.domain.timeline.TimelineLoadAction
import com.tunjid.heron.domain.timeline.timelineLoadMutations
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.queries
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.Clock
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias HomeStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Inject
class HomeStateHolderCreator(
    private val creator: (scope: CoroutineScope, route: Route) -> ActualHomeStateHolder
) : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route
    ): ActualHomeStateHolder = creator.invoke(scope, route)
}

@Inject
class ActualHomeStateHolder(
    timelineRepository: TimelineRepository,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER")
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope), HomeStateHolder by scope.actionStateFlowMutator(
    initialState = State(
        currentQuery = TimelineQuery.Home(
            source = Constants.timelineFeed,
            data = TimelineQuery.Data(
                page = 0,
                firstRequestInstant = Clock.System.now(),
            )
        )
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
    ),
    actionTransform = transform@{ actions ->
        actions.toMutationStream(
            keySelector = Action::key
        ) {
            when (val action = type()) {
                is Action.LoadFeed -> action.flow.timelineLoadMutations(
                    stateHolder = this@transform,
                    timelineRepository = timelineRepository,
                )

                is Action.Navigate -> action.flow.consumeNavigationActions(
                    navigationMutationConsumer = navActions
                )
            }
        }
    }
)


/**
 * Feed mutations as a function of the user's scroll position
 */
private suspend fun Flow<Action.LoadFeed>.timelineLoadMutations(
    stateHolder: SuspendingStateHolder<State>,
    timelineRepository: TimelineRepository,
): Flow<Mutation<State>> = with(stateHolder) {
    // Read the starting state at the time of subscription
    val startingState = state()

    return map {
        when (it) {
            is Action.LoadFeed.GridSize -> TimelineLoadAction.GridSize(it.numColumns)
            is Action.LoadFeed.LoadAround -> TimelineLoadAction.LoadAround(it.query)
        }
    }
        .timelineLoadMutations(
            startQuery = startingState.currentQuery,
            startNumColumns = startingState.numColumns,
            cursorListLoader = timelineRepository::timeline,
            mutations = { queriesFlow, numColumnsFlow, tiledListFlow ->
                merge(
                    queriesFlow.mapToMutation { copy(currentQuery = it) },
                    numColumnsFlow.mapToMutation { copy(numColumns = it) },
                    tiledListFlow.mapToMutation { fetchedList ->
                        if (!fetchedList.queries().contains(currentQuery)) this
                        else copy(
                            feed = fetchedList.distinctBy(TimelineItem::id)
                        )
                    }
                )
            }
        )
}
