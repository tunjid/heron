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

import kotlin.time.Duration
import kotlinx.serialization.Serializable
import sh.christian.ozone.api.Did

/**
 * Represents an OAuth token received after a successful authorization or refresh request.
 *
 * Per the [atproto OAuth spec](https://atproto.com/specs/oauth) the access token is opaque to the
 * client, so nothing here is derived by parsing it.
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
)
