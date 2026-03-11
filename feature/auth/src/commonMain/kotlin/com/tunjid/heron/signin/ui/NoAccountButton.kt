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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.tunjid.heron.data.core.models.Server
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.PrimaryDialogButton
import com.tunjid.heron.ui.SimpleDialog
import com.tunjid.heron.ui.SimpleDialogText
import com.tunjid.heron.ui.SimpleDialogTitle
import com.tunjid.heron.ui.text.CommonStrings
import heron.feature.auth.generated.resources.Res
import heron.feature.auth.generated.resources.learn_more
import heron.feature.auth.generated.resources.no_account_dialog_details
import heron.feature.auth.generated.resources.no_account_dialog_title
import heron.feature.auth.generated.resources.no_account_help_button
import heron.feature.auth.generated.resources.no_account_help_content_description
import heron.ui.core.generated.resources.dismiss
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
    var selectedServer by remember {
        mutableStateOf(AtProtoServer)
    }
    SimpleDialog(
        onDismissRequest = onDismiss,
        title = {
            SimpleDialogTitle(
                text = stringResource(Res.string.no_account_dialog_title),
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SimpleDialogText(
                    text = stringResource(Res.string.no_account_dialog_details),
                )

                Row(
                    modifier = Modifier
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    LearnMoreServers.forEach { server ->
                        IconButton(
                            onClick = {
                                selectedServer = server
                            },
                            content = {
                                Icon(
                                    modifier = Modifier
                                        .size(30.dp),
                                    imageVector = server.logo,
                                    contentDescription = stringResource(server.stringResource),
                                    tint =
                                    if (selectedServer == server) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                )
                            },
                        )
                    }
                }
            }
        },
        dismissButton = {
            NeutralDialogButton(
                text = stringResource(CommonStrings.dismiss),
                onClick = onDismiss,
            )
        },
        confirmButton = {
            val uriHandler = LocalUriHandler.current
            AnimatedContent(
                targetState = selectedServer,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
            ) { server ->
                PrimaryDialogButton(
                    text = stringResource(
                        Res.string.learn_more,
                        stringResource(server.stringResource),
                    ),
                    onClick = {
                        runCatching {
                            uriHandler.openUri(server.webPage())
                        }
                    },
                )
            }
        },
    )
}

private fun Server.webPage(): String = when (this) {
    Server.BlueSky -> "https://bsky.social"
    Server.BlackSky -> "https://blacksky.community"
    Server.EuroSky -> "https://eurosky.tech"
    Server.Pckt -> "https://pckt.blog"
    else -> endpoint
}

private val LearnMoreServers = listOf(AtProtoServer) + Server.KnownServers
