package com.tunjid.heron.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tunjid.heron.profile.Action
import com.tunjid.heron.ui.DestructiveDialogButton
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.SimpleDialog
import com.tunjid.heron.ui.SimpleDialogText
import com.tunjid.heron.ui.SimpleDialogTitle
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.Res
import heron.ui.core.generated.resources.block_account_dialog_description
import heron.ui.core.generated.resources.block_account_dialog_title
import heron.ui.core.generated.resources.cancel
import heron.ui.core.generated.resources.mute_account_dialog_description
import heron.ui.core.generated.resources.mute_account_dialog_title
import heron.ui.core.generated.resources.unblock_account_dialog_description
import heron.ui.core.generated.resources.unblock_account_dialog_title
import heron.ui.core.generated.resources.unmute_account_dialog_description
import heron.ui.core.generated.resources.unmute_account_dialog_title
import heron.ui.core.generated.resources.viewer_state_block_account
import heron.ui.core.generated.resources.viewer_state_mute_account
import heron.ui.core.generated.resources.viewer_state_unblock_account
import heron.ui.core.generated.resources.viewer_state_unmute_account
import org.jetbrains.compose.resources.stringResource

@Stable
class ProfileRestrictionsDialogState internal constructor() {

    var moderation by mutableStateOf<Action.Moderation?>(null)
        private set

    fun show(moderation: Action.Moderation) {
        this.moderation = moderation
    }

    fun hide() {
        moderation = null
    }

    companion object {
        @Composable
        fun rememberProfileRestrictionsDialogState(
            onApproved: (Action.Moderation) -> Unit
        ): ProfileRestrictionsDialogState {
            val state = remember { ProfileRestrictionsDialogState() }

            state.moderation?.let { moderation ->
                ProfileRestrictionsDialog(
                    moderation = moderation,
                    onDismiss = state::hide,
                    onApproved = {
                        onApproved(moderation)
                        state.hide()
                    },
                )
            }

            return state
        }
    }
}

@Composable
private fun ProfileRestrictionsDialog(
    moderation: Action.Moderation,
    onDismiss: () -> Unit,
    onApproved: () -> Unit,
) {
    val (title, description, confirmText) =
        when (moderation) {
            is Action.Block.Add ->
                Triple(
                    stringResource(CommonStrings.block_account_dialog_title),
                    stringResource(CommonStrings.block_account_dialog_description),
                    stringResource(CommonStrings.viewer_state_block_account),
                )

            is Action.Block.Remove ->
                Triple(
                    stringResource(CommonStrings.unblock_account_dialog_title),
                    stringResource(CommonStrings.unblock_account_dialog_description),
                    stringResource(CommonStrings.viewer_state_unblock_account),
                )

            is Action.Mute.Add ->
                Triple(
                    stringResource(CommonStrings.mute_account_dialog_title),
                    stringResource(CommonStrings.mute_account_dialog_description),
                    stringResource(CommonStrings.viewer_state_mute_account),
                )

            is Action.Mute.Remove ->
                Triple(
                    stringResource(CommonStrings.unmute_account_dialog_title),
                    stringResource(CommonStrings.unmute_account_dialog_description),
                    stringResource(CommonStrings.viewer_state_unmute_account),
                )
        }

    SimpleDialog(
        onDismissRequest = onDismiss,
        title = { SimpleDialogTitle(text = title) },
        text = { SimpleDialogText(text = description) },
        confirmButton = { DestructiveDialogButton(text = confirmText, onClick = onApproved) },
        dismissButton = {
            NeutralDialogButton(text = stringResource(Res.string.cancel), onClick = onDismiss)
        },
    )
}
