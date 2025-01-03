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

package com.tunjid.heron.notifications


import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.repository.AuthTokenRepository
import com.tunjid.heron.data.repository.NotificationsQuery
import com.tunjid.heron.data.repository.NotificationsRepository
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.data.utilities.cursorListTiler
import com.tunjid.heron.data.utilities.cursorTileInputs
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
import com.tunjid.tiler.map
import com.tunjid.tiler.queries
import com.tunjid.tiler.toTiledList
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias NotificationsStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Inject
class NotificationsStateHolderCreator(
    private val creator: (scope: CoroutineScope, route: Route) -> ActualNotificationsStateHolder,
) : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualNotificationsStateHolder = creator.invoke(scope, route)
}

@Inject
class ActualNotificationsStateHolder(
    navActions: (NavigationMutation) -> Unit,
    authTokenRepository: AuthTokenRepository,
    notificationsRepository: NotificationsRepository,
    @Assisted
    scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER")
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope), NotificationsStateHolder by scope.actionStateFlowMutator(
    initialState = State(
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
        loadProfileMutations(
            authTokenRepository
        ),
    ),
    actionTransform = transform@{ actions ->
        actions.toMutationStream(
            keySelector = Action::key
        ) {
            when (val action = type()) {
                is Action.LoadAround -> action.flow.notificationsMutations(
                    stateHolder = this@transform,
                    notificationsRepository = notificationsRepository,
                )

                is Action.Navigate -> action.flow.consumeNavigationActions(
                    navigationMutationConsumer = navActions
                )
            }
        }
    }
)

private fun loadProfileMutations(
    authTokenRepository: AuthTokenRepository,
): Flow<Mutation<State>> =
    authTokenRepository.signedInUser.mapToMutation {
        copy(signedInProfile = it)
    }

suspend fun Flow<Action.LoadAround>.notificationsMutations(
    stateHolder: SuspendingStateHolder<State>,
    notificationsRepository: NotificationsRepository,
): Flow<Mutation<State>> {
    val startingQuery = stateHolder.state().currentQuery
    val updatePage: NotificationsQuery.(CursorQuery.Data) -> NotificationsQuery = {
        copy(data = it)
    }
    return cursorTileInputs<NotificationsQuery, Notification>(
        numColumns = flowOf(1),
        queries = map { it.query },
        updatePage = updatePage,
    )
        .toTiledList(
            cursorListTiler(
                startingQuery = startingQuery,
                cursorListLoader = notificationsRepository::notifications,
                updatePage = updatePage,
            )
        )
        .mapToMutation {
            if (it.queries().contains(currentQuery)) copy(notifications = it)
            else this
        }
}
