package com.tunjid.heron.sheets.inference

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import com.tunjid.heron.ui.scaffold.ui.theme.AppTheme
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.mutator.coroutines.asNoOpActionSuspendingStateMutator

@Preview
@Composable
internal fun InferenceSheetPreview() {
    AppTheme {
        Surface {
            val scope = rememberCoroutineScope()
            InferenceBottomSheet(
                BottomSheetScope.rememberBottomSheetState {
                    InferenceSheetState(
                        scope = it,
                        stateHolder = InferenceViewModel(
                            InferenceState.Immutable(
                                kind = InferenceKind.Translation,
                                translationOutcome = InferenceOutcome.NoModel,
                            ).asNoOpActionSuspendingStateMutator(),
                            scope = scope,
                        ),
                    )
                },
            )
        }
    }
}
