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
import com.tunjid.heron.data.core.models.OauthUriRequest
import com.tunjid.heron.data.core.models.Server
import com.tunjid.heron.data.core.models.SessionRequest
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.lexicons.XrpcBlueskyApi
import com.tunjid.heron.data.lexicons.XrpcSerializersModule
import com.tunjid.heron.data.network.oauth.DpopKeyPair
import com.tunjid.heron.data.network.oauth.OAuthApi
import com.tunjid.heron.data.network.oauth.OAuthScope
import com.tunjid.heron.data.network.oauth.OauthRedirect
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.utilities.Collections
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
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.TextContent
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.http.set
import io.ktor.http.takeFrom
import io.ktor.util.AttributeKey
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Handle
import sh.christian.ozone.api.response.AtpErrorDescription
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

@Inject
internal class PersistedSessionManager(
    httpClient: HttpClient,
    private val savedStateDataSource: SavedStateDataSource,
    private val identityResolver: IdentityResolver,
    private val oauthRedirect: OauthRedirect,
    private val authSessions: AuthSessions,
    private val nonces: DPoPNonces,
) : SessionManager {

    private val sessionRequestUrl = MutableStateFlow<Url?>(null)

    private val authHttpClient = httpClient.config {
        install(DefaultRequest) {
            // Authentication requests use the most recent value from
            // create session requests, or the latest token value.
            sessionRequestUrl.value?.let(url::takeFrom)
                ?: url.takeFrom(savedStateDataSource.savedState.value.auth.defaultUrl)
        }
        installOrReplace(HttpTimeout) {
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
        val profileHandle = when (request) {
            is OauthUriRequest.SignIn -> request.handle
            is OauthUriRequest.SignUp -> null
        }
        return oAuthApi.buildAuthorizationRequest(
            oauthClient = oauthRedirect.initializeOAuthClient(),
            scopes = HeronOauthScopes,
            loginHandleHint = profileHandle?.id,
            prompt = when (request) {
                is OauthUriRequest.SignIn -> null
                is OauthUriRequest.SignUp -> OauthCreateAccountPrompt
            },
        )
            .let {
                SavedState.AuthTokens.Pending.DPoP(
                    profileHandle = profileHandle,
                    endpoint = request.server.endpoint,
                    authorizeRequestUrl = it.authorizeRequestUrl,
                    codeVerifier = it.codeVerifier,
                    nonce = it.nonce,
                    state = it.state,
                    expiresAt = Clock.System.now() + it.expiresIn,
                    keyPair = SavedState.AuthTokens.Authenticated.DPoP.DERKeyPair(
                        publicKey = it.keyPair.publicKey(DpopKeyPair.PublicKeyFormat.DER),
                        privateKey = it.keyPair.privateKey(DpopKeyPair.PrivateKeyFormat.DER),
                    ),
                )
            }
    }

    override suspend fun createSession(
        request: SessionRequest,
    ): SavedState.AuthTokens = try {
        // Update the auth server to use the endpoint from the request's server.
        sessionRequestUrl.update { Url(request.server.endpoint) }
        when (request) {
            is SessionRequest.Credentials -> {
                val result = api.createSession(
                    CreateSessionRequest(
                        identifier = request.handle.id,
                        password = request.password,
                    ),
                )
                    .requireResponse()

                SavedState.AuthTokens.Authenticated.Bearer(
                    authProfileId = ProfileId(result.did.did),
                    auth = result.accessJwt,
                    refresh = result.refreshJwt,
                    didDoc = SavedState.AuthTokens.DidDoc.fromJsonContentOrEmpty(
                        jsonContent = result.didDoc,
                    )
                        .withResolvedPds(
                            did = result.did,
                            identityResolver = identityResolver,
                        ),
                    authEndpoint = request.server.endpoint,
                    expiresAt = result.accessJwt.jwtExpiresAt(),
                )
            }
            is SessionRequest.Oauth -> {
                val existingAuth = withTimeoutOrNull(PendingTokenTimeout) {
                    savedStateDataSource.savedState
                        .map { it.auth }
                        .filterIsInstance<SavedState.AuthTokens.Pending.DPoP>()
                        .first()
                }

                val pendingRequest = existingAuth
                    ?: throw IllegalStateException("No pending oauth session to finalize.")

                require(request.server.endpoint == pendingRequest.endpoint) {
                    "Mismatched server endpoints in OAuth flow. Expected ${pendingRequest.endpoint}, but got ${request.server.endpoint}"
                }

                val callbackUrl = Url(request.callbackUri.uri)

                val state = callbackUrl.parameters["state"]
                    ?: throw IllegalStateException("No state in callback")

                require(state == pendingRequest.state) {
                    "Mismatched state in OAuth callback. Expected ${pendingRequest.state}, but got $state"
                }

                val code = callbackUrl.parameters[OauthCallbackUriCodeParam]
                    ?: throw IllegalStateException("No auth code")

                val oAuthToken = oAuthApi.requestToken(
                    oauthClient = oauthRedirect.initializedClient,
                    nonce = pendingRequest.nonce,
                    codeVerifier = pendingRequest.codeVerifier,
                    code = code,
                    // Reuse the DPoP key bound to the PAR request so the authorization code
                    // exchange presents a proof for the same key the server bound it to.
                    keyPair = pendingRequest.keyPair?.let {
                        DpopKeyPair.fromKeyPair(
                            publicKey = it.publicKey,
                            publicKeyFormat = DpopKeyPair.PublicKeyFormat.DER,
                            privateKey = it.privateKey,
                            privateKeyFormat = DpopKeyPair.PrivateKeyFormat.DER,
                        )
                    } ?: throw IllegalStateException(
                        "Missing DPoP key pair for pending authorization request. Please restart the sign-in flow.",
                    ),
                )
                nonces.update(
                    url = Url(pendingRequest.endpoint),
                    nonce = oAuthToken.nonce,
                )

                // Access tokens are opaque per the atproto OAuth spec, so the PDS is discovered
                // from the account's DID document instead of being parsed out of the token.
                val pdsUrl = identityResolver.resolvePds(oAuthToken.subject)
                    ?: throw IllegalStateException(
                        "Could not resolve a PDS for ${oAuthToken.subject.did}",
                    )

                when (val profileHandle = pendingRequest.profileHandle) {
                    // A new account has no handle to check the token against. Instead, per the
                    // atproto OAuth spec, its PDS must delegate to the server that issued the token.
                    null -> require(
                        oAuthApi.resolveAuthorizationServers(resourceServer = pdsUrl)
                            .containsIssuer(issuer = pendingRequest.endpoint),
                    ) {
                        "The PDS for ${oAuthToken.subject.did} does not delegate to ${pendingRequest.endpoint}"
                    }
                    else -> {
                        val callingDid = api.resolveHandle(
                            ResolveHandleQueryParams(Handle(profileHandle.id)),
                        )
                            .requireResponse()
                            .did

                        if (oAuthToken.subject != callingDid) {
                            throw IllegalStateException("Invalid login session")
                        }
                    }
                }

                oAuthToken.toAppToken(
                    authEndpoint = request.server.endpoint,
                    pdsUrl = pdsUrl.toString(),
                    clientId = oauthRedirect.clientId,
                )
            }
            is SessionRequest.Guest -> SavedState.AuthTokens.Guest(
                server = request.server,
            )
        }
    } finally {
        sessionRequestUrl.update { null }
    }

    override suspend fun endSession() {
        when (val authTokens = authSessions.current()) {
            is SavedState.AuthTokens.Authenticated.Bearer -> api.deleteSession()
            is SavedState.AuthTokens.Authenticated.DPoP -> {
                val issuer = Url(authTokens.issuerEndpoint)
                oAuthApi.revokeToken(
                    accessToken = authTokens.auth,
                    clientId = oauthRedirect.clientId,
                    nonce = nonces[issuer],
                    keyPair = authTokens.toKeyPair(),
                    authorizationServer = issuer,
                )
            }
            null -> Unit
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
        // Routing runs first: DPoP proofs sign the final URL.
        install(
            atProtoRouting(
                readAuth = authSessions::current,
                resolvePds = identityResolver::resolvePds,
                resolveFeedServiceDid = { feedUri ->
                    identityResolver.resolveFeedServiceDid(
                        feedUri = FeedGeneratorUri(feedUri),
                    )
                },
            ),
        )
        install(
            atProtoAuth(
                readAuth = authSessions::current,
                refreshed = authSessions::refreshed,
                authenticate = ::authenticate,
                nonces = nonces,
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

                val requestUrl = url.build()
                val dpopHeader = oAuthApi.createDpopHeaderValue(
                    keyPair = tokens.toKeyPair(),
                    method = method.value,
                    endpoint = requestUrl.toString(),
                    nonce = nonces[requestUrl],
                    accessToken = tokens.auth,
                )

                header(Authorization, "$DPoP ${tokens.auth}")
                if (headers[DPoP] == null) {
                    header(DPoP, dpopHeader)
                }
            }
        }
    }
}

/**
 * Routes requests to the server that should serve them: the signed in user's PDS, a proxied
 * service, another user's PDS, or a public app view. Requests that must not carry the signed
 * in user's credentials are marked with [SkipAuth] for [atProtoAuth].
 */
private fun atProtoRouting(
    readAuth: suspend () -> SavedState.AuthTokens.Authenticated?,
    resolvePds: suspend (Did) -> Url?,
    resolveFeedServiceDid: suspend (feedUri: String) -> Did?,
) = createClientPlugin("AtProtoRoutingPlugin") {
    on(Send) intercept@{ context ->
        // Blacksky serves topic feeds from its app view host directly and publicly. Route there and
        // skip the PDS host-rewrite and auth entirely.
        if (context.url.encodedPath.endsWith(GetTopicFeedPath)) {
            val topicFeedUrl = Url(urlString = BlackSkyAppViewUrl)
            context.url.protocol = topicFeedUrl.protocol
            context.url.host = topicFeedUrl.host
            context.url.port = topicFeedUrl.port
            context.attributes.put(SkipAuth, Unit)
            return@intercept proceed(context)
        }

        val authTokens = readAuth.invoke()

        // Computed once: the feed-interaction branch resolves a DID and must not run twice.
        val proxyHeaderValue = context.proxyHeaderValue(resolveFeedServiceDid)

        proxyHeaderValue?.let { headerValue ->
            context.headers.append(
                name = AtProtoProxyHeader,
                value = headerValue,
            )
            authTokens?.serviceUrl?.let {
                context.url.set(host = Url(urlString = it).host)
            }
        }

        if (authTokens == null && SignedOutPaths.any(predicate = context.url.encodedPath::endsWith)) {
            context.url.set(
                host = Url(urlString = SignedOutUrl).host,
            )
        } else if (authTokens != null && proxyHeaderValue != ChatAtProtoProxyHeaderValue) {
            val pdsUrl = Url(authTokens.defaultUrl)
            if (context.url.host != pdsUrl.host) {
                context.url.protocol = pdsUrl.protocol
                context.url.host = pdsUrl.host
                context.url.port = pdsUrl.port
            }
        }

        if (context.url.encodedPath.endsWith(UploadBlobPath)) context.timeout {
            requestTimeoutMillis = 6.minutes.inWholeMilliseconds
        }

        if (context.url.encodedPath.contains(ComAtProtoPathSegment)) {
            context.extractSubjectDid()?.let { subjectDid ->
                val isSignedInUser = authTokens != null && authTokens.authProfileId.id == subjectDid.did
                if (!isSignedInUser) {
                    resolvePds(subjectDid)?.let { resolvedPdsUrl ->
                        context.url.protocol = resolvedPdsUrl.protocol
                        context.url.host = resolvedPdsUrl.host
                        context.url.port = resolvedPdsUrl.port
                        // Another user's PDS serves their records publicly.
                        context.attributes.put(SkipAuth, Unit)
                    }
                }
            }
        }

        proceed(context)
    }
}

/**
 * Signs requests with the latest tokens from [readAuth], retrying when required.
 * A session may be ended based on what [refreshed] returns.
 */
private fun atProtoAuth(
    readAuth: suspend () -> SavedState.AuthTokens.Authenticated?,
    refreshed: suspend (
        used: SavedState.AuthTokens.Authenticated,
        reason: RefreshReason,
    ) -> Refresh,
    authenticate: suspend HttpRequestBuilder.(SavedState.AuthTokens.Authenticated) -> Unit,
    nonces: DPoPNonces,
) = createClientPlugin("AtProtoAuthPlugin") {
    val proceedWithNonceRetry: suspend Send.Sender.(
        tokens: SavedState.AuthTokens.Authenticated,
        context: HttpRequestBuilder,
    ) -> HttpClientCall = call@{ tokens, context ->
        val call = proceed(context).also { it.recordDPoPNonce(nonces) }
        if (call.response.status.isSuccess()) return@call call

        val savedCall = call.save()
        if (!savedCall.needsNewDPoPNonce()) return@call savedCall

        context.clearAuth()
        context.authenticate(tokens)
        proceed(context)
            .also { it.recordDPoPNonce(nonces) }
            .let { if (it.response.status.isSuccess()) it else it.save() }
    }

    on(Send) intercept@{ context ->
        val skipAuth = context.attributes.contains(SkipAuth) ||
            context.headers.contains(Authorization)
        if (skipAuth) return@intercept proceed(context)

        val tokens = readAuth.invoke() ?: return@intercept proceed(context)

        context.authenticate(tokens)
        val call = proceedWithNonceRetry(tokens, context)
        if (call.response.status.isSuccess()) return@intercept call

        val reason = when {
            call.rejectsToken() -> RefreshReason.Rejected
            call.response.status == HttpStatusCode.Unauthorized -> RefreshReason.Unauthorized
            else -> return@intercept call
        }

        when (val refresh = refreshed(tokens, reason)) {
            is Refresh.Fresh -> {
                context.clearAuth()
                context.authenticate(refresh.tokens)
                proceedWithNonceRetry(refresh.tokens, context)
            }
            // Fail this request. The session is kept unless the refresh ended it.
            Refresh.SessionEnded,
            Refresh.Declined,
            is Refresh.Unavailable,
            -> call
        }
    }
}

private suspend fun HttpClientCall.atProtoError() =
    runCatching<AtpErrorDescription?> {
        response.body<AtpErrorDescription>()
    }.getOrNull()?.error
private suspend fun HttpClientCall.rejectsToken(): Boolean =
    atProtoError() in RejectedTokenErrors ||
        RejectedTokenErrors.any { error ->
            response.headers[HttpHeaders.WWWAuthenticate]?.contains("error=\"$error\"") == true
        }

private suspend fun HttpClientCall.needsNewDPoPNonce(): Boolean =
    response.headers[HttpHeaders.WWWAuthenticate]?.contains(UseDPoPNonce) == true ||
        atProtoError() == UseDPoPNonce

private fun HttpClientCall.recordDPoPNonce(
    nonces: DPoPNonces,
) = nonces.update(
    url = request.url,
    nonce = response.headers[DPoPNonceHeaderKey],
)

private suspend fun HttpRequestBuilder.proxyHeaderValue(
    resolveFeedServiceDid: suspend (feedUri: String) -> Did?,
): String? = when {
    ChatProxyPaths.any(predicate = url.encodedPath::endsWith) -> ChatAtProtoProxyHeaderValue
    HeronProxyPaths.any(predicate = url.encodedPath::endsWith) -> HeronAtProtoProxyHeaderValue
    // Some app views (e.g. Blacksky) do not serve these discovery endpoints; their own front ends
    // proxy them to Bluesky's app view. Apply uniformly so search and feed discovery works
    // everywhere.
    BlueskyAppViewProxyPaths.any(predicate = url.encodedPath::endsWith) -> BlueskyAtProtoProxyHeaderValue
    // Interactions are proxied to the feed generator's own service, whose DID varies per feed and
    // is not the AT-URI authority. Resolve it from the request body's `feed`, then target #bsky_fg.
    url.encodedPath.endsWith(SendInteractionsPath) ->
        extractFeedUriFromBody()
            ?.let { feedUri -> resolveFeedServiceDid(feedUri) }
            ?.let { serviceDid -> "${serviceDid.did}$FeedGeneratorProxyFragment" }
    else -> null
}

private fun HttpRequestBuilder.clearAuth() {
    headers.remove(Authorization)
    headers.remove(DPoP)
}

private fun HttpRequestBuilder.extractSubjectDid(): Did? =
    extractDidFromQueryParams()?.let(::Did)
        ?: extractDidFromBody()?.let(::Did)

private fun HttpRequestBuilder.extractDidFromQueryParams(): String? =
    url.parameters[DidParam]
        ?: url.parameters[RepoParam]
            ?.takeIf { it.startsWith(DidPrefix) }

private fun HttpRequestBuilder.extractDidFromBody(): String? {
    val textContent = body as? TextContent ?: return null
    val jsonObject = runCatching {
        BlueskyJson.parseToJsonElement(textContent.text) as? JsonObject
    }.getOrNull() ?: return null

    return (jsonObject[DidParam] as? JsonPrimitive)?.content
        ?: (jsonObject[RepoParam] as? JsonPrimitive)?.content
            ?.takeIf { it.startsWith(DidPrefix) }
}

private fun HttpRequestBuilder.extractFeedUriFromBody(): String? {
    val textContent = body as? TextContent ?: return null
    val jsonObject = runCatching {
        BlueskyJson.parseToJsonElement(textContent.text) as? JsonObject
    }.getOrNull() ?: return null

    return (jsonObject[FeedParam] as? JsonPrimitive)?.content
}

private val SavedState.AuthTokens?.defaultUrl
    get() = when (this) {
        is SavedState.AuthTokens.Authenticated.Bearer -> authEndpoint
        is SavedState.AuthTokens.Authenticated.DPoP -> issuerEndpoint
        is SavedState.AuthTokens.Guest -> server.endpoint
        is SavedState.AuthTokens.Pending.DPoP -> endpoint
        null -> Server.BlueSky.endpoint
    }

/**
 * Whether [issuer] is one of these authorization servers. Issuers are bare origins, so they're
 * compared by origin to tolerate cosmetic differences like a trailing slash.
 */
internal fun List<String>.containsIssuer(
    issuer: String,
): Boolean {
    val issuerUrl = Url(issuer)
    return any { authorizationServer ->
        val authorizationServerUrl = runCatching { Url(authorizationServer) }
            .getOrNull()
            ?: return@any false

        authorizationServerUrl.protocol == issuerUrl.protocol &&
            authorizationServerUrl.host.equals(issuerUrl.host, ignoreCase = true) &&
            authorizationServerUrl.port == issuerUrl.port
    }
}

internal val BlueskyJson: Json = Json(
    from = buildXrpcJsonConfiguration(XrpcSerializersModule),
    builderAction = {
        explicitNulls = false
        ignoreUnknownKeys = true
    },
)

private val HeronOauthScopes = listOf(
    OAuthScope.AtProto,
    OAuthScope.Generic,
    OAuthScope.BlueskyChat,
)

private val ChatProxyPaths = listOf(
    "chat.bsky.convo.listConvos",
    "chat.bsky.convo.getConvoForMembers",
    "chat.bsky.convo.getMessages",
    "chat.bsky.convo.getLog",
    "chat.bsky.convo.getUnreadCounts",
    "chat.bsky.convo.sendMessage",
    "chat.bsky.convo.addReaction",
    "chat.bsky.convo.removeReaction",
    "chat.bsky.convo.acceptConvo",
    "chat.bsky.convo.leaveConvo",
    "chat.bsky.convo.muteConvo",
    "chat.bsky.convo.unmuteConvo",
    "chat.bsky.convo.updateRead",
)

private val HeronProxyPaths = listOf(
    "site.standard.heron.getDocument",
    "site.standard.heron.getDocuments",
    "site.standard.heron.getPublications",
    "site.standard.heron.getSubscribedPublications",
    "dev.tunji.heron.getModelUrl",
    "social.heron.graze.createFeed",
    "social.heron.graze.editFeed",
    "social.heron.graze.deleteFeed",
    "social.heron.graze.getFeed",
    "social.heron.notification.registerPush",
    "social.heron.gif.searchGifs",
    "social.heron.gif.getTrendingGifs",
    "app.rocksky.actor.getActorAlbums",
    "app.rocksky.actor.getActorSongs",
    "app.rocksky.actor.getActorArtists",
    "app.rocksky.actor.getActorScrobbles",
)

private val BlueskyAppViewProxyPaths = listOf(
    "app.bsky.feed.searchPostsV2",
    "app.bsky.actor.searchActors",
    "app.bsky.actor.searchActorsTypeahead",
    "app.bsky.unspecced.getPopularFeedGenerators",
)

private val SignedOutPaths = listOf(
    "app.bsky.actor.getProfile",
    "app.bsky.actor.searchActors",
    "app.bsky.actor.searchActorsTypeahead",
    "app.bsky.feed.getAuthorFeed",
    "app.bsky.feed.getFeed",
    "app.bsky.feed.getPostThread",
    "app.bsky.feed.getFeedGenerator",
    "app.bsky.feed.searchPostsV2",
    "app.bsky.unspecced.getPopularFeedGenerators",
    "app.bsky.unspecced.getPostThreadV2",
    "app.bsky.unspecced.getTrends",
)

private val PendingTokenTimeout = 2.seconds

// Marks requests that must not carry the signed in user's credentials.
private val SkipAuth = AttributeKey<Unit>("AtProtoSkipAuth")

// Errors with which a server rejects an access token as expired or revoked, or its DPoP proof.
private val RejectedTokenErrors = setOf(
    ExpiredTokenError,
    InvalidTokenError,
    InvalidTokenXrpcError,
    InvalidDPoPProofError,
)

private const val AtProtoProxyHeader = "Atproto-Proxy"
private const val AtProtoLabelerHeader = "atproto-accept-labelers"
private const val ChatAtProtoProxyHeaderValue = "did:web:api.bsky.chat#bsky_chat"
private const val BlueskyAtProtoProxyHeaderValue = "did:web:api.bsky.app#bsky_appview"

private const val UploadBlobPath = "com.atproto.repo.uploadBlob"
private const val SendInteractionsPath = "app.bsky.feed.sendInteractions"
private const val HeronAtProtoProxyHeaderValue = "did:web:heron.social#heron_appview"
private const val FeedGeneratorProxyFragment = "#bsky_fg"
private const val SignedOutUrl = "https://public.api.bsky.app"
private const val BlackSkyAppViewUrl = "https://api.blacksky.community"
private const val GetTopicFeedPath = "app.bsky.unspecced.getTopicFeed"
private const val OauthCallbackUriCodeParam = "code"
private const val OauthCreateAccountPrompt = "create"
private const val ExpiredTokenError = "ExpiredToken"
private const val InvalidTokenError = "invalid_token"
private const val InvalidTokenXrpcError = "InvalidToken"
private const val InvalidDPoPProofError = "invalid_dpop_proof"
private const val UseDPoPNonce = "use_dpop_nonce"
private const val DPoPNonceHeaderKey = "DPoP-Nonce"
private const val DPoP = "DPoP"
private const val ComAtProtoPathSegment = "com.atproto"
private const val DidParam = "did"
private const val RepoParam = "repo"
private const val FeedParam = "feed"
private const val DidPrefix = "did:"
