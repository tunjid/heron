package com.tunjid.heron.timeline.ui.sheets.selectlist

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.timeline.utilities.SheetWhileSubscribed
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.serialization.Serializable

typealias SelectListStateHolder = ActionSuspendingStateMutator<SelectListAction, SelectListState>

@AssistedFactory
fun interface SelectListViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
    ): SelectListViewModel
}

@AssistedInject
class SelectListViewModel(
    recordRepository: RecordRepository,
    @Assisted scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    SelectListStateHolder by scope.actionSuspendingStateMutator(
        state = SelectListState.Immutable().toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(SheetWhileSubscribed),
        producer = { state, _ ->
            launchLoadListsMutations(
                state = state,
                recordRepository = recordRepository,
            )
        },
    )

context(productionScope: CoroutineScope)
private fun launchLoadListsMutations(
    state: SelectListState.SnapshotMutable,
    recordRepository: RecordRepository,
) = recordRepository.recentLists
    .launchedCollect { state.lists = it }

@Stable
@Snapshottable
interface SelectListState {
    @SnapshotSpec
    @Serializable
    data class Immutable(
        val lists: List<FeedList> = emptyList(),
    ) : SelectListState
}

sealed class SelectListAction(val key: String)
