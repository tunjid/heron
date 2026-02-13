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
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.data.graze.GrazeFeed
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.data.repository.SearchRepository
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
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
    searchRepository: SearchRepository,
    recordRepository: RecordRepository,
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
                        is Action.SearchProfiles -> action.flow.searchMutations(
                            searchRepository = searchRepository,
                        )
                        is Action.Save -> action.flow.saveMutations(
                            recordRepository = recordRepository,
                        )
                        is Action.EditorNavigation -> action.flow.editorNavigationMutations()
                        is Action.EditFilter -> action.flow.editFilterFilterMutations()
                    }
                },
            )
        },
    )

private fun Flow<Action.SearchProfiles>.searchMutations(
    searchRepository: SearchRepository,
): Flow<Mutation<State>> =
    debounce(SEARCH_DEBOUNCE_MILLIS)
        .flatMapLatest { action ->
            searchRepository.autoCompleteProfileSearch(
                query = SearchQuery.OfProfiles(
                    query = action.query,
                    isLocalOnly = false,
                    data = CursorQuery.Data(
                        page = 0,
                        cursorAnchor = Clock.System.now(),
                        limit = MAX_SUGGESTED_PROFILES.toLong(),
                    ),
                ),
                cursor = Cursor.Initial,
            ).mapToMutation { profiles ->
                copy(
                    suggestedProfiles = profiles.map(ProfileWithViewerState::profile),
                )
            }
        }

private fun Flow<Action.Save>.saveMutations(
    recordRepository: RecordRepository,
): Flow<Mutation<State>> =
    mapLatestToManyMutations { action ->
        recordRepository.updateGrazeFeed(
            when (val feed = action.feed) {
                is GrazeFeed.Created -> GrazeFeed.Update.Edit(
                    feed = feed,
                )
                is GrazeFeed.Pending -> GrazeFeed.Update.Create(
                    feed = feed,
                )
            },
        )
    }

private fun Flow<Action.EditorNavigation>.editorNavigationMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        when (action) {
            is Action.EditorNavigation.EnterFilter ->
                copy(
                    suggestedProfiles = emptyList(),
                    currentPath = currentPath + action.index,
                )
            Action.EditorNavigation.ExitFilter ->
                if (currentPath.isEmpty()) copy(
                    suggestedProfiles = emptyList(),
                )
                else copy(
                    suggestedProfiles = emptyList(),
                    currentPath = currentPath.dropLast(1),
                )
        }
    }

private fun Flow<Action.EditFilter>.editFilterFilterMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        val editedFilter = feed.filter.updateAt(action.path) { target ->
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
        }
        copy(
            suggestedProfiles = emptyList(),
            feed = when (val currentFeed = feed) {
                is GrazeFeed.Created -> currentFeed.copy(filter = editedFilter)
                is GrazeFeed.Pending -> currentFeed.copy(filter = editedFilter)
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

private const val SEARCH_DEBOUNCE_MILLIS = 300L
const val MAX_SUGGESTED_PROFILES = 5
