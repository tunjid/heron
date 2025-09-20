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

import com.atproto.identity.ResolveHandleQueryParams
import com.atproto.server.RefreshSessionResponse
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.lexicons.BlueskyApi
import com.tunjid.heron.data.lexicons.XrpcBlueskyApi
import com.tunjid.heron.data.lexicons.XrpcSerializersModule
import com.tunjid.heron.data.local.models.SessionRequest
import com.tunjid.heron.data.network.oauth.DpopKeyPair
import com.tunjid.heron.data.network.oauth.OAuthApi
import com.tunjid.heron.data.network.oauth.OAuthAuthorizationRequest
import com.tunjid.heron.data.network.oauth.OAuthClient
import com.tunjid.heron.data.network.oauth.OAuthScope
import com.tunjid.heron.data.network.oauth.OAuthToken
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.signedInAuth
import com.tunjid.heron.data.utilities.InvalidTokenException
import com.tunjid.heron.data.utilities.runCatchingWithNetworkRetry
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import sh.christian.ozone.api.Handle
import sh.christian.ozone.api.response.AtpResponse
import sh.christian.ozone.api.runtime.buildXrpcJsonConfiguration

interface NetworkService {
    val api: BlueskyApi

    suspend fun beginOauthFlowUri(
        handle: ProfileHandle,
    ): GenericUri

    suspend fun finishOauthFlow(
        request: SessionRequest.Oauth,
    ): SavedState.AuthTokens.Authenticated.DPoP

    suspend fun <T : Any> runCatchingWithMonitoredNetworkRetry(
        times: Int = 3,
        initialDelay: Long = 100, // 0.1 second
        maxDelay: Long = 5000, // 1 second
        factor: Double = 2.0,
        block: suspend BlueskyApi.() -> AtpResponse<T>,
    ): Result<T>
}

@Inject
class KtorNetworkService(
    savedStateDataSource: SavedStateDataSource,
    private val networkMonitor: NetworkMonitor,
) : NetworkService {

    private var pendingOauthSession: OauthSession? = null

    private val httpClient = HttpClient {
        expectSuccess = false

        install(DefaultRequest) {
            url.takeFrom(BaseEndpoint)
        }

        install(ContentNegotiation) {
            json(
                json = BlueskyJson,
                contentType = ContentType.Application.Json,
            )
        }

        install(
            atProtoAuth(
                networkErrorConverter = BlueskyJson::decodeFromString,
                readAuth = savedStateDataSource.signedInAuth::first,
                saveAuth = savedStateDataSource::setAuth,
                authenticate = ::authenticate,
                refresh = ::refresh,
            ),
        )

        install(Logging) {
            level = LogLevel.INFO
            logger = object : Logger {
                override fun log(message: String) {
//                        println("Logger Ktor => $message")
                }
            }
        }
    }

    private val oAuthApi: OAuthApi = OAuthApi(
        httpClient = httpClient,
    )

    override val api = XrpcBlueskyApi(httpClient)

    override suspend fun beginOauthFlowUri(
        handle: ProfileHandle,
    ) = oAuthApi.buildAuthorizationRequest(
        oauthClient = HeronOauthClient,
        scopes = HeronOauthScopes,
        loginHandleHint = handle.id,
    )
        .also {
            pendingOauthSession = OauthSession(handle = handle, request = it)
        }
        .authorizeRequestUrl
        .let(::GenericUri)

    override suspend fun finishOauthFlow(
        request: SessionRequest.Oauth,
    ): SavedState.AuthTokens.Authenticated.DPoP {
        val pendingRequest = pendingOauthSession
            ?: throw IllegalStateException("Expired authentication session")

        try {
            val callbackUrl = Url(request.callbackUri.uri)

            val code = callbackUrl.parameters[OauthCallbackUriCodeParam]
                ?: throw IllegalStateException("No auth code")

            val oAuthToken = oAuthApi.requestToken(
                oauthClient = HeronOauthClient,
                nonce = pendingRequest.request.nonce,
                codeVerifier = pendingRequest.request.codeVerifier,
                code = code,
            )

            val callingDid = api.resolveHandle(
                ResolveHandleQueryParams(Handle(pendingRequest.handle.id)),
            )
                .requireResponse()
                .did

            if (oAuthToken.subject != callingDid) {
                throw IllegalStateException("Invalid login session")
            }

            return oAuthToken.toAppToken()
        } finally {
            pendingOauthSession = null
        }
    }

    override suspend fun <T : Any> runCatchingWithMonitoredNetworkRetry(
        times: Int,
        initialDelay: Long, // 0.1 second
        maxDelay: Long, // 1 second
        factor: Double,
        block: suspend BlueskyApi.() -> AtpResponse<T>,
    ): Result<T> = networkMonitor.runCatchingWithNetworkRetry(
        times,
        initialDelay,
        maxDelay,
        factor,
        block = { block(api) },
    )

    private suspend fun authenticate(
        context: HttpRequestBuilder,
        tokens: SavedState.AuthTokens.Authenticated,
    ) = with(context) {
        when (tokens) {
            is SavedState.AuthTokens.Authenticated.Bearer -> bearerAuth(
                token = tokens.auth,
            )
            is SavedState.AuthTokens.Authenticated.DPoP -> {
                val pdsUrl = Url(tokens.pdsUrl)
                url.protocol = pdsUrl.protocol
                url.host = pdsUrl.host

                val dpopHeader = oAuthApi.createDpopHeaderValue(
                    keyPair = tokens.toKeyPair(),
                    method = method.value,
                    endpoint = url.toString(),
                    nonce = tokens.nonce,
                    accessToken = tokens.auth,
                )

                header(Authorization, "$DPoP ${tokens.auth}")
                header(DPoP, dpopHeader)
            }
        }
    }

    private suspend fun refresh(
        tokens: SavedState.AuthTokens.Authenticated,
    ): SavedState.AuthTokens.Authenticated = when (tokens) {
        is SavedState.AuthTokens.Authenticated.Bearer -> httpClient.post(
            urlString = RefreshTokenEndpoint,
            block = { bearerAuth(tokens.refresh) },
        )
            .takeIf { it.status.isSuccess() }
            ?.body<RefreshSessionResponse>()
            ?.let { refreshed ->
                SavedState.AuthTokens.Authenticated.Bearer(
                    authProfileId = ProfileId(refreshed.did.did),
                    auth = refreshed.accessJwt,
                    refresh = refreshed.refreshJwt,
                    didDoc = SavedState.AuthTokens.DidDoc.fromJsonContentOrEmpty(
                        jsonContent = refreshed.didDoc,
                    ),
                )
            }
            ?: throw InvalidTokenException()

        is SavedState.AuthTokens.Authenticated.DPoP -> {
            oAuthApi.refreshToken(
                clientId = tokens.clientId,
                nonce = tokens.nonce,
                refreshToken = tokens.refresh,
                keyPair = tokens.toKeyPair(),
            ).toAppToken()
        }
    }
}

private suspend fun SavedState.AuthTokens.Authenticated.DPoP.toKeyPair() =
    DpopKeyPair.fromKeyPair(
        publicKey = keyPair.publicKey,
        publicKeyFormat = DpopKeyPair.PublicKeyFormat.DER,
        privateKey = keyPair.privateKey,
        privateKeyFormat = DpopKeyPair.PrivateKeyFormat.DER,
    )

private suspend fun OAuthToken.toAppToken() =
    SavedState.AuthTokens.Authenticated.DPoP(
        authProfileId = subject.did.let(::ProfileId),
        auth = accessToken,
        refresh = refreshToken,
        pdsUrl = pds.toString(),
        keyPair = SavedState.AuthTokens.Authenticated.DPoP.DERKeyPair(
            publicKey = keyPair.publicKey(DpopKeyPair.PublicKeyFormat.DER),
            privateKey = keyPair.privateKey(DpopKeyPair.PrivateKeyFormat.DER),
        ),
        clientId = clientId,
        nonce = nonce,
    )

private class OauthSession(
    val handle: ProfileHandle,
    val request: OAuthAuthorizationRequest,
)

internal val BlueskyJson: Json = buildXrpcJsonConfiguration(XrpcSerializersModule)

private val HeronOauthClient = OAuthClient(
    clientId = "https://heron.tunji.dev/oauth-client.json",
    redirectUri = "https://heron.tunji.dev/oauth/callback",
)

private val HeronOauthScopes = listOf(
    OAuthScope.AtProto,
    OAuthScope.Generic,
    OAuthScope.BlueskyChat,
)

private const val BaseEndpoint = "https://bsky.social"
private const val RefreshTokenEndpoint = "/xrpc/com.atproto.server.refreshSession"
private const val OauthCallbackUriCodeParam = "code"
internal const val DPoP = "DPoP"
