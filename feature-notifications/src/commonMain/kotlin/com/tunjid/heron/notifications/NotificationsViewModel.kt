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

package com.tunjid.heron.notifications


import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.NotificationsQuery
import com.tunjid.heron.data.repository.NotificationsRepository
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.data.utilities.cursorListTiler
import com.tunjid.heron.data.utilities.cursorTileInputs
import com.tunjid.heron.data.utilities.hasDifferentAnchor
import com.tunjid.heron.data.utilities.isValidFor
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.toTiledList
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.datetime.Clock

internal typealias NotificationsStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualNotificationsViewModel
}

@Inject
class ActualNotificationsViewModel(
    navActions: (NavigationMutation) -> Unit,
    writeQueue: WriteQueue,
    authRepository: AuthRepository,
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
        lastRefreshedMutations(
            notificationsRepository
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
                is Action.Fetch -> action.flow.notificationsMutations(
                    stateHolder = this@transform,
                    notificationsRepository = notificationsRepository,
                )

                is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                    writeQueue = writeQueue,
                )

                is Action.MarkNotificationsRead -> action.flow.markNotificationsReadMutations(
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
    authRepository: AuthRepository,
): Flow<Mutation<State>> =
    authRepository.signedInUser.mapToMutation {
        copy(signedInProfile = it)
    }

fun lastRefreshedMutations(
    notificationsRepository: NotificationsRepository,
): Flow<Mutation<State>> =
    notificationsRepository.lastRefreshed.mapToMutation { refreshedAt ->
        copy(
            lastRefreshed = refreshedAt,
            isRefreshing =
                if (isRefreshing) refreshedAt == null || refreshedAt < currentQuery.data.cursorAnchor
                else isRefreshing
        )
    }

private fun Flow<Action.SendPostInteraction>.postInteractionMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        writeQueue.enqueue(Writable.Interaction(action.interaction))
    }

private fun Flow<Action.MarkNotificationsRead>.markNotificationsReadMutations(
    notificationsRepository: NotificationsRepository,
): Flow<Mutation<State>> =
    mapToManyMutations {
        notificationsRepository.markRead(it.at)
    }

suspend fun Flow<Action.Fetch>.notificationsMutations(
    stateHolder: SuspendingStateHolder<State>,
    notificationsRepository: NotificationsRepository,
): Flow<Mutation<State>> = scan(
    initial = MutableStateFlow(stateHolder.state().currentQuery)
) { queries, action ->
    // update backing states as a side effect
    when (action) {
        is Action.Fetch.LoadAround -> {
            if (!queries.value.hasDifferentAnchor(action.query))
                queries.value = action.query
        }

        is Action.Fetch.Refresh -> {
            queries.value = NotificationsQuery(
                data = queries.value.data.copy(
                    page = 0,
                    cursorAnchor = Clock.System.now(),
                ),
            )
        }
    }
    // Emit the same item with each action
    queries
}
    // Only emit once
    .distinctUntilChanged()
    .flatMapLatest { queries ->
        merge(
            queryMutations(queries),
            itemMutations(
                queries = queries,
                cursorListLoader = notificationsRepository::notifications
            ),
        )
    }

private fun queryMutations(queries: MutableStateFlow<NotificationsQuery>) =
    queries.mapToMutation<NotificationsQuery, State> { newQuery ->
        copy(
            currentQuery = newQuery,
            isRefreshing = if (currentQuery.hasDifferentAnchor(newQuery)) true else isRefreshing
        )
    }

private fun itemMutations(
    queries: Flow<NotificationsQuery>,
    cursorListLoader: (NotificationsQuery, Cursor) -> Flow<CursorList<Notification>>,
): Flow<Mutation<State>> {
    // Refreshes need to tear down the tiling pipeline all over
    val refreshes = queries.distinctUntilChangedBy {
        it.data.cursorAnchor
    }
    val updatePage: NotificationsQuery.(CursorQuery.Data) -> NotificationsQuery = {
        copy(data = it)
    }
    return refreshes.flatMapLatest { refreshedQuery ->
        cursorTileInputs<NotificationsQuery, Notification>(
            numColumns = flowOf(1),
            queries = queries,
            updatePage = updatePage,
        )
            .toTiledList(
                cursorListTiler(
                    startingQuery = refreshedQuery,
                    cursorListLoader = cursorListLoader,
                    updatePage = updatePage,
                )
            )
    }
        .mapToMutation { notifications ->
            if (notifications.isValidFor(currentQuery)) copy(
                notifications = notifications.distinctBy(Notification::cid)
            )
            else this
        }
}