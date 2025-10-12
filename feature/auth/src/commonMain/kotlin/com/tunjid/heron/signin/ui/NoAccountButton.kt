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

package com.tunjid.heron.signin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import heron.feature.auth.generated.resources.Res
import heron.feature.auth.generated.resources.learn_more
import heron.feature.auth.generated.resources.no_account_dialog_details
import heron.feature.auth.generated.resources.no_account_dialog_title
import heron.feature.auth.generated.resources.no_account_help_button
import heron.feature.auth.generated.resources.no_account_help_content_description
import heron.feature.auth.generated.resources.okay
import org.jetbrains.compose.resources.stringResource

@Composable
fun NoAccountButton(
    modifier: Modifier = Modifier,
) {
    var showNoAccountDialog by remember { mutableStateOf(false) }

    TextButton(
        modifier = modifier,
        onClick = { showNoAccountDialog = true },
        content = {
            val contentDescription = stringResource(Res.string.no_account_help_content_description)
            Row(
                modifier = Modifier.semantics {
                    this.role = Role.Button
                    this.contentDescription = contentDescription
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Help,
                    contentDescription = null,
                )
                Text(stringResource(Res.string.no_account_help_button))
            }
        },
    )
    if (showNoAccountDialog) NoAccountDialog {
        showNoAccountDialog = false
    }
}

@Composable
private fun NoAccountDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.no_account_dialog_title),
            )
        },
        text = {
            Text(
                text = stringResource(Res.string.no_account_dialog_details),
            )
        },
        dismissButton = {
            val uriHandler = LocalUriHandler.current

            TextButton(
                onClick = {
                    runCatching { uriHandler.openUri(AtProtoWebsiteUrl) }
                },
            ) {
                Text(
                    text = stringResource(Res.string.learn_more),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    text = stringResource(Res.string.okay),
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

private const val AtProtoWebsiteUrl = "https://atproto.com/"
