package com.tunjid.heron.timeline.utilities

import androidx.compose.runtime.Composable
import com.tunjid.heron.ui.DestructiveDialogButton
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.SimpleDialog
import com.tunjid.heron.ui.SimpleDialogText
import com.tunjid.heron.ui.SimpleDialogTitle
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.viewer_state_block
import heron.ui.core.generated.resources.viewer_state_unblock
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.block_account_dialog_description
import heron.ui.timeline.generated.resources.block_account_dialog_title
import heron.ui.timeline.generated.resources.cancel
import heron.ui.timeline.generated.resources.unblock_account_dialog_description
import heron.ui.timeline.generated.resources.unblock_account_dialog_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun BlockAccountDialog(
    showBlockAccountDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!showBlockAccountDialog) return

    SimpleDialog(
        onDismissRequest = onDismiss,
        title = {
            SimpleDialogTitle(
                text = stringResource(Res.string.block_account_dialog_title),
            )
        },
        text = {
            SimpleDialogText(
                text = stringResource(Res.string.block_account_dialog_description),
            )
        },
        confirmButton = {
            DestructiveDialogButton(
                text = stringResource(CommonStrings.viewer_state_block),
                onClick = onConfirm,
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

@Composable
fun UnblockAccountDialog(
    showUnblockAccountDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!showUnblockAccountDialog) return

    SimpleDialog(
        onDismissRequest = onDismiss,
        title = {
            SimpleDialogTitle(
                text = stringResource(Res.string.unblock_account_dialog_title),
            )
        },
        text = {
            SimpleDialogText(
                text = stringResource(Res.string.unblock_account_dialog_description),
            )
        },
        confirmButton = {
            DestructiveDialogButton(
                text = stringResource(CommonStrings.viewer_state_unblock),
                onClick = onConfirm,
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
