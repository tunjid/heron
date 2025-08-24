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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import heron.feature.auth.generated.resources.Res
import heron.feature.auth.generated.resources.password
import heron.feature.auth.generated.resources.username
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SignInScreen(
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        state.fields.forEach { field ->
            OutlinedTextField(
                modifier = Modifier
                    .semantics {
                        field.contentType?.let { contentType = it }
                    },
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
                                }
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
                            }
                        )
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
}

