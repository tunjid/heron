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
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.local.models.SessionRequest
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.scaffold.SnackbarMessage
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
    val contentType: ContentType? = null,
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
            contentType = ContentType.Username,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
            ),
        ),
        FormField(
            id = Password,
            value = "",
            maxLines = 1,
            leadingIcon = Icons.Rounded.Lock,
            transformation = PasswordVisualTransformation(),
            contentType = ContentType.Password,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
        ),
    ),
    @Transient
    val messages: List<SnackbarMessage> = emptyList(),
)

val State.submitButtonEnabled: Boolean get() = !isSignedIn && !isSubmitting

val State.canSignInLater: Boolean
    get() = fields.all { field ->
        field.value.isBlank()
    }

val State.sessionRequest: SessionRequest
    get() = fields.associateBy { it.id }.let { formMap ->
        SessionRequest.Credentials(
            handle = ProfileHandle(formMap.getValue(Username).value),
            password = formMap.getValue(Password).value,
        )
    }

sealed class Action(val key: String) {
    data class FieldChanged(val field: FormField) : Action("FieldChanged")
    sealed class Submit : Action("Submit") {
        data object GuestAuth : Submit()
        data class Auth(
            val request: SessionRequest,
        ) : Submit()
    }

    data class MessageConsumed(
        val message: SnackbarMessage,
    ) : Action("MessageConsumed")

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction
}
