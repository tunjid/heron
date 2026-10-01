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

import com.tunjid.heron.data.network.BlueskyJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class OAuthParRequestTest {

    @Test
    fun signUpRequest_omitsLoginHint() {
        val json = encode(
            loginHint = null,
            prompt = "create",
        )
        assertFalse("login_hint" in json)
        assertEquals(
            expected = JsonPrimitive("create"),
            actual = json["prompt"],
        )
    }

    @Test
    fun signInRequest_omitsPrompt() {
        val json = encode(
            loginHint = "alice.bsky.social",
            prompt = null,
        )
        assertFalse("prompt" in json)
        assertEquals(
            expected = JsonPrimitive("alice.bsky.social"),
            actual = json["login_hint"],
        )
    }

    private fun encode(
        loginHint: String?,
        prompt: String?,
    ): JsonObject = BlueskyJson.encodeToJsonElement(
        serializer = OAuthParRequest.serializer(),
        value = OAuthParRequest(
            responseType = "code",
            codeChallengeMethod = "S256",
            scope = "atproto",
            clientId = "https://heron.tunji.dev/oauth-client.json",
            redirectUri = "https://heron.tunji.dev/oauth/callback",
            codeChallenge = "challenge",
            state = "state",
            loginHint = loginHint,
            prompt = prompt,
        ),
    ).jsonObject
}
