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
import com.atproto.server.CreateSessionRequest
import com.atproto.server.RefreshSessionResponse
import com.tunjid.heron.data.core.models.OauthUriRequest
import com.tunjid.heron.data.core.models.Server
import com.tunjid.heron.data.core.models.SessionRequest
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.lexicons.XrpcBlueskyApi
import com.tunjid.heron.data.lexicons.XrpcSerializersModule
import com.tunjid.heron.data.network.oauth.DpopKeyPair
import com.tunjid.heron.data.network.oauth.OAuthApi
import com.tunjid.heron.data.network.oauth.OAuthClient
import com.tunjid.heron.data.network.oauth.OAuthScope
import com.tunjid.heron.data.network.oauth.OAuthToken
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.signedInAuth
import com.tunjid.heron.data.utilities.AtProtoException
import com.tunjid.heron.data.utilities.Collections
import com.tunjid.heron.data.utilities.DeferredMutex
import com.tunjid.heron.data.utilities.InvalidTokenException
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.body
import io.ktor.client.call.save
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.http.set
import io.ktor.http.takeFrom
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import sh.christian.ozone.api.Handle
import sh.christian.ozone.api.response.AtpErrorDescription
import sh.christian.ozone.api.response.AtpException
import sh.christian.ozone.api.runtime.buildXrpcJsonConfiguration

internal interface SessionManager {

    suspend fun initiateOauthSession(
        request: OauthUriRequest,
    ): SavedState.AuthTokens.Pending

    suspend fun createSession(
        request: SessionRequest,
    ): SavedState.AuthTokens

    suspend fun endSession()

    fun manage(config: HttpClientConfig<*>)
}

internal class PersistedSessionManager @Inject constructor(
    httpClient: HttpClient,
    private val savedStateDataSource: SavedStateDataSource,
) : SessionManager {

    private val sessionRequestUrl = MutableStateFlow<Url?>(null)

    private val authHttpClient = httpClient.config {
        install(DefaultRequest) {
            // Authentication requests use the most recent value from
            // create session requests, or the latest token value.
            sessionRequestUrl.value?.let(url::takeFrom)
                ?: url.takeFrom(savedStateDataSource.savedState.value.auth.defaultUrl)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15.seconds.inWholeMilliseconds
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
        httpClient = authHttpClient,
    )

    private val oAuthApi: OAuthApi = OAuthApi(
        client = authHttpClient,
    )

    override suspend fun initiateOauthSession(
        request: OauthUriRequest,
    ): SavedState.AuthTokens.Pending {
        sessionRequestUrl.update { Url(request.server.endpoint) }
        return oAuthApi.buildAuthorizationRequest(
            oauthClient = HeronOauthClient,
            scopes = HeronOauthScopes,
            loginHandleHint = request.handle.id,
        )
            .let {
                SavedState.AuthTokens.Pending.DPoP(
                    profileHandle = request.handle,
                    endpoint = request.server.endpoint,
                    authorizeRequestUrl = it.authorizeRequestUrl,
                    codeVerifier = it.codeVerifier,
                    nonce = it.nonce,
                )
            }
    }

    override suspend fun createSession(
        request: SessionRequest,
    ): SavedState.AuthTokens = try {
        // Update the auth server to use the endpoint from the request's server.
        sessionRequestUrl.update { Url(request.server.endpoint) }
        when (request) {
            is SessionRequest.Credentials -> api.createSession(
                CreateSessionRequest(
                    identifier = request.handle.id,
                    password = request.password,
                ),
            )
                .map { result ->
                    SavedState.AuthTokens.Authenticated.Bearer(
                        authProfileId = ProfileId(result.did.did),
                        auth = result.accessJwt,
                        refresh = result.refreshJwt,
                        didDoc = SavedState.AuthTokens.DidDoc.fromJsonContentOrEmpty(
                            jsonContent = result.didDoc,
                        ),
                        authEndpoint = request.server.endpoint,
                    )
                }
                .requireResponse()
            is SessionRequest.Oauth -> {
                val existingAuth = savedStateDataSource.savedState.value.auth
                val pendingRequest = existingAuth as? SavedState.AuthTokens.Pending.DPoP
                    ?: throw IllegalStateException("No pending oauth session to finalize. Current auth state: $existingAuth")

                require(request.server.endpoint == pendingRequest.endpoint) {
                    "Mismatched server endpoints in OAuth flow. Expected ${pendingRequest.endpoint}, but got ${request.server.endpoint}"
                }

                val callbackUrl = Url(request.callbackUri.uri)

                val code = callbackUrl.parameters[OauthCallbackUriCodeParam]
                    ?: throw IllegalStateException("No auth code")

                val oAuthToken = oAuthApi.requestToken(
                    oauthClient = HeronOauthClient,
                    nonce = pendingRequest.nonce,
                    codeVerifier = pendingRequest.codeVerifier,
                    code = code,
                )

                val callingDid = api.resolveHandle(
                    ResolveHandleQueryParams(Handle(pendingRequest.profileHandle.id)),
                )
                    .requireResponse()
                    .did

                if (oAuthToken.subject != callingDid) {
                    throw IllegalStateException("Invalid login session")
                }

                oAuthToken.toAppToken(authEndpoint = request.server.endpoint)
            }
            is SessionRequest.Guest -> SavedState.AuthTokens.Guest(
                server = request.server,
            )
        }
    } finally {
        sessionRequestUrl.update { null }
    }

    override suspend fun endSession() {
        when (val authTokens = savedStateDataSource.savedState.value.auth) {
            is SavedState.AuthTokens.Authenticated.Bearer -> api.deleteSession()
            is SavedState.AuthTokens.Authenticated.DPoP -> oAuthApi.revokeToken(
                accessToken = authTokens.auth,
                clientId = HeronOauthClient.clientId,
                nonce = authTokens.nonce,
                keyPair = authTokens.toKeyPair(),
            )
            is SavedState.AuthTokens.Guest,
            is SavedState.AuthTokens.Pending,
            null,
            -> Unit
        }
    }

    override fun manage(
        config: HttpClientConfig<*>,
    ) = with(config) {
        install(DefaultRequest) {
            // The managed session must take its default URL from the current auth token
            val currentSavedState = savedStateDataSource.savedState.value
            url.takeFrom(currentSavedState.auth.defaultUrl)
            currentSavedState.signedInProfileData
                ?.preferences
                ?.labelerPreferences
                ?.map { it.labelerCreatorId.id }
                ?.plus(Collections.DefaultLabelerProfileId.id)
                ?.let { labelProfileIds ->
                    headers.appendAll(AtProtoLabelerHeader, labelProfileIds)
                }
        }
        install(
            atProtoAuth(
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
//                    println("Logger Ktor => $message")
                }
            }
        }
    }

    private suspend fun authenticate(
        context: HttpRequestBuilder,
        tokens: SavedState.AuthTokens.Authenticated,
    ) = with(context) {
        when (tokens) {
            is SavedState.AuthTokens.Authenticated.Bearer -> {
                bearerAuth(token = tokens.auth)
            }
            is SavedState.AuthTokens.Authenticated.DPoP -> {
                // DPoP requests must always be made to the user's PDS
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
        is SavedState.AuthTokens.Authenticated.Bearer -> authHttpClient.post(
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
                    authEndpoint = tokens.authEndpoint,
                )
            }
            ?: throw InvalidTokenException()

        is SavedState.AuthTokens.Authenticated.DPoP -> {
            oAuthApi.refreshToken(
                clientId = tokens.clientId,
                nonce = tokens.nonce,
                refreshToken = tokens.refresh,
                keyPair = tokens.toKeyPair(),
            ).toAppToken(authEndpoint = tokens.issuerEndpoint)
        }
    }
}

/**
 * Invalidates session cookies that have expired or are otherwise invalid
 */
private fun atProtoAuth(
    readAuth: suspend () -> SavedState.AuthTokens.Authenticated?,
    saveAuth: suspend (SavedState.AuthTokens.Authenticated?) -> Unit,
    authenticate: suspend HttpRequestBuilder.(SavedState.AuthTokens.Authenticated) -> Unit,
    refresh: suspend (SavedState.AuthTokens.Authenticated) -> SavedState.AuthTokens.Authenticated,
) = createClientPlugin("AtProtoAuthPlugin") {
    val tokenRefreshDeferredMutex = DeferredMutex<String, SavedState.AuthTokens.Authenticated>()
    val nonceDeferredMutex = DeferredMutex<String, SavedState.AuthTokens.Authenticated.DPoP?>()

    val maybeUpdateAndSaveDPoPNonce: suspend SavedState.AuthTokens.Authenticated?.(String?) -> Unit =
        update@{ newNonce ->
            this ?: return@update
            newNonce ?: return@update
            nonceDeferredMutex.withSingleAccess(newNonce) {
                maybeUpdateDPoPNonce(newNonce)
            }?.also { saveAuth(it) }
        }

    val proceedAndRetryIfDPoPNonceError: suspend Send.Sender.(
        existingTokens: SavedState.AuthTokens.Authenticated?,
        context: HttpRequestBuilder,
    ) -> HttpClientCall = call@{ tokens, context ->
        var call = proceed(context)
        if (call.response.status.isSuccess()) return@call call

        call = call.save()
        val error = call.atProtoError()

        if (error == UseDPoPNonce) {
            val updatedTokens = call.newDPoPNonce
                ?.let(tokens::maybeUpdateDPoPNonce)
                ?: return@call call

            context.clearAuth()
            context.authenticate(updatedTokens)
            return@call proceed(context).save()
        }
        return@call call
    }

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

        var result: HttpClientCall = proceedAndRetryIfDPoPNonceError(
            authTokens,
            context,
        )
        if (result.response.status.isSuccess()) {
            authTokens.maybeUpdateAndSaveDPoPNonce(result.newDPoPNonce)
            return@intercept result
        }

        val updatedTokensResult = runCatchingUnlessCancelled {
            when (result.atProtoError()) {
                InvalidTokenError,
                ExpiredTokenError,
                -> authTokens?.let { existingToken ->
                    tokenRefreshDeferredMutex.withSingleAccess(
                        key = existingToken.singleAccessKey,
                        block = { refresh(existingToken) },
                    )
                }
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
                    context.clearAuth()
                    context.authenticate(updatedTokens)
                    result = proceedAndRetryIfDPoPNonceError(updatedTokens, context).also { call ->
                        when {
                            call.response.status.isSuccess() -> {
                                saveAuth(updatedTokens)
                                updatedTokens.maybeUpdateAndSaveDPoPNonce(call.newDPoPNonce)
                            }
                            else -> {
                                val error = call.atProtoError()
                                if (call.response.status == HttpStatusCode.Unauthorized ||
                                    error == InvalidTokenError ||
                                    error == ExpiredTokenError
                                ) {
                                    saveAuth(null)
                                }
                            }
                        }
                    }
                }
            },
            onFailure = {
                // Delete existing token and force a log out
                when (it) {
                    is InvalidTokenException,
                    is AtpException,
                    is AtProtoException,
                    -> saveAuth(null)
                    else -> throw it
                }
            },
        )

        result
    }
}

private suspend fun HttpClientCall.atProtoError() =
    runCatching<AtpErrorDescription?> {
        response.body<AtpErrorDescription>()
    }.getOrNull()?.error

private fun SavedState.AuthTokens.Authenticated?.maybeUpdateDPoPNonce(
    newNonce: String,
): SavedState.AuthTokens.Authenticated.DPoP? {
    return if (this is SavedState.AuthTokens.Authenticated.DPoP) copy(nonce = newNonce)
    else null
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

private suspend fun OAuthToken.toAppToken(
    authEndpoint: String,
) = SavedState.AuthTokens.Authenticated.DPoP(
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
    issuerEndpoint = authEndpoint,
)

private val SavedState.AuthTokens?.defaultUrl
    get() = when (this) {
        is SavedState.AuthTokens.Authenticated.Bearer -> authEndpoint
        is SavedState.AuthTokens.Authenticated.DPoP -> issuerEndpoint
        is SavedState.AuthTokens.Guest -> server.endpoint
        is SavedState.AuthTokens.Pending.DPoP -> endpoint
        null -> Server.BlueSky.endpoint
    }

private val SavedState.AuthTokens.Authenticated.singleAccessKey
    get() = when (this) {
        is SavedState.AuthTokens.Authenticated.Bearer -> "$auth-$refresh"
        is SavedState.AuthTokens.Authenticated.DPoP -> "$auth-$refresh"
    }

internal val BlueskyJson: Json = Json(
    from = buildXrpcJsonConfiguration(XrpcSerializersModule),
    builderAction = {
        explicitNulls = false
        ignoreUnknownKeys = true
    },
)

private val HttpClientCall.newDPoPNonce
    get() = response.headers[DPoPNonceHeaderKey]

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
private const val AtProtoLabelerHeader = "atproto-accept-labelers"
private const val ChatAtProtoProxyHeaderValue = "did:web:api.bsky.chat#bsky_chat"
private const val SignedOutUrl = "https://public.api.bsky.app"
private const val RefreshTokenEndpoint = "/xrpc/com.atproto.server.refreshSession"
private const val OauthCallbackUriCodeParam = "code"
private const val ExpiredTokenError = "ExpiredToken"
private const val InvalidTokenError = "invalid_token"
private const val UseDPoPNonce = "use_dpop_nonce"
private const val DPoPNonceHeaderKey = "DPoP-Nonce"
private const val DPoP = "DPoP"
