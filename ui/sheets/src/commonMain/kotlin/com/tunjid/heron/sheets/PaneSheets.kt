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

package com.tunjid.heron.sheets

import androidx.compose.runtime.Composable
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.PostInteractionSettingsPreference
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.sheets.embedrecordoptions.EmbeddableRecordOptionsSheetState
import com.tunjid.heron.sheets.embedrecordoptions.EmbeddableRecordOptionsViewModel
import com.tunjid.heron.sheets.mutedwords.MutedWordsSheetState
import com.tunjid.heron.sheets.mutedwords.MutedWordsViewModel
import com.tunjid.heron.sheets.postinteractions.PostInteractionsSheetState
import com.tunjid.heron.sheets.postinteractions.PostInteractionsViewModel
import com.tunjid.heron.sheets.postoptions.PostOption
import com.tunjid.heron.sheets.postoptions.PostOptionsSheetState
import com.tunjid.heron.sheets.postoptions.PostOptionsViewModel
import com.tunjid.heron.sheets.selectlist.SelectListSheetState
import com.tunjid.heron.sheets.selectlist.SelectListViewModel
import com.tunjid.heron.sheets.threadgate.ThreadGateSheetState
import com.tunjid.heron.sheets.threadgate.ThreadGateViewModel
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.SnackbarDisplayEffect
import com.tunjid.heron.ui.stateproduction.SheetViewModel
import kotlinx.coroutines.CoroutineScope

/**
 * Builds the `(CoroutineScope) -> VM` initializer for a sheet [SheetViewModel] by resolving it from
 * the app graph through [PaneScaffoldState]. This is how the sheets reach their DI-provided
 * ViewModels without the scaffold layer depending on this module.
 */
private inline fun <reified VM : SheetViewModel> PaneScaffoldState.sheetInitializer(): (CoroutineScope) -> VM =
    { scope -> sheetViewModelInitializer(VM::class).invoke(scope) as VM }

@Composable
fun PaneScaffoldState.rememberMutedWordsSheetState(): MutedWordsSheetState {
    val sheetState = MutedWordsSheetState.rememberUpdatedMutedWordsSheetState(
        initializer = sheetInitializer<MutedWordsViewModel>(),
    )
    SnackbarDisplayEffect(
        messages = sheetState.messages,
        onMessageConsumed = sheetState::onSnackbarMessageConsumed,
    )
    return sheetState
}

@Composable
fun PaneScaffoldState.rememberPostOptionsSheetState(
    onOptionClicked: (PostOption) -> Unit,
): PostOptionsSheetState =
    PostOptionsSheetState.rememberUpdatedPostOptionsSheetState(
        initializer = sheetInitializer<PostOptionsViewModel>(),
        onOptionClicked = onOptionClicked,
    )

@Composable
fun PaneScaffoldState.rememberTimelineThreadGateSheetState(): ThreadGateSheetState.OfTimeline {
    val sheetState = ThreadGateSheetState.rememberUpdatedThreadGateSheetState(
        initializer = sheetInitializer<ThreadGateViewModel>(),
    )
    SnackbarDisplayEffect(
        messages = sheetState.messages,
        onMessageConsumed = sheetState::onSnackbarMessageConsumed,
    )
    return sheetState
}

@Composable
fun PaneScaffoldState.rememberPreferenceThreadGateSheetState(
    onDefaultThreadGateUpdated: (PostInteractionSettingsPreference) -> Unit,
): ThreadGateSheetState.OfPreference =
    ThreadGateSheetState.rememberUpdatedThreadGateSheetState(
        initializer = sheetInitializer<ThreadGateViewModel>(),
        onDefaultThreadGateUpdated = onDefaultThreadGateUpdated,
    )

@Composable
fun PaneScaffoldState.rememberEmbeddableRecordOptionsSheetState(
    editTitle: String?,
    onEditClicked: (EmbeddableRecordUri) -> Unit,
    onShareInConversationClicked: (EmbeddableRecordUri, Conversation) -> Unit,
    onShareInPostClicked: (EmbeddableRecordUri) -> Unit,
): EmbeddableRecordOptionsSheetState =
    EmbeddableRecordOptionsSheetState.rememberUpdatedEmbeddableRecordOptionsState(
        initializer = sheetInitializer<EmbeddableRecordOptionsViewModel>(),
        editTitle = editTitle,
        onEditClicked = onEditClicked,
        onShareInConversationClicked = onShareInConversationClicked,
        onShareInPostClicked = onShareInPostClicked,
    )

@Composable
fun PaneScaffoldState.rememberSelectListSheetState(
    onListSelected: (FeedList) -> Unit,
): SelectListSheetState =
    SelectListSheetState.rememberUpdatedSelectListSheetState(
        initializer = sheetInitializer<SelectListViewModel>(),
        onListSelected = onListSelected,
    )

@Composable
fun PaneScaffoldState.rememberPostInteractionsSheetState(
    sharedElementPrefix: String?,
): PostInteractionsSheetState {
    val sheetState = PostInteractionsSheetState.rememberUpdatedPostInteractionsSheetState(
        initializer = sheetInitializer<PostInteractionsViewModel>(),
        sharedElementPrefix = sharedElementPrefix,
    )
    SnackbarDisplayEffect(
        messages = sheetState.messages,
        onMessageConsumed = sheetState::onSnackbarMessageConsumed,
    )
    return sheetState
}
