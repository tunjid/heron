/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.signin

import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.tunjid.heron.data.local.models.SessionRequest
import com.tunjid.heron.navigation.NavigationAction
import com.tunjid.heron.navigation.NavigationMutation
import com.tunjid.treenav.pop
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class FormField(
    val id: String,
    val value: String,
    val transformation: VisualTransformation,
)

fun List<FormField>.update(updatedField: FormField) = map { field ->
    if (field.id == updatedField.id) updatedField
    else field
}

@Serializable
data class State(
    val isSignedIn: Boolean = false,
    val isSubmitting: Boolean = false,
    val fields: List<FormField> = listOf(
        FormField(
            id = "username",
            value = "",
            transformation = VisualTransformation.None,
        ),
        FormField(
            id = "password",
            value = "",
            transformation = PasswordVisualTransformation()
        )
    ),
    @Transient
    val messages: List<String> = emptyList(),
)

val State.submitButtonEnabled: Boolean get() = !isSignedIn && !isSubmitting

val State.sessionRequest: SessionRequest
    get() = fields.associateBy { it.id }.let { formMap ->
        SessionRequest(
            username = formMap.getValue("username").value,
            password = formMap.getValue("password").value,
        )
    }

sealed class Action(key: String) {
    data class FieldChanged(val field: FormField) : Action("FieldChanged")
    data class Submit(val request: SessionRequest) : Action("Submit")
    data class MessageConsumed(
        val message: String
    ) : Action("MessageConsumed")

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data object Pop : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        }
    }
}