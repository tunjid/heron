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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Server
import com.tunjid.heron.scaffold.scaffold.ScaffoldMessage
import com.tunjid.heron.signin.DomainRegex
import com.tunjid.heron.ui.ItemSelection
import heron.feature.auth.generated.resources.Res
import heron.feature.auth.generated.resources.blacksky_server
import heron.feature.auth.generated.resources.bluesky_server
import heron.feature.auth.generated.resources.cancel
import heron.feature.auth.generated.resources.custom_server
import heron.feature.auth.generated.resources.empty_form
import heron.feature.auth.generated.resources.invalid_domain
import heron.feature.auth.generated.resources.submit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun ServerSelection(
    modifier: Modifier = Modifier,
    selectedServer: Server,
    availableServers: List<Server>,
    onServerSelected: (Server) -> Unit,
) {
    ItemSelection(
        modifier = modifier,
        selectedItem = selectedServer,
        availableItems = availableServers,
        key = Server::key,
        icon = Server::logo,
        stringResource = Server::stringResource,
        onItemSelected = onServerSelected,
    )
}

@Stable
class ServerSelectionSheetState private constructor(
    internal val sheetState: SheetState,
    internal val scope: CoroutineScope,
) {
    var currentServer by mutableStateOf<Server?>(null)
        internal set

    var showBottomSheet by mutableStateOf(false)
        internal set

    fun onServer(interaction: Server) {
        currentServer = interaction
    }

    internal fun hideSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                showBottomSheet = false
                currentServer = null
            }
        }
    }

    companion object {
        @Composable
        fun rememberUpdatedServerSelectionState(
            onServerConfirmed: (Server) -> Unit,
        ): ServerSelectionSheetState {
            val sheetState = rememberModalBottomSheetState()
            val scope = rememberCoroutineScope()

            val state = remember(sheetState, scope) {
                ServerSelectionSheetState(
                    sheetState = sheetState,
                    scope = scope,
                )
            }

            ServerSelectionBottomSheet(
                state = state,
                onServerConfirmed = onServerConfirmed,
            )

            return state
        }
    }
}

@Composable
private fun ServerSelectionBottomSheet(
    state: ServerSelectionSheetState,
    onServerConfirmed: (Server) -> Unit,
) {
    LaunchedEffect(state.currentServer) {
        val server = state.currentServer
        when (server?.endpoint) {
            null -> Unit
            Server.BlueSky.endpoint,
            Server.BlackSky.endpoint,
            -> {
                onServerConfirmed(server)
                state.currentServer = null
            }

            else -> state.showBottomSheet = true
        }
    }

    if (state.showBottomSheet) ModalBottomSheet(
        onDismissRequest = {
            state.showBottomSheet = false
        },
        sheetState = state.sheetState,
        content = {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                var customServerFormField by remember {
                    mutableStateOf(CustomServerFormField)
                }
                val maybeSelectServer = {
                    customServerFormField = customServerFormField.validated()
                    if (customServerFormField.isValid) {
                        onServerConfirmed(customServerFormField.asServer())
                        state.hideSheet()
                    }
                }
                FormField(
                    modifier = Modifier
                        .fillMaxWidth(),
                    field = customServerFormField,
                    onValueChange = { _, newValue ->
                        customServerFormField = customServerFormField.copyWithValidation(newValue)
                    },
                    keyboardActions = {
                        maybeSelectServer()
                    },
                )

                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        maybeSelectServer()
                    },
                    content = {
                        Text(
                            text = stringResource(Res.string.submit)
                                .capitalize(Locale.current),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                )

                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        state.hideSheet()
                    },
                    content = {
                        Text(
                            text = stringResource(Res.string.cancel)
                                .capitalize(Locale.current),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
                Spacer(
                    Modifier.height(16.dp),
                )
            }
        },
    )
}

private fun FormField.asServer() = Server(
    endpoint = value,
    supportsOauth = false,
)

private val CustomServerFormField = FormField(
    id = FormField.Id("custom_server"),
    value = "",
    maxLines = 1,
    leadingIcon = Icons.Rounded.Public,
    transformation = VisualTransformation.None,
    keyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.None,
        autoCorrectEnabled = false,
        keyboardType = KeyboardType.Uri,
        imeAction = ImeAction.Done,
    ),
    contentDescription = ScaffoldMessage.Resource(Res.string.custom_server),
    validator = Validator(
        String::isNotBlank to ScaffoldMessage.Resource(
            Res.string.empty_form,
            listOf(Res.string.custom_server),
        ),
        DomainRegex::matches to ScaffoldMessage.Resource(
            Res.string.invalid_domain,
        ),
    ),
)

private val Server.key
    get() = when (this) {
        in Server.KnownServers -> endpoint
        else -> "custom"
    }

private val Server.logo
    get() = when (endpoint) {
        Server.BlueSky.endpoint -> Bluesky
        Server.BlackSky.endpoint -> Blacksky
        else -> Icons.Rounded.Public
    }

internal val Server.stringResource
    get() = when (endpoint) {
        Server.BlueSky.endpoint -> Res.string.bluesky_server
        Server.BlackSky.endpoint -> Res.string.blacksky_server
        else -> Res.string.custom_server
    }
