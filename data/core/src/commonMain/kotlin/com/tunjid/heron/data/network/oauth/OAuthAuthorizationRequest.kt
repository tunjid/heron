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

/**
 * Represents an OAuth authorization request.
 *
 * @param authorizeRequestUrl The URL to which the user should be redirected to authorize the request.
 * @param expiresIn The duration for which the authorization request is valid.
 * @param codeVerifier A unique string that will be used to verify the token request.
 * @param state A unique string to maintain state between the request and callback.
 * @param nonce A unique string to prevent replay attacks.
 */
@Serializable
data class OAuthAuthorizationRequest(
    val authorizeRequestUrl: String,
    val expiresIn: Duration,
    val codeVerifier: String,
    val state: String,
    val nonce: String,
)
