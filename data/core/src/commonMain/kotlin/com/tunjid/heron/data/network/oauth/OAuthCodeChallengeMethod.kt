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

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256

/**
 * Represents an OAuth code challenge method that the OAuth server will use to verify the code
 * challenge.
 */
abstract class OAuthCodeChallengeMethod(open val method: String) {
    abstract suspend fun provideCodeChallenge(codeVerifier: String): String

    /**
     * The "plain" code challenge method, which does not apply any transformation to the code
     * verifier. This is not secure and is generally not recommended for production use.
     */
    data object Plain : OAuthCodeChallengeMethod("plain") {
        override suspend fun provideCodeChallenge(codeVerifier: String): String = codeVerifier
    }

    /**
     * The "S256" code challenge method, which applies SHA-256 hashing to the code verifier. This
     * must be supported by all clients and Authorization Servers; see
     * [RFC 7636](https://datatracker.ietf.org/doc/html/rfc7636) for details.
     *
     * The transform involves a relatively simple SHA-256 hash and base64url string encoding. The
     * code value is a set of 32 to 96 random bytes, encoded in base64url (resulting in 43 or more
     * string-encoded characters).
     */
    data object S256 : OAuthCodeChallengeMethod("S256") {
        override suspend fun provideCodeChallenge(codeVerifier: String): String {
            val digest =
                CryptographyProvider.Default.get(SHA256)
                    .hasher()
                    .hash(codeVerifier.encodeToByteArray())
            return digest.encodeBase64Url()
        }
    }
}
