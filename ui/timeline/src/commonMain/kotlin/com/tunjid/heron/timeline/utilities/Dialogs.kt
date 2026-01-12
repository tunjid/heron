package com.tunjid.heron.timeline.utilities

import androidx.compose.runtime.Composable
import com.tunjid.heron.ui.DestructiveDialogButton
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.SimpleDialog
import com.tunjid.heron.ui.SimpleDialogText
import com.tunjid.heron.ui.SimpleDialogTitle
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.block_account_dialog_confirm
import heron.ui.timeline.generated.resources.block_account_dialog_description
import heron.ui.timeline.generated.resources.block_account_dialog_title
import heron.ui.timeline.generated.resources.cancel
import heron.ui.timeline.generated.resources.unblock_account_dialog_confirm
import heron.ui.timeline.generated.resources.unblock_account_dialog_description
import heron.ui.timeline.generated.resources.unblock_account_dialog_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BlockAccountDialog(
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
                text = stringResource(Res.string.block_account_dialog_confirm),
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
                text = stringResource(Res.string.unblock_account_dialog_confirm),
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
