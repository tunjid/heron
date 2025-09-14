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

package com.tunjid.heron.data.network.oauth

import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.buildUrl
import io.ktor.util.decodeBase64String
import kotlin.time.Duration
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import sh.christian.ozone.api.Did

/**
 * Represents an OAuth token received after a successful authorization or refresh request.
 *
 * @param accessToken The access token used to authenticate API requests.
 * @param refreshToken The refresh token used to obtain a new access token when the current one expires.
 * @param keyPair The DPoP key pair used for signing requests.
 * @param expiresIn The duration for which the access token is valid.
 * @param scopes The list of scopes granted to the access token.
 * @param subject The DID of the user account associated with the token.
 * @param nonce A unique string to prevent replay attacks, typically used in conjunction with DPoP.
 */
@Serializable
data class OAuthToken(
    val accessToken: String,
    val refreshToken: String,
    val keyPair: DpopKeyPair,
    val expiresIn: Duration,
    val scopes: List<OAuthScope>,
    val subject: Did,
    val nonce: String,
) {
    private val payloadJwt: String by lazy {
        accessToken.split(".")[1]
    }
    private val payloadJson: JsonObject by lazy {
        Json.decodeFromString(JsonObject.serializer(), payloadJwt.decodeBase64String())
    }

    /**
     * The unique identifier for the OAuth client.
     */
    val clientId: String by lazy {
        requirePayload("client_id")
    }

    /**
     * The audience of the JWT, typically the DID of the PDS (Personal Data Server) that the token is intended for.
     */
    val audience: Did by lazy {
        Did(requirePayload("aud"))
    }

    /**
     * The URL of the PDS (Personal Data Server) associated with the audience.
     */
    val pds: Url by lazy {
        buildUrl {
            protocol = URLProtocol.HTTPS
            host = audience.toString().substringAfterLast(":")
        }
    }

    private fun requirePayload(key: String): String {
        return requireNotNull(payloadJson[key]?.let { (it as? JsonPrimitive)?.contentOrNull }) {
            "JWT payload does not contain '$key' claim"
        }
    }
}
