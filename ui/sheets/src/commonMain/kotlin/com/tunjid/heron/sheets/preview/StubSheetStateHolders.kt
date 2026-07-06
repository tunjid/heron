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

import com.tunjid.heron.sheets.embedrecordoptions.EmbeddableRecordOptionsAction
import com.tunjid.heron.sheets.embedrecordoptions.EmbeddableRecordOptionsState
import com.tunjid.heron.sheets.embedrecordoptions.EmbeddableRecordOptionsStateHolder
import com.tunjid.heron.sheets.inference.InferenceAction
import com.tunjid.heron.sheets.inference.InferenceState
import com.tunjid.heron.sheets.inference.InferenceStateHolder
import com.tunjid.heron.sheets.mutedwords.MutedWordsAction
import com.tunjid.heron.sheets.mutedwords.MutedWordsState
import com.tunjid.heron.sheets.mutedwords.MutedWordsStateHolder
import com.tunjid.heron.sheets.postinteractions.PostInteractionsAction
import com.tunjid.heron.sheets.postinteractions.PostInteractionsState
import com.tunjid.heron.sheets.postinteractions.PostInteractionsStateHolder
import com.tunjid.heron.sheets.postoptions.PostOptionsAction
import com.tunjid.heron.sheets.postoptions.PostOptionsState
import com.tunjid.heron.sheets.postoptions.PostOptionsStateHolder
import com.tunjid.heron.sheets.selectlist.SelectListAction
import com.tunjid.heron.sheets.selectlist.SelectListState
import com.tunjid.heron.sheets.selectlist.SelectListStateHolder
import com.tunjid.heron.sheets.threadgate.ThreadGateAction
import com.tunjid.heron.sheets.threadgate.ThreadGateState
import com.tunjid.heron.sheets.threadgate.ThreadGateStateHolder
import com.tunjid.heron.ui.stateproduction.SheetStateHolder
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.asNoOpActionSuspendingStateMutator
import kotlin.reflect.KClass

/**
 * Creates a no-op [SheetStateHolder] for the sheet state holder interface [type], backed by an
 * [asNoOpActionSuspendingStateMutator] so its state is immutable and its actions are ignored. This
 * lets previews render any sheet without repositories, the write queue, navigation, the dependency
 * graph or an AndroidX `ViewModel`, mirroring how previews stub the identity, navigation and
 * notification state holders.
 */
fun stubSheetStateHolder(
    type: KClass<*>,
): SheetStateHolder = when (type) {
    ThreadGateStateHolder::class ->
        object :
            ThreadGateStateHolder,
            ActionSuspendingStateMutator<ThreadGateAction, ThreadGateState>
            by ThreadGateState.Immutable().asNoOpActionSuspendingStateMutator() {}

    SelectListStateHolder::class ->
        object :
            SelectListStateHolder,
            ActionSuspendingStateMutator<SelectListAction, SelectListState>
            by SelectListState.Immutable().asNoOpActionSuspendingStateMutator() {}

    PostOptionsStateHolder::class ->
        object :
            PostOptionsStateHolder,
            ActionSuspendingStateMutator<PostOptionsAction, PostOptionsState>
            by PostOptionsState.Immutable().asNoOpActionSuspendingStateMutator() {}

    MutedWordsStateHolder::class ->
        object :
            MutedWordsStateHolder,
            ActionSuspendingStateMutator<MutedWordsAction, MutedWordsState>
            by MutedWordsState.Immutable().asNoOpActionSuspendingStateMutator() {}

    PostInteractionsStateHolder::class ->
        object :
            PostInteractionsStateHolder,
            ActionSuspendingStateMutator<PostInteractionsAction, PostInteractionsState>
            by PostInteractionsState.Immutable().asNoOpActionSuspendingStateMutator() {}

    EmbeddableRecordOptionsStateHolder::class ->
        object :
            EmbeddableRecordOptionsStateHolder,
            ActionSuspendingStateMutator<EmbeddableRecordOptionsAction, EmbeddableRecordOptionsState>
            by EmbeddableRecordOptionsState.Immutable().asNoOpActionSuspendingStateMutator() {}

    InferenceStateHolder::class ->
        object :
            InferenceStateHolder,
            ActionSuspendingStateMutator<InferenceAction, InferenceState>
            by InferenceState.Immutable().asNoOpActionSuspendingStateMutator() {}

    else -> error("No stub SheetStateHolder registered for $type")
}
