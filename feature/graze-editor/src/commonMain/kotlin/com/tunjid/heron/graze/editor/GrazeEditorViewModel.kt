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

package com.tunjid.heron.graze.editor

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
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
import kotlinx.coroutines.flow.merge

internal typealias GrazeEditorStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualGrazeEditorViewModel
}

@AssistedInject
class ActualGrazeEditorViewModel(
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    GrazeEditorStateHolder by scope.actionStateFlowMutator(
        initialState = State(route),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        actionTransform = transform@{ actions ->
            merge(
                actions.toMutationStream(
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.Navigate -> action.flow.consumeNavigationActions(
                            navigationMutationConsumer = navActions,
                        )
                        is Action.EditorNavigation -> action.flow.editorNavigationMutations()
                        is Action.EditFilter -> action.flow.editFilterFilterMutations()
                    }
                },
            )
        },
    )

private fun Flow<Action.EditorNavigation>.editorNavigationMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        when (action) {
            is Action.EditorNavigation.EnterFilter ->
                copy(currentPath = currentPath + action.index)
            Action.EditorNavigation.ExitFilter ->
                if (currentPath.isEmpty()) this
                else copy(currentPath = currentPath.dropLast(1))
        }
    }

private fun Flow<Action.EditFilter>.editFilterFilterMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(
            filter = filter.updateAt(action.path) { target ->
                if (action is Action.EditFilter.FlipRootFilter) when (target) {
                    is Filter.And -> Filter.Or(
                        id = target.id,
                        filters = target.filters,
                    )
                    is Filter.Or -> Filter.And(
                        id = target.id,
                        filters = target.filters,
                    )
                }
                else target.updateFilters { filters ->
                    when (action) {
                        is Action.EditFilter.AddFilter -> filters + action.filter
                        is Action.EditFilter.RemoveFilter -> filters.filterIndexed { index, _ ->
                            index != action.index
                        }
                        is Action.EditFilter.UpdateFilter -> filters.mapIndexed { index, filter ->
                            if (index == action.index) action.filter
                            else filter
                        }
                        is Action.EditFilter.FlipRootFilter -> throw IllegalArgumentException(
                            "Flip action should not operate on non root filters",
                        )
                    }
                }
            },
        )
    }

private fun Filter.Root.updateAt(
    path: List<Int>,
    update: (Filter.Root) -> Filter.Root,
): Filter.Root {
    if (path.isEmpty()) return update(this)
    val index = path.first()
    // This cast should be safe if path logic is correct
    val child = filters[index] as Filter.Root
    val updatedChild = child.updateAt(path.drop(1), update)

    val newFilters = filters.toMutableList()
    newFilters[index] = updatedChild

    return when (this) {
        is Filter.And -> copy(filters = newFilters)
        is Filter.Or -> copy(filters = newFilters)
    }
}

private inline fun Filter.Root.updateFilters(
    update: (List<Filter>) -> List<Filter>,
): Filter.Root {
    val updatedFilters = update(filters)
    return when (this) {
        is Filter.And -> copy(filters = updatedFilters)
        is Filter.Or -> copy(filters = updatedFilters)
    }
}
