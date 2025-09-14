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

package com.tunjid.heron.signin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.signin.oauth.rememberOauthFlowState
import heron.feature.auth.generated.resources.Res
import heron.feature.auth.generated.resources.password
import heron.feature.auth.generated.resources.sign_with_password
import heron.feature.auth.generated.resources.username
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SignInScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(state = scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        val oauthFlowState = rememberOauthFlowState { result ->
            actions(
                Action.OauthFlowResultAvailable(
                    handle = state.profileHandle,
                    result = result,
                ),
            )
        }

        val oauthFlowUri = rememberUpdatedState(state.oauthRequestUri)

        state.onFormFieldMatchingAuth { field ->
            key(field.id) {
                OutlinedTextField(
                    modifier = Modifier
                        .semantics {
                            field.contentType?.let { contentType = it }
                        }
                        .animateBounds(paneScaffoldState),
                    value = field.value,
                    maxLines = field.maxLines,
                    onValueChange = {
                        actions(Action.FieldChanged(field = field.copy(value = it)))
                    },
                    shape = MaterialTheme.shapes.large,
                    visualTransformation = field.transformation,
                    keyboardOptions = field.keyboardOptions,
                    keyboardActions = KeyboardActions {
                        when (field.id) {
                            Username -> focusManager.moveFocus(
                                focusDirection = FocusDirection.Next,
                            )

                            Password -> if (state.submitButtonEnabled) {
                                actions(
                                    when {
                                        state.canSignInLater -> Action.Submit.GuestAuth
                                        else -> Action.Submit.Auth(state.sessionRequest)
                                    },
                                )
                                keyboardController?.hide()
                            }
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(
                                when (field.id) {
                                    Username -> Res.string.username
                                    Password -> Res.string.password
                                    else -> throw IllegalArgumentException()
                                },
                            ),
                        )
                    },
                    leadingIcon = {
                        if (field.leadingIcon != null) {
                            Icon(
                                imageVector = field.leadingIcon,
                                contentDescription = stringResource(Res.string.password),
                            )
                        }
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = state.isOauthAvailable && state.profileHandle.id.isNotBlank(),
            modifier = Modifier
                .animateBounds(paneScaffoldState),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.authMode == AuthMode.UserSelectable.Password,
                    onCheckedChange = { checked ->
                        actions(
                            Action.SetAuthMode(
                                if (checked) AuthMode.UserSelectable.Password
                                else AuthMode.UserSelectable.Oauth,
                            ),
                        )
                    },
                )
                Text(stringResource(Res.string.sign_with_password))
            }
        }

        LaunchedEffect(Unit) {
            snapshotFlow { oauthFlowState.supportsOauth }
                .collectLatest { supportsOauth ->
                    actions(Action.OauthAvailabilityChanged(supportsOauth))
                }
        }

        LaunchedEffect(Unit) {
            snapshotFlow { oauthFlowUri.value }
                .filterNotNull()
                .collectLatest(oauthFlowState::launch)
        }
    }
}
