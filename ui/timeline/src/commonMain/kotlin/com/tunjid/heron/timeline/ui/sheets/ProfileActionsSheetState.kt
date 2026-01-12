package com.tunjid.heron.timeline.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.timeline.utilities.BlockAccount
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState

@Stable
class ProfileActionsSheetState(
    scope: BottomSheetScope,
) : BottomSheetState(scope) {

    override fun onHidden() {
    }

    companion object {
        @Composable
        fun rememberUpdatedProfileActionsSheetState(
            onBlockAccountClicked: () -> Unit,
            onShown: () -> Unit,
        ): ProfileActionsSheetState {
            val state = rememberBottomSheetState(
                skipPartiallyExpanded = false,
            ) { scope ->
                ProfileActionsSheetState(
                    scope = scope,
                )
            }
            ProfileActionsBottomSheet(
                state = state,
                onShown = onShown,
                onBlockAccountClicked = onBlockAccountClicked,
            )
            return state
        }
    }
}

@Composable
private fun ProfileActionsBottomSheet(
    state: ProfileActionsSheetState,
    onBlockAccountClicked: () -> Unit,
    onShown: () -> Unit,
) {
    state.ModalBottomSheet {
        Column(
            modifier = Modifier
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BlockAccount(
                onBlockAccountClicked = {
                    state.hide()
                    onBlockAccountClicked()
                },
            )
        }
    }
}
