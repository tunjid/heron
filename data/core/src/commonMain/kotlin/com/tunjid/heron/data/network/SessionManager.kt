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
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.body
import io.ktor.client.call.save
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.http.set
import io.ktor.http.takeFrom
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import sh.christian.ozone.api.Handle
import sh.christian.ozone.api.response.AtpErrorDescription
import sh.christian.ozone.api.response.AtpException
import sh.christian.ozone.api.runtime.buildXrpcJsonConfiguration

internal interface SessionManager {

    suspend fun startOauthSessionUri(
        handle: ProfileHandle,
    ): GenericUri

    suspend fun createOauthSession(
        request: SessionRequest.Oauth,
    ): SavedState.AuthTokens.Authenticated.DPoP

    fun manage(config: HttpClientConfig<*>)
}

internal class PersistedSessionManager @Inject constructor(
    httpClient: HttpClient,
    private val savedStateDataSource: SavedStateDataSource,
) : SessionManager {

    private val oauthHttpClient = httpClient.config {
        install(DefaultRequest) {
            url.takeFrom(BaseEndpoint)
        }

        install(Logging) {
            level = LogLevel.BODY
            logger = object : Logger {
                override fun log(message: String) {
//                    println("Logger SessionManager => $message")
                }
            }
        }
    }

    private val api = XrpcBlueskyApi(
        httpClient = oauthHttpClient,
    )

    private val oAuthApi: OAuthApi = OAuthApi(
        client = oauthHttpClient,
    )

    private var pendingOauthSession: OauthSession? = null

    override suspend fun startOauthSessionUri(
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

    override suspend fun createOauthSession(
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

    override fun manage(
        config: HttpClientConfig<*>,
    ) = with(config) {
        install(
            atProtoAuth(
                networkErrorConverter = BlueskyJson::decodeFromString,
                readAuth = savedStateDataSource.signedInAuth::first,
                saveAuth = savedStateDataSource::setAuth,
                authenticate = ::authenticate,
                refresh = ::refresh,
            ),
        )
    }

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
                if (headers[DPoP] == null) {
                    header(DPoP, dpopHeader)
                }
            }
        }
    }

    private suspend fun refresh(
        tokens: SavedState.AuthTokens.Authenticated,
    ): SavedState.AuthTokens.Authenticated = when (tokens) {
        is SavedState.AuthTokens.Authenticated.Bearer -> oauthHttpClient.post(
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

/**
 * Invalidates session cookies that have expired or are otherwise invalid
 */
private fun atProtoAuth(
    networkErrorConverter: (String) -> AtpErrorDescription,
    readAuth: suspend () -> SavedState.AuthTokens.Authenticated?,
    saveAuth: suspend (SavedState.AuthTokens.Authenticated?) -> Unit,
    authenticate: suspend HttpRequestBuilder.(SavedState.AuthTokens.Authenticated) -> Unit,
    refresh: suspend (SavedState.AuthTokens.Authenticated) -> SavedState.AuthTokens.Authenticated,
) = createClientPlugin("AtProtoAuthPlugin") {
    on(Send) intercept@{ context ->
        val authTokens = readAuth.invoke()

        if (ChatProxyPaths.any(predicate = context.url.encodedPath::endsWith)) {
            context.headers.append(
                name = AtProtoProxyHeader,
                value = ChatAtProtoProxyHeaderValue,
            )
            authTokens?.serviceUrl?.let {
                context.url.set(host = Url(urlString = it).host)
            }
        }

        if (authTokens == null && SignedOutPaths.any(predicate = context.url.encodedPath::endsWith)) {
            context.url.set(
                host = Url(urlString = SignedOutUrl).host,
            )
        }

        if (!context.headers.contains(Authorization) && authTokens != null) {
            context.authenticate(authTokens)
        }

        var result: HttpClientCall = proceed(context)
        if (result.response.status.isSuccess()) {
            return@intercept result
        }

        // Cache the response in memory since we will need to decode it potentially more than once.
        result = result.save()

        val response = runCatching<AtpErrorDescription?> {
            networkErrorConverter(result.response.bodyAsText())
        }

        val updatedTokensResult = runCatchingUnlessCancelled {
            when (response.getOrNull()?.error) {
                InvalidTokenError,
                ExpiredTokenError,
                -> readAuth()?.let { existingToken ->
                    refresh(existingToken)
                }
                // If this returns null, do not throw an exception.
                // This header value is sometimes returned when a new token is issued,
                // and concurrent requests may see it. The value eventually becomes consistent.
                UseDPoPNonce -> maybeUpdateDPoPNonce(
                    response = result.response,
                    readAuth = readAuth,
                )
                else -> when (result.response.status) {
                    HttpStatusCode.Unauthorized ->
                        if (authTokens != null) throw InvalidTokenException()
                        else null
                    else -> null
                }
            }
        }

        updatedTokensResult.fold(
            onSuccess = { updatedTokens ->
                if (updatedTokens != null) {
                    saveAuth(updatedTokens)
                    context.clearAuth()
                    context.authenticate(updatedTokens)
                    result = proceed(context)
                }
            },
            onFailure = {
                // Delete existing token and force a log out
                when (it) {
                    is InvalidTokenException,
                    is AtpException,
                    -> saveAuth(null)
                    else -> throw it
                }
            },
        )

        maybeUpdateDPoPNonce(
            response = result.response,
            readAuth = readAuth,
        )?.also { saveAuth(it) }

        result
    }
}

private suspend fun maybeUpdateDPoPNonce(
    response: HttpResponse,
    readAuth: suspend () -> SavedState.AuthTokens.Authenticated?,
): SavedState.AuthTokens.Authenticated.DPoP? {
    val currentDPoPNonce = response.headers[DPoPNonceHeaderKey] ?: return null
    val existingToken = readAuth() ?: return null
    if (existingToken !is SavedState.AuthTokens.Authenticated.DPoP) return null

    return existingToken.copy(nonce = currentDPoPNonce)
}

private fun HttpRequestBuilder.clearAuth() {
    headers.remove(Authorization)
    headers.remove(DPoP)
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

internal val BlueskyJson: Json = Json(
    from = buildXrpcJsonConfiguration(XrpcSerializersModule),
    builderAction = {
        explicitNulls = false
    },
)

private val HeronOauthClient = OAuthClient(
    clientId = "https://heron.tunji.dev/oauth-client.json",
    redirectUri = "https://heron.tunji.dev/oauth/callback",
)

private val HeronOauthScopes = listOf(
    OAuthScope.AtProto,
    OAuthScope.Generic,
    OAuthScope.BlueskyChat,
)

private val ChatProxyPaths = listOf(
    "chat.bsky.convo.listConvos",
    "chat.bsky.convo.getMessages",
    "chat.bsky.convo.getLog",
    "chat.bsky.convo.sendMessage",
    "chat.bsky.convo.addReaction",
    "chat.bsky.convo.removeReaction",
)

private val SignedOutPaths = listOf(
    "app.bsky.actor.getProfile",
    "app.bsky.actor.searchActors",
    "app.bsky.actor.searchActorsTypeahead",
    "app.bsky.feed.getAuthorFeed",
    "app.bsky.feed.getFeed",
    "app.bsky.feed.getPostThread",
    "app.bsky.feed.getFeedGenerator",
    "app.bsky.feed.searchPosts",
    "app.bsky.unspecced.getPopularFeedGenerators",
    "app.bsky.unspecced.getTrends",
)

private const val AtProtoProxyHeader = "Atproto-Proxy"
private const val ChatAtProtoProxyHeaderValue = "did:web:api.bsky.chat#bsky_chat"
private const val SignedOutUrl = "https://public.api.bsky.app"
private const val RefreshTokenEndpoint = "/xrpc/com.atproto.server.refreshSession"
private const val OauthCallbackUriCodeParam = "code"
private const val ExpiredTokenError = "ExpiredToken"
private const val InvalidTokenError = "invalid_token"
private const val UseDPoPNonce = "use_dpop_nonce"
private const val DPoPNonceHeaderKey = "DPoP-Nonce"
private const val DPoP = "DPoP"
