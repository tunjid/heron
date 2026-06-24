package com.tunjid.heron.scaffold.scaffold

import androidx.compose.runtime.Composable
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.PostInteractionSettingsPreference
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.timeline.ui.sheets.embedrecordoptions.EmbeddableRecordOptionsSheetState
import com.tunjid.heron.timeline.ui.sheets.mutedwords.MutedWordsSheetState
import com.tunjid.heron.timeline.ui.sheets.postinteractions.PostInteractionsSheetState
import com.tunjid.heron.timeline.ui.sheets.postoptions.PostOption
import com.tunjid.heron.timeline.ui.sheets.postoptions.PostOptionsSheetState
import com.tunjid.heron.timeline.ui.sheets.selectlist.SelectListSheetState
import com.tunjid.heron.timeline.ui.sheets.threadgate.ThreadGateSheetState

@Composable
fun PaneScaffoldState.rememberMutedWordsSheetState(): MutedWordsSheetState {
    val sheetState = MutedWordsSheetState.rememberUpdatedMutedWordsSheetState(
        initializer = appState.sheetsViewModelInitializers.mutedWordsViewModelInitializer,
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
        initializer = appState.sheetsViewModelInitializers.postOptionsViewModelInitializer,
        onOptionClicked = onOptionClicked,
    )

@Composable
fun PaneScaffoldState.rememberTimelineThreadGateSheetState(): ThreadGateSheetState.OfTimeline {
    val sheetState = ThreadGateSheetState.rememberUpdatedThreadGateSheetState(
        initializer = appState.sheetsViewModelInitializers.threadGateViewModelInitializer,
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
        initializer = appState.sheetsViewModelInitializers.threadGateViewModelInitializer,
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
        initializer = appState.sheetsViewModelInitializers.embeddableRecordOptionsViewModelInitializer,
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
        initializer = appState.sheetsViewModelInitializers.selectListViewModelInitializer,
        onListSelected = onListSelected,
    )

@Composable
fun PaneScaffoldState.rememberPostInteractionsSheetState(
    onSignInClicked: () -> Unit,
    onQuotePostClicked: (Post.Interaction.Create.Repost) -> Unit,
): PostInteractionsSheetState {
    val sheetState = PostInteractionsSheetState.rememberUpdatedPostInteractionsSheetState(
        initializer = appState.sheetsViewModelInitializers.postInteractionsViewModelInitializer,
        onSignInClicked = onSignInClicked,
        onQuotePostClicked = onQuotePostClicked,
    )
    SnackbarDisplayEffect(
        messages = sheetState.messages,
        onMessageConsumed = sheetState::onSnackbarMessageConsumed,
    )
    return sheetState
}
