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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.tunjid.heron.data.core.models.Server
import com.tunjid.heron.data.core.models.SessionRequest
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.scaffold.ScaffoldMessage
import com.tunjid.heron.signin.oauth.OauthFlowResult
import com.tunjid.heron.signin.ui.FormField
import com.tunjid.heron.signin.ui.Validator
import heron.feature.auth.generated.resources.Res
import heron.feature.auth.generated.resources.empty_form
import heron.feature.auth.generated.resources.invalid_domain
import heron.feature.auth.generated.resources.password
import heron.feature.auth.generated.resources.username
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

internal val Username = FormField.Id("username")
internal val Password = FormField.Id("password")

internal val DomainRegex = Regex(
    pattern = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}\$",
)

@Serializable
sealed class AuthMode {
    @Serializable
    data object Oauth : AuthMode()

    @Serializable
    data object Password : AuthMode()
}

@Serializable
data class State(
    val isSignedIn: Boolean = false,
    val isSubmitting: Boolean = false,
    val isOauthAvailable: Boolean = false,
    val oauthRequestUri: GenericUri? = null,
    val selectedServer: Server = Server.BlueSky,
    val availableServers: List<Server> = listOf(
        Server.BlueSky,
        Server.BlackSky,
        Server(
            endpoint = "https://custom.app",
            supportsOauth = false,
        ),
    ),
    val showCustomServerPopup: Boolean = false,
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
            contentDescription = ScaffoldMessage.Resource(Res.string.username),
            validator = Validator(
                String::isNotBlank to ScaffoldMessage.Resource(
                    Res.string.empty_form,
                    listOf(Res.string.username),
                ),
                DomainRegex::matches to ScaffoldMessage.Resource(
                    Res.string.invalid_domain,
                ),
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
            contentDescription = ScaffoldMessage.Resource(Res.string.password),
            validator = Validator(
                String::isNotBlank to ScaffoldMessage.Resource(
                    Res.string.empty_form,
                    listOf(Res.string.password),
                ),
            ),
        ),
    ),
    @Transient
    val messages: List<ScaffoldMessage> = emptyList(),
)

val State.submitButtonEnabled: Boolean get() = !isSignedIn && !isSubmitting

val State.profileHandle: ProfileHandle
    get() = ProfileHandle(id = fields.first { it.id == Username }.value)

val State.canSignInLater: Boolean
    get() = fields.all { field ->
        field.value.isBlank()
    }

val State.credentialSessionRequest: SessionRequest
    get() = fields.associateBy { it.id }.let { formMap ->
        SessionRequest.Credentials(
            handle = ProfileHandle(formMap.getValue(Username).value),
            password = formMap.getValue(Password).value,
            server = selectedServer,
        )
    }

val State.hasEnteredPassword: Boolean
    get() = fields
        .firstOrNull { it.id == Password }
        ?.value
        ?.isNotBlank()
        ?: false

val State.authMode: AuthMode
    get() = when {
        isOauthAvailable && selectedServer.supportsOauth && !hasEnteredPassword -> AuthMode.Oauth
        else -> AuthMode.Password
    }


sealed class Action(val key: String) {
    data class FieldChanged(
        val id: FormField.Id,
        val text: String,
    ) : Action("FieldChanged")

    data class OauthAvailabilityChanged(
        val isOauthAvailable: Boolean,
    ) : Action("OauthAvailabilityChanged")

    data class BeginOauthFlow(
        val handle: ProfileHandle,
    ) : Action("BeginOauthFlow")

    data class OauthFlowResultAvailable(
        val handle: ProfileHandle,
        val result: OauthFlowResult,
    ) : Action("OauthFlowResultAvailable")

    data class SetServer(
        val server: Server,
    ) : Action("SetServer")

    sealed class Submit : Action("Submit") {
        data object GuestAuth : Submit()
        data class Auth(
            val request: SessionRequest,
        ) : Submit()
    }

    data class MessageConsumed(
        val message: ScaffoldMessage,
    ) : Action("MessageConsumed")

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction
}
