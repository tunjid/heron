/*
 *    MIT License
 *
 *    Copyright (c) 2023 Christian De Angelis
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy
 *    of this software and associated documentation files (the "Software"), to deal
 *    in the Software without restriction, including without limitation the rights
 *    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *    copies of the Software, and to permit persons to whom the Software is
 *    furnished to do so, subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included in all
 *    copies or substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *    SOFTWARE.
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
 * @param refreshToken The refresh token used to obtain a new access token when the current one
 *   expires.
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
    private val payloadJwt: String by lazy { accessToken.split(".")[1] }
    private val payloadJson: JsonObject by lazy {
        Json.decodeFromString(JsonObject.serializer(), payloadJwt.decodeBase64String())
    }

    /** The unique identifier for the OAuth client. */
    val clientId: String by lazy { requirePayload("client_id") }

    /**
     * The audience of the JWT, typically the DID of the PDS (Personal Data Server) that the token
     * is intended for.
     */
    val audience: Did by lazy { Did(requirePayload("aud")) }

    /** The URL of the PDS (Personal Data Server) associated with the audience. */
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
