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
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.sheets.embedrecordoptions.EmbeddableRecordOptionsSheetState
import com.tunjid.heron.sheets.embedrecordoptions.EmbeddableRecordOptionsStateHolder
import com.tunjid.heron.sheets.inference.InferenceSheetState
import com.tunjid.heron.sheets.inference.InferenceStateHolder
import com.tunjid.heron.sheets.mutedwords.MutedWordsSheetState
import com.tunjid.heron.sheets.mutedwords.MutedWordsStateHolder
import com.tunjid.heron.sheets.postinteractions.PostInteractionsSheetState
import com.tunjid.heron.sheets.postinteractions.PostInteractionsStateHolder
import com.tunjid.heron.sheets.postoptions.PostOption
import com.tunjid.heron.sheets.postoptions.PostOptionsSheetState
import com.tunjid.heron.sheets.postoptions.PostOptionsStateHolder
import com.tunjid.heron.sheets.profile.ProfileSearchSheetState
import com.tunjid.heron.sheets.profile.ProfileSearchStateHolder
import com.tunjid.heron.sheets.selectlist.SelectListSheetState
import com.tunjid.heron.sheets.selectlist.SelectListStateHolder
import com.tunjid.heron.sheets.threadgate.ThreadGateSheetState
import com.tunjid.heron.sheets.threadgate.ThreadGateStateHolder
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.SnackbarDisplayEffect
import com.tunjid.heron.ui.scaffold.scaffold.retainSheetStateHolder

@Composable
fun PaneScaffoldState.rememberMutedWordsSheetState(): MutedWordsSheetState {
    val sheetState = MutedWordsSheetState.rememberUpdatedMutedWordsSheetState(
        stateHolder = retainSheetStateHolder<MutedWordsStateHolder>(),
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
): PostOptionsSheetState {
    val sheetState = PostOptionsSheetState.rememberUpdatedPostOptionsSheetState(
        stateHolder = retainSheetStateHolder<PostOptionsStateHolder>(),
        onOptionClicked = onOptionClicked,
    )
    SnackbarDisplayEffect(
        messages = sheetState.messages,
        onMessageConsumed = sheetState::onSnackbarMessageConsumed,
    )
    return sheetState
}

@Composable
fun PaneScaffoldState.rememberTimelineThreadGateSheetState(): ThreadGateSheetState.OfTimeline {
    val sheetState = ThreadGateSheetState.rememberUpdatedThreadGateSheetState(
        stateHolder = retainSheetStateHolder<ThreadGateStateHolder>(),
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
        stateHolder = retainSheetStateHolder<ThreadGateStateHolder>(),
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
        stateHolder = retainSheetStateHolder<EmbeddableRecordOptionsStateHolder>(),
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
        stateHolder = retainSheetStateHolder<SelectListStateHolder>(),
        onListSelected = onListSelected,
    )

@Composable
fun PaneScaffoldState.rememberPostInteractionsSheetState(
    sharedElementPrefix: String?,
): PostInteractionsSheetState {
    val sheetState = PostInteractionsSheetState.rememberUpdatedPostInteractionsSheetState(
        stateHolder = retainSheetStateHolder<PostInteractionsStateHolder>(),
        sharedElementPrefix = sharedElementPrefix,
    )
    SnackbarDisplayEffect(
        messages = sheetState.messages,
        onMessageConsumed = sheetState::onSnackbarMessageConsumed,
    )
    return sheetState
}

@Composable
fun PaneScaffoldState.rememberInferenceSheetState(): InferenceSheetState =
    InferenceSheetState.rememberUpdatedInferenceSheetState(
        stateHolder = retainSheetStateHolder<InferenceStateHolder>(),
    )

@Composable
fun PaneScaffoldState.rememberProfileSearchSheetState(
    onProfileClicked: (Profile) -> Unit,
): ProfileSearchSheetState =
    ProfileSearchSheetState.rememberUpdatedProfileSearchSheetState(
        stateHolder = retainSheetStateHolder<ProfileSearchStateHolder>(),
        onProfileClicked = onProfileClicked,
    )
