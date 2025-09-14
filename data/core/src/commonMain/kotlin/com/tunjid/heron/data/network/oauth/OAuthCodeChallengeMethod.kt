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

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256

/**
 * Represents an OAuth code challenge method that the OAuth server will use to verify the code challenge.
 */
abstract class OAuthCodeChallengeMethod(open val method: String) {
    abstract suspend fun provideCodeChallenge(codeVerifier: String): String

    /**
     * The "plain" code challenge method, which does not apply any transformation to the code verifier.
     * This is not secure and is generally not recommended for production use.
     */
    data object Plain : OAuthCodeChallengeMethod("plain") {
        override suspend fun provideCodeChallenge(codeVerifier: String): String = codeVerifier
    }

    /**
     * The "S256" code challenge method, which applies SHA-256 hashing to the code verifier.
     * This must be supported by all clients and Authorization Servers; see
     * [RFC 7636](https://datatracker.ietf.org/doc/html/rfc7636) for details.
     *
     * The transform involves a relatively simple SHA-256 hash and base64url string encoding. The code value is a set of
     * 32 to 96 random bytes, encoded in base64url (resulting in 43 or more string-encoded characters).
     */
    data object S256 : OAuthCodeChallengeMethod("S256") {
        override suspend fun provideCodeChallenge(codeVerifier: String): String {
            val digest = CryptographyProvider.Default.get(SHA256)
                .hasher()
                .hash(codeVerifier.encodeToByteArray())
            return digest.encodeBase64Url()
        }
    }
}
