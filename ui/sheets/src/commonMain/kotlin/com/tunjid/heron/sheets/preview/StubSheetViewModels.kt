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

package com.tunjid.heron.sheets.preview

import com.tunjid.heron.sheets.embedrecordoptions.EmbeddableRecordOptionsState
import com.tunjid.heron.sheets.embedrecordoptions.EmbeddableRecordOptionsViewModel
import com.tunjid.heron.sheets.mutedwords.MutedWordsState
import com.tunjid.heron.sheets.mutedwords.MutedWordsViewModel
import com.tunjid.heron.sheets.postinteractions.PostInteractionsState
import com.tunjid.heron.sheets.postinteractions.PostInteractionsViewModel
import com.tunjid.heron.sheets.postoptions.PostOptionsState
import com.tunjid.heron.sheets.postoptions.PostOptionsViewModel
import com.tunjid.heron.sheets.selectlist.SelectListState
import com.tunjid.heron.sheets.selectlist.SelectListViewModel
import com.tunjid.heron.sheets.threadgate.ThreadGateState
import com.tunjid.heron.sheets.threadgate.ThreadGateViewModel
import com.tunjid.heron.ui.stateproduction.SheetViewModel
import com.tunjid.heron.ui.stateproduction.SheetViewModelInitializer
import com.tunjid.mutator.coroutines.asNoOpActionSuspendingStateMutator
import kotlin.reflect.KClass

/**
 * Creates a [SheetViewModelInitializer] for [modelClass] backed by an
 * [asNoOpActionSuspendingStateMutator], producing a sheet [SheetViewModel] whose state is immutable
 * and whose actions are ignored. This lets previews render any sheet without repositories, the write
 * queue, navigation, or the dependency graph, mirroring how `PreviewPaneScaffoldState` stubs the
 * identity, navigation and notification state holders.
 */
fun stubSheetViewModelInitializer(
    modelClass: KClass<out SheetViewModel>,
): SheetViewModelInitializer = SheetViewModelInitializer { scope ->
    when (modelClass) {
        ThreadGateViewModel::class -> ThreadGateViewModel(
            mutator = ThreadGateState.Immutable().asNoOpActionSuspendingStateMutator(),
            scope = scope,
        )
        SelectListViewModel::class -> SelectListViewModel(
            mutator = SelectListState.Immutable().asNoOpActionSuspendingStateMutator(),
            scope = scope,
        )
        PostOptionsViewModel::class -> PostOptionsViewModel(
            mutator = PostOptionsState.Immutable().asNoOpActionSuspendingStateMutator(),
            scope = scope,
        )
        MutedWordsViewModel::class -> MutedWordsViewModel(
            mutator = MutedWordsState.Immutable().asNoOpActionSuspendingStateMutator(),
            scope = scope,
        )
        PostInteractionsViewModel::class -> PostInteractionsViewModel(
            mutator = PostInteractionsState.Immutable().asNoOpActionSuspendingStateMutator(),
            scope = scope,
        )
        EmbeddableRecordOptionsViewModel::class -> EmbeddableRecordOptionsViewModel(
            mutator = EmbeddableRecordOptionsState.Immutable().asNoOpActionSuspendingStateMutator(),
            scope = scope,
        )
        else -> error("No stub SheetViewModel registered for $modelClass")
    }
}
