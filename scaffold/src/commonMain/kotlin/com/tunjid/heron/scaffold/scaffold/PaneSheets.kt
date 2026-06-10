package com.tunjid.heron.scaffold.scaffold

import androidx.compose.runtime.Composable
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.PostInteractionSettingsPreference
import com.tunjid.heron.data.core.models.ThreadGate
import com.tunjid.heron.timeline.ui.sheets.mutedwords.MutedWordsSheetState
import com.tunjid.heron.timeline.ui.sheets.postoptions.PostOption
import com.tunjid.heron.timeline.ui.sheets.postoptions.PostOptionsSheetState
import com.tunjid.heron.timeline.ui.sheets.threadgate.Mode
import com.tunjid.heron.timeline.ui.sheets.threadgate.ThreadGateSheetState

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

@Composable
fun PaneScaffoldState.rememberTimelineThreadGateSheetState(
    onThreadGateUpdated: (Post.Interaction.Upsert.Gate) -> Unit,
): ThreadGateSheetState.OfTimeline =
    ThreadGateSheetState.rememberUpdatedThreadGateSheetState(
        initializer = appState.sheetsViewModelInitializers.threadGateViewModelInitializer,
        onThreadGateUpdated = onThreadGateUpdated,
    )

@Composable
fun PaneScaffoldState.rememberPreferenceThreadGateSheetState(
    onDefaultThreadGateUpdated: (PostInteractionSettingsPreference) -> Unit,
): ThreadGateSheetState.OfPreference =
    ThreadGateSheetState.rememberUpdatedThreadGateSheetState(
        initializer = appState.sheetsViewModelInitializers.threadGateViewModelInitializer,
        onDefaultThreadGateUpdated = onDefaultThreadGateUpdated,
    )
