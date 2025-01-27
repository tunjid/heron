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

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.tunjid.heron.data.local.models.SessionRequest
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.treenav.pop
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.jvm.JvmInline

@Serializable
data class FormField(
    val id: Id,
    val value: String,
    val maxLines: Int,
    @Transient
    val leadingIcon: ImageVector? = null,
    @Transient
    val transformation: VisualTransformation = VisualTransformation.None,
    @Transient
    val autofillTypes: List<AutofillType> = emptyList(),
    @Transient
    val keyboardOptions: KeyboardOptions = KeyboardOptions(),
) {
    @Serializable
    @JvmInline
    value class Id(
        private val id: String,
    ) {
        override fun toString(): String = id
    }
}

fun List<FormField>.update(updatedField: FormField) = map { field ->
    if (field.id == updatedField.id) updatedField
    else field
}

internal val Username = FormField.Id("username")
internal val Password = FormField.Id("password")

@Serializable
data class State(
    val isSignedIn: Boolean = false,
    val isSubmitting: Boolean = false,
    val fields: List<FormField> = listOf(
        FormField(
            id = Username,
            value = "",
            maxLines = 1,
            leadingIcon = Icons.Rounded.AccountCircle,
            transformation = VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next,
            ),
        ),
        FormField(
            id = Password,
            value = "",
            maxLines = 1,
            leadingIcon = Icons.Rounded.Lock,
            transformation = PasswordVisualTransformation(),
            autofillTypes = listOf(AutofillType.Password),
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
        )
    ),
    @Transient
    val messages: List<String> = emptyList(),
)

val State.submitButtonEnabled: Boolean get() = !isSignedIn && !isSubmitting

val State.sessionRequest: SessionRequest
    get() = fields.associateBy { it.id }.let { formMap ->
        SessionRequest(
            username = formMap.getValue(Username).value,
            password = formMap.getValue(Password).value,
        )
    }

sealed class Action(val key: String) {
    data class FieldChanged(val field: FormField) : Action("FieldChanged")
    data class Submit(val request: SessionRequest) : Action("Submit")
    data class MessageConsumed(
        val message: String,
    ) : Action("MessageConsumed")

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data object Pop : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        }
    }
}