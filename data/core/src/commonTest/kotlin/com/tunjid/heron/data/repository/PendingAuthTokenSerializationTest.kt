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

package com.tunjid.heron.data.repository

import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.helper.SavedStateSerializationHelper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
class PendingAuthTokenSerializationTest {

    private val proto = SavedStateSerializationHelper.proto

    @Test
    fun signUpPendingToken_roundTripsWithoutHandle() {
        val pending = pendingToken(
            profileHandle = null,
        )
        assertEquals(
            expected = pending,
            actual = proto.decodeFromByteArray(
                deserializer = SavedState.AuthTokens.serializer(),
                bytes = proto.encodeToByteArray(
                    serializer = SavedState.AuthTokens.serializer(),
                    value = pending,
                ),
            ),
        )
    }

    @Test
    fun signInPendingToken_roundTripsWithHandle() {
        val pending = pendingToken(
            profileHandle = ProfileHandle("alice.bsky.social"),
        )
        assertEquals(
            expected = pending,
            actual = proto.decodeFromByteArray(
                deserializer = SavedState.AuthTokens.serializer(),
                bytes = proto.encodeToByteArray(
                    serializer = SavedState.AuthTokens.serializer(),
                    value = pending,
                ),
            ),
        )
    }

    @Test
    fun pendingTokenWithNonNullHandleField_decodesIntoCurrentModel() {
        val legacy = LegacyPendingDPoP(
            profileHandle = ProfileHandle("alice.bsky.social"),
            endpoint = Endpoint,
            authorizeRequestUrl = AuthorizeRequestUrl,
            codeVerifier = CodeVerifier,
            nonce = Nonce,
            state = State,
            expiresAt = ExpiresAt,
        )
        assertEquals(
            expected = pendingToken(
                profileHandle = ProfileHandle("alice.bsky.social"),
            ),
            actual = proto.decodeFromByteArray(
                deserializer = SavedState.AuthTokens.Pending.DPoP.serializer(),
                bytes = proto.encodeToByteArray(
                    serializer = LegacyPendingDPoP.serializer(),
                    value = legacy,
                ),
            ),
        )
    }

    private fun pendingToken(
        profileHandle: ProfileHandle?,
    ) = SavedState.AuthTokens.Pending.DPoP(
        profileHandle = profileHandle,
        endpoint = Endpoint,
        authorizeRequestUrl = AuthorizeRequestUrl,
        codeVerifier = CodeVerifier,
        nonce = Nonce,
        state = State,
        expiresAt = ExpiresAt,
    )
}

/**
 * [SavedState.AuthTokens.Pending.DPoP] as persisted before sign up existed, when every pending
 * token had a handle. Fields are positional in protobuf, so the order must match.
 */
@Serializable
private data class LegacyPendingDPoP(
    val profileHandle: ProfileHandle,
    val endpoint: String,
    val authorizeRequestUrl: String,
    val codeVerifier: String,
    val nonce: String,
    val state: String,
    val expiresAt: Instant,
    val keyPair: SavedState.AuthTokens.Authenticated.DPoP.DERKeyPair? = null,
)

private const val Endpoint = "https://bsky.social"
private const val AuthorizeRequestUrl = "https://bsky.social/oauth/authorize?request_uri=urn"
private const val CodeVerifier = "verifier"
private const val Nonce = "nonce"
private const val State = "state"
private val ExpiresAt = Instant.fromEpochSeconds(1_700_000_000)
