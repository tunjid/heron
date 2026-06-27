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

import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.recordUriOrNull
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.data.graze.GrazeFeed
import com.tunjid.heron.data.graze.GrazeFeed.Update.Create
import com.tunjid.heron.data.graze.GrazeFeed.Update.Delete
import com.tunjid.heron.data.graze.GrazeFeed.Update.Edit
import com.tunjid.heron.data.graze.GrazeFeed.Update.Get
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.data.repository.SearchRepository
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.graze.editor.di.initialLoad
import com.tunjid.heron.ui.coroutines.RouteViewModel
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.error_deleting_graze_feed
import heron.feature.graze_editor.generated.resources.error_fetching_graze_feed
import heron.feature.graze_editor.generated.resources.error_saving_graze_feed
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart

internal typealias GrazeEditorStateHolder = ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface GrazeEditorViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualGrazeEditorViewModel
}

@AssistedInject
class ActualGrazeEditorViewModel(
    navActions: (NavigationMutation) -> Unit,
    searchRepository: SearchRepository,
    recordRepository: RecordRepository,
    authRepository: AuthRepository,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : RouteViewModel(scope, route),
    GrazeEditorStateHolder by scope.actionSuspendingStateMutator(
        state = State(route).toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { state, actions ->
            actions
                .withInitialLoad(route)
                .launchMutationsIn(
                    productionScope = this,
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.Navigate -> action.flow.collect {
                            navActions(it.navigationMutation)
                        }
                        is Action.SearchProfiles -> action.flow.launchSearchMutations(
                            state = state,
                            searchRepository = searchRepository,
                        )
                        is Action.Update -> action.flow.launchUpdateMutations(
                            state = state,
                            recordRepository = recordRepository,
                            authRepository = authRepository,
                            navActions = navActions,
                        )
                        is Action.EditorNavigation -> action.flow.launchEditorNavigationMutations(
                            state = state,
                        )
                        is Action.EditFilter -> action.flow.launchEditFilterMutations(
                            state = state,
                        )
                        is Action.Metadata -> action.flow.launchUpdateMetadataMutations(
                            state = state,
                        )
                    }
                }
        },
    )

private fun Flow<Action>.withInitialLoad(
    route: Route,
) = onStart {
    route.initialLoad?.let { emit(it) }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SearchProfiles>.launchSearchMutations(
    state: State.SnapshotMutable,
    searchRepository: SearchRepository,
) = debounce(SEARCH_DEBOUNCE_MILLIS)
    .launchedCollectLatest { action ->
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
        ).collect { profiles ->
            state.suggestedProfiles = profiles.map(ProfileWithViewerState::profile)
        }
    }

context(productionScope: CoroutineScope)
private fun Flow<Action.Update>.launchUpdateMutations(
    state: State.SnapshotMutable,
    recordRepository: RecordRepository,
    authRepository: AuthRepository,
    navActions: (NavigationMutation) -> Unit,
) = launchedCollectLatest { action ->
    state.isLoading = true
    recordRepository.updateGrazeFeed(
        action.toGrazeFeedUpdate(),
    )
        .onSuccess { grazeFeed ->
            if (grazeFeed !is GrazeFeed.Editable) {
                navActions(
                    Action.Navigate.PopFeed(action.associatedRecordKey).navigationMutation,
                )
                return@onSuccess
            }

            state.grazeFeed = grazeFeed
            state.isLoading = false

            // Observe the feed
            authRepository.signedInUser
                .mapNotNull { it?.did }
                .distinctUntilChanged()
                .map {
                    recordUriOrNull(
                        profileId = it,
                        namespace = FeedGeneratorUri.NAMESPACE,
                        recordKey = grazeFeed.recordKey,
                    )
                }
                .filterIsInstance<FeedGeneratorUri>()
                .flatMapLatest(recordRepository::embeddableRecord)
                .distinctUntilChanged()
                .filterIsInstance<FeedGenerator>()
                .collect { state.feedGenerator = it }
        }
        .onFailure { throwable ->
            state.isLoading = false
            state.messages += action.toErrorMessage(throwable)
        }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.Metadata>.launchUpdateMetadataMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.grazeFeed = when (val grazeFeed = state.grazeFeed) {
        is GrazeFeed.Created -> grazeFeed.copy(
            displayName = action.displayName,
            description = action.description,
        )
        is GrazeFeed.Pending -> grazeFeed.copy(
            displayName = action.displayName,
            description = action.description,
        )
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.EditorNavigation>.launchEditorNavigationMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    when (action) {
        is Action.EditorNavigation.EnterFilter -> {
            state.suggestedProfiles = emptyList()
            state.currentPath = state.currentPath + action.index
        }
        Action.EditorNavigation.ExitFilter -> {
            state.suggestedProfiles = emptyList()
            if (state.currentPath.isNotEmpty()) {
                state.currentPath = state.currentPath.dropLast(1)
            }
        }
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.EditFilter>.launchEditFilterMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    val editedFilter = state.grazeFeed.filter.updateAt(action.path) { target ->
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
    state.suggestedProfiles = emptyList()
    state.grazeFeed = when (val currentFeed = state.grazeFeed) {
        is GrazeFeed.Created -> currentFeed.copy(filter = editedFilter)
        is GrazeFeed.Pending -> currentFeed.copy(filter = editedFilter)
    }
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

private val Action.Update.associatedRecordKey
    get() = when (this) {
        is Action.Update.Delete -> recordKey
        is Action.Update.InitialLoad -> recordKey
        is Action.Update.Save -> feed.recordKey
    }
private fun Action.Update.toGrazeFeedUpdate(): GrazeFeed.Update = when (this) {
    is Action.Update.InitialLoad -> Get(recordKey = recordKey)
    is Action.Update.Save -> when (val feed = feed) {
        is GrazeFeed.Created -> Edit(
            feed = feed,
        )
        is GrazeFeed.Pending -> Create(
            feed = feed.copy(
                displayName = feed.displayName.takeUnless(String?::isNullOrBlank)
                    ?: feed.recordKey.value,
            ),
        )
    }
    is Action.Update.Delete -> Delete(recordKey = recordKey)
}

private fun Action.Update.toErrorMessage(throwable: Throwable): Memo.Resource {
    val message = throwable.message ?: ""
    val stringResource = when (this) {
        is Action.Update.Delete -> Res.string.error_deleting_graze_feed
        is Action.Update.InitialLoad -> Res.string.error_fetching_graze_feed
        is Action.Update.Save -> Res.string.error_saving_graze_feed
    }
    return Memo.Resource(stringResource = stringResource, args = listOf(message))
}

private const val SEARCH_DEBOUNCE_MILLIS = 300L
const val MAX_SUGGESTED_PROFILES = 5
