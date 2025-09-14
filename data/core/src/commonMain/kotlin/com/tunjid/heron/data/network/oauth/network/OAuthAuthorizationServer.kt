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

package com.tunjid.heron.data.network.oauth.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OAuthAuthorizationServer(
    val issuer: String,
    @SerialName("request_parameter_supported")
    val requestParameterSupported: Boolean,
    @SerialName("request_uri_parameter_supported")
    val requestUriParameterSupported: Boolean,
    @SerialName("require_request_uri_registration")
    val requireRequestUriRegistration: Boolean = true,
    @SerialName("scopes_supported")
    val scopesSupported: List<String>,
    @SerialName("subject_types_supported")
    val subjectTypesSupported: List<String>,
    @SerialName("response_types_supported")
    val responseTypesSupported: List<String>,
    @SerialName("response_modes_supported")
    val responseModesSupported: List<String>,
    @SerialName("grant_types_supported")
    val grantTypesSupported: List<String>,
    @SerialName("code_challenge_methods_supported")
    val codeChallengeMethodsSupported: List<String>,
    @SerialName("ui_locales_supported")
    val uiLocalesSupported: List<String>,
    @SerialName("display_values_supported")
    val displayValuesSupported: List<String>,
    @SerialName("request_object_signing_alg_values_supported")
    val requestObjectSigningAlgValuesSupported: List<String>,
    @SerialName("authorization_response_iss_parameter_supported")
    val authorizationResponseIssParameterSupported: Boolean = true,
    @SerialName("request_object_encryption_alg_values_supported")
    val requestObjectEncryptionAlgValuesSupported: List<String>,
    @SerialName("request_object_encryption_enc_values_supported")
    val requestObjectEncryptionEncValuesSupported: List<String>,
    @SerialName("jwks_uri")
    val jwksUri: String,
    @SerialName("authorization_endpoint")
    val authorizationEndpoint: String,
    @SerialName("token_endpoint")
    val tokenEndpoint: String,
    @SerialName("token_endpoint_auth_methods_supported")
    val tokenEndpointAuthMethodsSupported: List<String>,
    @SerialName("token_endpoint_auth_signing_alg_values_supported")
    val tokenEndpointAuthSigningAlgValuesSupported: List<String>,
    @SerialName("revocation_endpoint")
    val revocationEndpoint: String,
    @SerialName("introspection_endpoint")
    val introspectionEndpoint: String,
    @SerialName("pushed_authorization_request_endpoint")
    val pushedAuthorizationRequestEndpoint: String,
    @SerialName("require_pushed_authorization_requests")
    val requirePushedAuthorizationRequests: Boolean = true,
    @SerialName("dpop_signing_alg_values_supported")
    val dpopSigningAlgValuesSupported: List<String>,
    @SerialName("client_id_metadata_document_supported")
    val clientIdMetadataDocumentSupported: Boolean,
)
