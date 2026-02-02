/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tunjid.heron.ui.DestructiveDialogButton
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.SimpleDialog
import com.tunjid.heron.ui.SimpleDialogText
import com.tunjid.heron.ui.SimpleDialogTitle
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.cancel
import heron.feature.settings.generated.resources.sign_out
import heron.feature.settings.generated.resources.sign_out_confirmation
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignOutItem(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onSignOutClicked: () -> Unit,
) {
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }

    SettingsItemRow(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                showSignOutDialog = true
            },
        title = stringResource(Res.string.sign_out),
        titleColor = MaterialTheme.colorScheme.error,
        icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
        enabled = enabled,
    )

    SignOutDialog(
        showSignOutDialog = showSignOutDialog,
        onDismiss = { showSignOutDialog = false },
        onConfirmSignOut = {
            showSignOutDialog = false
            onSignOutClicked()
        },
    )
}

@Composable
fun SignOutDialog(
    showSignOutDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirmSignOut: () -> Unit,
) {
    if (!showSignOutDialog) return

    SimpleDialog(
        onDismissRequest = onDismiss,
        title = {
            SimpleDialogTitle(
                text = stringResource(Res.string.sign_out),
            )
        },
        text = {
            SimpleDialogText(
                text = stringResource(Res.string.sign_out_confirmation),
            )
        },
        confirmButton = {
            DestructiveDialogButton(
                text = stringResource(Res.string.sign_out),
                onClick = onConfirmSignOut,
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
