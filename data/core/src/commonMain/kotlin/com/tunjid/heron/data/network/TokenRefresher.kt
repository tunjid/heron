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

package com.tunjid.heron.data.network

import com.atproto.server.RefreshSessionResponse
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.network.oauth.DpopKeyPair
import com.tunjid.heron.data.network.oauth.OAuthApi
import com.tunjid.heron.data.network.oauth.OAuthToken
import com.tunjid.heron.data.repository.SavedState
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.buildUrl
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.long
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.response.AtpErrorDescription
import sh.christian.ozone.api.response.AtpException
import sh.christian.ozone.api.response.StatusCode

internal interface TokenRefresher {

    /**
     * Returns new tokens for [tokens]. Throws [SessionEndedException] if the refresh token
     * is invalid. Other failures, like being offline, a server outage or an unexpected
     * response, are rethrown.
     */
    suspend fun refresh(
        tokens: SavedState.AuthTokens.Authenticated,
    ): SavedState.AuthTokens.Authenticated
}

internal class SessionEndedException(
    cause: Throwable,
) : Exception(
    "The session can no longer be refreshed",
    cause,
)

@Inject
internal class NetworkTokenRefresher(
    httpClient: HttpClient,
    private val identityResolver: IdentityResolver,
    private val nonces: DPoPNonces,
) : TokenRefresher {

    private val client = httpClient.config {
        installOrReplace(HttpTimeout) {
            requestTimeoutMillis = 15.seconds.inWholeMilliseconds
        }
    }

    private val oAuthApi = OAuthApi(
        client = client,
    )

    override suspend fun refresh(
        tokens: SavedState.AuthTokens.Authenticated,
    ): SavedState.AuthTokens.Authenticated = try {
        when (tokens) {
            is SavedState.AuthTokens.Authenticated.Bearer -> refreshBearer(tokens)
            is SavedState.AuthTokens.Authenticated.DPoP -> refreshDPoP(tokens)
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        // Ending a session discards a refresh token that may still work, like when a captive
        // portal or a proxy answered instead of the server, so only the server's verdict ends it.
        throw if (exception.endsSession()) SessionEndedException(exception) else exception
    }

    private suspend fun refreshBearer(
        tokens: SavedState.AuthTokens.Authenticated.Bearer,
    ): SavedState.AuthTokens.Authenticated.Bearer {
        val response = client.post(
            url = buildUrl {
                takeFrom(tokens.authEndpoint)
                encodedPath = RefreshSessionPath
            },
            block = { bearerAuth(tokens.refresh) },
        )
        if (!response.status.isSuccess()) throw response.atpException()

        val refreshed = response.body<RefreshSessionResponse>()
        return SavedState.AuthTokens.Authenticated.Bearer(
            authProfileId = ProfileId(refreshed.did.did),
            auth = refreshed.accessJwt,
            refresh = refreshed.refreshJwt,
            didDoc = SavedState.AuthTokens.DidDoc.fromJsonContentOrEmpty(
                jsonContent = refreshed.didDoc,
            )
                .withResolvedPds(
                    did = refreshed.did,
                    identityResolver = identityResolver,
                ),
            authEndpoint = tokens.authEndpoint,
            expiresAt = refreshed.accessJwt.jwtExpiresAt(),
        )
    }

    private suspend fun refreshDPoP(
        tokens: SavedState.AuthTokens.Authenticated.DPoP,
    ): SavedState.AuthTokens.Authenticated.DPoP {
        val issuer = Url(tokens.issuerEndpoint)
        val oAuthToken = oAuthApi.refreshToken(
            clientId = tokens.clientId,
            nonce = nonces[issuer],
            refreshToken = tokens.refresh,
            keyPair = tokens.toKeyPair(),
            authorizationServer = issuer,
        )
        nonces.update(
            url = issuer,
            nonce = oAuthToken.nonce,
        )
        return oAuthToken.toAppToken(
            authEndpoint = tokens.issuerEndpoint,
            pdsUrl = tokens.pdsUrl,
            clientId = tokens.clientId,
        )
    }
}

private suspend fun HttpResponse.atpException() = AtpException(
    statusCode = StatusCode.fromCode(status.value),
    error = runCatching { body<AtpErrorDescription>() }.getOrNull(),
)

private fun Exception.endsSession(): Boolean =
    this is AtpException &&
        statusCode.code in RefreshTokenRejectedStatusCodes &&
        RefreshTokenRejectedErrors.any { rejectedError ->
            rejectedError.equals(
                other = error?.error,
                ignoreCase = true,
            )
        }

internal suspend fun SavedState.AuthTokens.Authenticated.DPoP.toKeyPair() =
    DpopKeyPair.fromKeyPair(
        publicKey = keyPair.publicKey,
        publicKeyFormat = DpopKeyPair.PublicKeyFormat.DER,
        privateKey = keyPair.privateKey,
        privateKeyFormat = DpopKeyPair.PrivateKeyFormat.DER,
    )

internal suspend fun OAuthToken.toAppToken(
    authEndpoint: String,
    pdsUrl: String,
    clientId: String,
) = SavedState.AuthTokens.Authenticated.DPoP(
    authProfileId = subject.did.let(::ProfileId),
    auth = accessToken,
    refresh = refreshToken,
    pdsUrl = pdsUrl,
    keyPair = SavedState.AuthTokens.Authenticated.DPoP.DERKeyPair(
        publicKey = keyPair.publicKey(DpopKeyPair.PublicKeyFormat.DER),
        privateKey = keyPair.privateKey(DpopKeyPair.PrivateKeyFormat.DER),
    ),
    clientId = clientId,
    nonce = nonce,
    issuerEndpoint = authEndpoint,
    expiresAt = Clock.System.now() + expiresIn,
)

internal suspend fun SavedState.AuthTokens.DidDoc.withResolvedPds(
    did: Did,
    identityResolver: IdentityResolver,
): SavedState.AuthTokens.DidDoc {
    if (service.isNotEmpty()) return this
    val pdsUrl = identityResolver.resolvePds(did) ?: return this
    return copy(
        service = listOf(
            SavedState.AuthTokens.DidDoc.Service(
                id = AtProtoPdsServiceId,
                type = AtProtoPdsServiceType,
                serviceEndpoint = pdsUrl.toString(),
            ),
        ),
    )
}

internal fun String.jwtExpiresAt(): Instant? = runCatching {
    val payload = split('.')[1]
    val json = Base64.UrlSafe
        .withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
        .decode(payload)
        .decodeToString()
    (BlueskyJson.parseToJsonElement(json).jsonObject.getValue(JwtExpiryClaim) as JsonPrimitive).long
}
    .getOrNull()
    ?.let(Instant::fromEpochSeconds)

private val RefreshTokenRejectedStatusCodes = setOf(
    HttpStatusCode.BadRequest.value,
    HttpStatusCode.Unauthorized.value,
)

// OAuth's verdict on a spent, revoked or expired refresh token, and the PDS's for a session.
private val RefreshTokenRejectedErrors = setOf(
    "invalid_grant",
    "ExpiredToken",
    "InvalidToken",
)

private const val RefreshSessionPath = "/xrpc/com.atproto.server.refreshSession"
private const val AtProtoPdsServiceId = "#atproto_pds"
private const val AtProtoPdsServiceType = "AtprotoPersonalDataServer"
private const val JwtExpiryClaim = "exp"
