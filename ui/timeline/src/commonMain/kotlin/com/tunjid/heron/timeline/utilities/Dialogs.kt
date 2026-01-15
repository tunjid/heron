package com.tunjid.heron.timeline.utilities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.ui.DestructiveDialogButton
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.SimpleDialog
import com.tunjid.heron.ui.SimpleDialogText
import com.tunjid.heron.ui.SimpleDialogTitle
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.block_account_dialog_description
import heron.ui.core.generated.resources.block_account_dialog_title
import heron.ui.core.generated.resources.mute_account_dialog_description
import heron.ui.core.generated.resources.mute_account_dialog_title
import heron.ui.core.generated.resources.viewer_state_block_account
import heron.ui.core.generated.resources.viewer_state_mute_account
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.cancel
import org.jetbrains.compose.resources.stringResource

@Stable
class ModerationDialogState internal constructor() {

    var moderation by mutableStateOf<PostAction.Moderation?>(null)
        private set

    fun show(moderation: PostAction.Moderation) {
        this.moderation = moderation
    }

    fun hide() {
        moderation = null
    }

    companion object {
        @Composable
        fun rememberModerationDialogState(
            onApproved: (PostAction.Moderation) -> Unit,
        ): ModerationDialogState {
            val state = remember { ModerationDialogState() }

            state.moderation?.let { moderation ->
                ModerationDialog(
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
private fun ModerationDialog(
    moderation: PostAction.Moderation,
    onDismiss: () -> Unit,
    onApproved: () -> Unit,
) {
    val (title, description, confirmText) = when (moderation) {
        is PostAction.Moderation.OfBlockAccount -> Triple(
            stringResource(CommonStrings.block_account_dialog_title),
            stringResource(CommonStrings.block_account_dialog_description),
            stringResource(CommonStrings.viewer_state_block_account),
        )
        is PostAction.Moderation.OfMuteAccount -> Triple(
            stringResource(CommonStrings.mute_account_dialog_title),
            stringResource(CommonStrings.mute_account_dialog_description),
            stringResource(CommonStrings.viewer_state_mute_account),
        )
    }

    SimpleDialog(
        onDismissRequest = onDismiss,
        title = { SimpleDialogTitle(text = title) },
        text = { SimpleDialogText(text = description) },
        confirmButton = {
            DestructiveDialogButton(
                text = confirmText,
                onClick = onApproved,
            )
        },
        dismissButton = {
            NeutralDialogButton(
                text = stringResource(Res.string.cancel),
                onClick = onDismiss,
            )
        },
    )
}
