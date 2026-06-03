package com.tunjid.heron.scaffold.scaffold

import androidx.compose.runtime.Composable
import com.tunjid.heron.timeline.ui.sheets.mutedwords.MutedWordsSheetState

@Composable
fun PaneScaffoldState.rememberMutedWordsSheetState(): MutedWordsSheetState =
    MutedWordsSheetState.rememberUpdatedMutedWordsSheetState(
        initializer = appState.sheetsViewModelInitializers.mutedWordsViewModelInitializer,
    )
