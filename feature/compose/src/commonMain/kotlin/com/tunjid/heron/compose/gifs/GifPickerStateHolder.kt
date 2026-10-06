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

package com.tunjid.heron.compose.gifs

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Gif
import com.tunjid.heron.data.repository.GifQuery
import com.tunjid.heron.data.repository.GifRepository
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.launchTilingMutations
import com.tunjid.heron.ui.stateproduction.SheetStateHolder
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
import com.tunjid.tiler.distinctBy
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn

@Stable
interface GifPickerStateHolder :
    SheetStateHolder,
    ActionSuspendingStateMutator<GifPickerAction, GifPickerState>

@AssistedFactory
fun interface GifPickerViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
    ): GifPickerViewModel
}

class GifPickerViewModel(
    mutator: ActionSuspendingStateMutator<GifPickerAction, GifPickerState>,
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    GifPickerStateHolder,
    ActionSuspendingStateMutator<GifPickerAction, GifPickerState> by mutator {

    @AssistedInject
    constructor(
        gifRepository: GifRepository,
        @Assisted scope: CoroutineScope,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = GifPickerState.Immutable().toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            producer = { state, actions ->
                // Searches and category changes swap the query being tiled, so they feed the
                // tiling pipeline alongside the scroll driven tiling actions.
                val queryChanges = MutableSharedFlow<TilingState.Action>(
                    replay = 1,
                )
                actions.launchMutationsIn(
                    productionScope = this,
                    keySelector = GifPickerAction::key,
                ) {
                    when (val action = type()) {
                        is GifPickerAction.Tile -> merge(
                            action.flow.map { it.tilingAction },
                            queryChanges,
                        ).launchGifTilingMutations(
                            state = state,
                            gifRepository = gifRepository,
                        )
                        is GifPickerAction.Search -> action.flow.launchSearchMutations(
                            state = state,
                            onQueryChanged = queryChanges::emit,
                        )
                        is GifPickerAction.SelectCategory -> action.flow.launchSelectCategoryMutations(
                            state = state,
                            onQueryChanged = queryChanges::emit,
                        )
                    }
                }
            },
        ),
        scope = scope,
    )
}

context(productionScope: CoroutineScope)
private fun Flow<TilingState.Action>.launchGifTilingMutations(
    state: GifPickerState.Mutable,
    gifRepository: GifRepository,
) = launchTilingMutations(
    state = state,
    updateQueryData = GifQuery::updateData,
    refreshQuery = GifQuery::refresh,
    cursorListLoader = gifRepository::gifs,
    onNewItems = { items ->
        items.distinctBy(Gif::id)
    },
)

context(productionScope: CoroutineScope)
private fun Flow<GifPickerAction.Search>.launchSearchMutations(
    state: GifPickerState.Mutable,
    onQueryChanged: suspend (TilingState.Action) -> Unit,
) {
    val shared = shareIn(
        scope = productionScope,
        started = SharingStarted.WhileSubscribed(),
        replay = 1,
    )
    shared.launchedCollect { action ->
        state.searchText = action.text
        state.selectedCategory = when {
            action.text.isBlank() -> state.selectedCategory ?: GifCategory.Trending
            else -> null
        }
    }
    shared
        .debounce(SearchDebounce)
        .launchedCollectLatest { action ->
            // A category was picked while this search was debouncing.
            if (action.text != state.searchText) return@launchedCollectLatest
            onQueryChanged(
                TilingState.Action.LoadAround(
                    query = gifQuery(
                        searchText = action.text,
                        category = state.selectedCategory,
                    ),
                ),
            )
        }
}

context(productionScope: CoroutineScope)
private fun Flow<GifPickerAction.SelectCategory>.launchSelectCategoryMutations(
    state: GifPickerState.Mutable,
    onQueryChanged: suspend (TilingState.Action) -> Unit,
) = launchedCollect { action ->
    state.searchText = ""
    state.selectedCategory = action.category
    onQueryChanged(
        TilingState.Action.LoadAround(
            query = gifQuery(
                searchText = "",
                category = action.category,
            ),
        ),
    )
}

private val SearchDebounce = 300.milliseconds
