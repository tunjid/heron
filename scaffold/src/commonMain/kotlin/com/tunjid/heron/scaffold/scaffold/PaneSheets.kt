package com.tunjid.heron.scaffold.scaffold

import androidx.compose.runtime.Composable
import com.tunjid.heron.timeline.ui.sheets.mutedwords.MutedWordsSheetState
import com.tunjid.heron.timeline.ui.sheets.postoptions.PostOption
import com.tunjid.heron.timeline.ui.sheets.postoptions.PostOptionsSheetState

@Composable
fun PaneScaffoldState.rememberMutedWordsSheetState(): MutedWordsSheetState =
    MutedWordsSheetState.rememberUpdatedMutedWordsSheetState(
        initializer = appState.sheetsViewModelInitializers.mutedWordsViewModelInitializer,
    )

@Composable
fun PaneScaffoldState.rememberPostOptionsSheetState(
    onOptionClicked: (PostOption) -> Unit,
): PostOptionsSheetState =
    PostOptionsSheetState.rememberUpdatedPostOptionsSheetState(
        initializer = appState.sheetsViewModelInitializers.postOptionsViewModelInitializer,
        onOptionClicked = onOptionClicked,
    )
