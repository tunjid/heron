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

package com.tunjid.heron.scaffold.scaffold

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import heron.scaffold.generated.resources.Res
import heron.scaffold.generated.resources.dismiss
import heron.scaffold.generated.resources.sign_in
import heron.scaffold.generated.resources.sign_in_prompt
import heron.scaffold.generated.resources.signed_out
import org.jetbrains.compose.resources.stringResource

@Stable
class SignInPopUpState private constructor() {
    internal var visible by mutableStateOf(false)

    fun show() {
        visible = true
    }

    companion object {
        @Composable
        fun rememberSignInPopUpState(
            onSignInClicked: () -> Unit
        ): SignInPopUpState {
            val state = remember(::SignInPopUpState)

            SignInPopUp(
                state = state,
                onSignInClicked = onSignInClicked,
            )
            return state
        }
    }
}

@Composable
private fun SignInPopUp(
    state: SignInPopUpState,
    onSignInClicked: () -> Unit
) {

    if (!state.visible) return

    AlertDialog(
        onDismissRequest = {
            state.visible = false
        },
        title = {
            Text(
                text = stringResource(Res.string.signed_out),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = stringResource(Res.string.sign_in_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = onSignInClicked,
                content = {
                    Text(
                        text = stringResource(Res.string.sign_in),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        },
        dismissButton = {
            TextButton(
                onClick = {
                    state.visible = false
                },
                content = {
                    Text(
                        text = stringResource(Res.string.dismiss),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = MaterialTheme.shapes.medium
    )

}