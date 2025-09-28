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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.cancel
import heron.feature.settings.generated.resources.sign_out
import heron.feature.settings.generated.resources.sign_out_confirmation
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignOutItem(
    onSignOutClicked: () -> Unit,
) {
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }

    Button(
        onClick = { showSignOutDialog = true },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = stringResource(Res.string.sign_out))
    }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.sign_out),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = stringResource(Res.string.sign_out_confirmation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmSignOut) {
                Text(
                    text = stringResource(Res.string.sign_out),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(Res.string.cancel),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = MaterialTheme.shapes.medium,
    )
}
