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

import com.tunjid.heron.data.core.models.Server
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.files.createFileManager
import com.tunjid.heron.data.network.oauth.DpopKeyPair
import com.tunjid.heron.data.network.oauth.OauthRedirect
import com.tunjid.heron.data.network.oauth.crypto.platformCryptographyProvider
import com.tunjid.heron.data.repository.DataStoreSavedStateDataSource
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.SavedStateEncryption
import com.tunjid.heron.data.repository.signedInAuth
import dev.whyoleg.cryptography.CryptographyProviderApi
import dev.whyoleg.cryptography.CryptographySystem
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.http.isSuccess
import io.ktor.http.parseUrlEncodedParameters
import io.ktor.serialization.kotlinx.json.json
import kotlin.io.encoding.Base64
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.protobuf.ProtoBuf
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Handle

/**
 * Exercises [AuthSessions] and the auth plugin against a server that, like a real atproto
 * OAuth server, rotates refresh tokens on every use and rejects any it already rotated. If a
 * spent refresh token is ever persisted, the next refresh after a restart ends the session.
 */
class SessionManagerTokenRaceTest {

    private lateinit var directory: Path
    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setUp() {
        registerCryptographyProvider
        directory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "heron-token-race-${Random.nextLong()}"
        FileSystem.SYSTEM.createDirectories(directory)
        scope = CoroutineScope(Dispatchers.Default)
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        FileSystem.SYSTEM.deleteRecursively(directory)
    }

    @Test
    fun responseCarryingAnUnchangedNonce_afterARefresh_keepsTheRefreshedTokens() = runBlocking {
        val server = FakeOAuthServer()
        val dataSource = savedStateDataSource()
        val app = app(
            server = server,
            dataSource = dataSource,
        )
        app.sessions.setActive(server.signIn())

        // A request with the first tokens sees the server's current nonce.
        assertTrue(app.client.get(XrpcUrl).status.isSuccess())

        // The access token expires. The next request refreshes, then succeeds with the same
        // nonce, since servers keep a nonce for minutes.
        server.expireAccessToken()
        assertTrue(app.client.get(XrpcUrl).status.isSuccess())

        assertStoredTokensAreCurrent(
            server = server,
            dataSource = dataSource,
        )
    }

    @Test
    fun responseToARequestSentBeforeARefresh_keepsTheRefreshedTokens() = runBlocking {
        val server = FakeOAuthServer()
        val dataSource = savedStateDataSource()
        val app = app(
            server = server,
            dataSource = dataSource,
        )
        app.sessions.setActive(server.signIn())
        // Learn the nonce first, so the held request below is the one that goes through.
        assertTrue(app.client.get(XrpcUrl).status.isSuccess())

        // This request goes out with the first tokens, but its response is held back.
        val slowResponse = CompletableDeferred<Unit>()
        server.holdNextResourceResponse(slowResponse)
        val slowCall = async { app.client.get(XrpcUrl) }
        server.awaitHeldResourceRequest()

        // Meanwhile the access token expires, and another request refreshes it.
        server.expireAccessToken()
        assertTrue(app.client.get(XrpcUrl).status.isSuccess())

        // The held response now arrives, carrying a new nonce.
        server.rotatePdsNonce()
        slowResponse.complete(Unit)
        assertTrue(slowCall.await().status.isSuccess())

        assertStoredTokensAreCurrent(
            server = server,
            dataSource = dataSource,
        )
    }

    @Test
    fun concurrentExpiredRequests_acrossClients_refreshOnce() = runBlocking {
        val server = FakeOAuthServer()
        val dataSource = savedStateDataSource()
        val app = app(
            server = server,
            dataSource = dataSource,
        )
        app.sessions.setActive(server.signIn())
        // KtorNetworkService configures two clients with the same session manager.
        val clients = listOf(
            app.client,
            app.newClient(),
        )

        server.expireAccessToken()
        List(6) { index ->
            async { clients[index % clients.size].get(XrpcUrl) }
        }
            .awaitAll()
            .forEach { assertTrue(it.status.isSuccess()) }

        assertEquals(
            expected = 1,
            actual = server.refreshCount,
        )
        assertStoredTokensAreCurrent(
            server = server,
            dataSource = dataSource,
        )
    }

    @Test
    fun refreshThatOutlivesItsCancelledRequest_isPersisted() = runBlocking {
        val server = FakeOAuthServer()
        val dataSource = savedStateDataSource()
        val app = app(
            server = server,
            dataSource = dataSource,
        )
        app.sessions.setActive(server.signIn())

        // The server rotates the refresh token, but its response is held back while the
        // request that triggered the refresh is cancelled, like a screen that stopped collecting.
        server.expireAccessToken()
        val tokenResponse = CompletableDeferred<Unit>()
        server.holdNextTokenResponse(tokenResponse)
        val call = launch { app.client.get(XrpcUrl) }
        server.awaitHeldTokenRequest()
        call.cancel()
        tokenResponse.complete(Unit)
        call.join()

        val persisted = withTimeoutOrNull(5.seconds) {
            dataSource.savedState.first { savedState ->
                (savedState.auth as? SavedState.AuthTokens.Authenticated.DPoP)
                    ?.refresh == server.currentRefreshToken()
            }
        }
        assertNotNull(persisted)
        assertStoredTokensAreCurrent(
            server = server,
            dataSource = dataSource,
        )
    }

    @Test
    fun tokensAboutToExpire_areRefreshedBeforeTheRequestIsSent() = runBlocking {
        val server = FakeOAuthServer()
        val dataSource = savedStateDataSource()
        val app = app(
            server = server,
            dataSource = dataSource,
        )
        app.sessions.setActive(server.signIn(expiresIn = 10.seconds))

        assertTrue(app.client.get(XrpcUrl).status.isSuccess())

        assertEquals(
            expected = 1,
            actual = server.refreshCount,
        )
        assertEquals(
            expected = 0,
            actual = server.rejectedAccessTokenCount,
        )
    }

    @Test
    fun eachServersNonce_isTrackedSeparately() = runBlocking {
        val server = FakeOAuthServer()
        val dataSource = savedStateDataSource()
        val app = app(
            server = server,
            dataSource = dataSource,
        )
        app.sessions.setActive(server.signIn())

        // The PDS asks for its nonce only in the WWW-Authenticate header.
        assertTrue(app.client.get(XrpcUrl).status.isSuccess())
        server.expireAccessToken()
        assertTrue(app.client.get(XrpcUrl).status.isSuccess())

        // PDS responses in between don't overwrite the authorization server's nonce, so the
        // second refresh needs no nonce challenge.
        server.rotatePdsNonce()
        assertTrue(app.client.get(XrpcUrl).status.isSuccess())
        server.expireAccessToken()
        assertTrue(app.client.get(XrpcUrl).status.isSuccess())

        assertEquals(
            expected = 2,
            actual = server.refreshCount,
        )
        assertEquals(
            expected = 1,
            actual = server.authNonceChallengeCount,
        )
        assertEquals(
            expected = 2,
            actual = server.pdsNonceChallengeCount,
        )
    }

    @Test
    fun unexplainedUnauthorizedResponse_keepsTheSession() = runBlocking<Unit> {
        val server = FakeOAuthServer()
        val dataSource = savedStateDataSource()
        val app = app(
            server = server,
            dataSource = dataSource,
        )
        app.sessions.setActive(server.signIn())

        // Like a feed generator rejecting a request, unrelated to the user's tokens.
        assertEquals(
            expected = HttpStatusCode.Unauthorized,
            actual = app.client.get(UnauthorizedFeedUrl).status,
        )

        assertEquals(
            expected = 0,
            actual = server.refreshCount,
        )
        assertNotNull(dataSource.signedInAuth.first())
    }

    @Test
    fun rejectedRefresh_endsTheSessionOnce() = runBlocking {
        val server = FakeOAuthServer()
        val dataSource = savedStateDataSource()
        val app = app(
            server = server,
            dataSource = dataSource,
        )
        app.sessions.setActive(server.signIn())

        server.revokeRefreshToken()
        server.expireAccessToken()
        List(4) { async { app.client.get(XrpcUrl) } }
            .awaitAll()
            .forEach { assertFalse(it.status.isSuccess()) }

        assertEquals(
            expected = 1,
            actual = server.rejectedRefreshCount,
        )
        assertNull(dataSource.signedInAuth.first())
    }

    @Test
    fun refreshFailuresWithoutAVerdict_keepTheSession() = runBlocking {
        val failures = listOf<TokenFailure>(
            { throw IOException("Offline") },
            {
                respond(
                    content = "",
                    status = HttpStatusCode.ServiceUnavailable,
                )
            },
            {
                respond(
                    content = "",
                    status = HttpStatusCode.TooManyRequests,
                )
            },
            {
                respond(
                    content = """{"error":"invalid_client","message":"Client metadata unavailable"}""",
                    status = HttpStatusCode.Forbidden,
                    headers = JsonHeaders,
                )
            },
            {
                respond(
                    content = """{"error":"invalid_dpop_proof","message":"DPoP proof iat is in the future"}""",
                    status = HttpStatusCode.BadRequest,
                    headers = JsonHeaders,
                )
            },
            // Like a captive portal answering instead of the server.
            {
                respond(
                    content = "<html>Not a token</html>",
                    status = HttpStatusCode.OK,
                    headers = JsonHeaders,
                )
            },
        )
        failures.forEachIndexed { index, failure ->
            val server = FakeOAuthServer()
            val dataSource = savedStateDataSource(name = "saved-state-$index")
            val app = app(
                server = server,
                dataSource = dataSource,
            )
            app.sessions.setActive(server.signIn())

            server.expireAccessToken()
            server.failTokenRequests(failure)
            assertFalse(app.client.get(XrpcUrl).status.isSuccess())
            assertNotNull(dataSource.signedInAuth.first())

            // The session recovers once the server can be reached.
            server.failTokenRequests(null)
            assertTrue(app.client.get(XrpcUrl).status.isSuccess())
            assertStoredTokensAreCurrent(
                server = server,
                dataSource = dataSource,
            )
        }
    }

    @Test
    fun refreshTokenVerdicts_endTheSession() = runBlocking {
        val failures = listOf<TokenFailure>(
            {
                respond(
                    content = """{"error":"invalid_grant","error_description":"Refresh token expired"}""",
                    status = HttpStatusCode.BadRequest,
                    headers = JsonHeaders,
                )
            },
            {
                respond(
                    content = """{"error":"ExpiredToken","message":"Token has been revoked"}""",
                    status = HttpStatusCode.BadRequest,
                    headers = JsonHeaders,
                )
            },
            {
                respond(
                    content = """{"error":"InvalidToken","message":"Token could not be verified"}""",
                    status = HttpStatusCode.Unauthorized,
                    headers = JsonHeaders,
                )
            },
        )
        failures.forEachIndexed { index, failure ->
            val server = FakeOAuthServer()
            val dataSource = savedStateDataSource(name = "saved-state-$index")
            val app = app(
                server = server,
                dataSource = dataSource,
            )
            app.sessions.setActive(server.signIn())

            server.expireAccessToken()
            server.failTokenRequests(failure)
            assertFalse(app.client.get(XrpcUrl).status.isSuccess())
            assertNull(dataSource.signedInAuth.first())
        }
    }

    @Test
    fun tokensRejectedRightAfterRefreshing_endTheSession() = runBlocking {
        val server = FakeOAuthServer()
        val dataSource = savedStateDataSource()
        val app = app(
            server = server,
            dataSource = dataSource,
        )
        app.sessions.setActive(server.signIn())

        // Like a device clock the PDS won't accept: refreshing succeeds, but doesn't help.
        server.rejectDPoPProofs()
        repeat(3) {
            assertFalse(app.client.get(XrpcUrl).status.isSuccess())
        }

        assertEquals(
            expected = 2,
            actual = server.refreshCount,
        )
        assertNull(dataSource.signedInAuth.first())
    }

    @Test
    fun tokensRejectedAfterRefreshingHelped_keepTheSession() = runBlocking<Unit> {
        val server = FakeOAuthServer()
        val dataSource = savedStateDataSource()
        val app = app(
            server = server,
            dataSource = dataSource,
        )
        app.sessions.setActive(server.signIn())

        // Each rejection comes soon after the last refresh, but each refresh fixed it.
        repeat(4) {
            server.expireAccessToken()
            assertTrue(app.client.get(XrpcUrl).status.isSuccess())
        }

        assertEquals(
            expected = 4,
            actual = server.refreshCount,
        )
        assertNotNull(dataSource.signedInAuth.first())
    }

    @Test
    fun tokensRejectedByAProxiedService_keepTheSession() = runBlocking {
        val server = FakeOAuthServer()
        val dataSource = savedStateDataSource()
        val app = app(
            server = server,
            dataSource = dataSource,
        )
        app.sessions.setActive(server.signIn())

        // Like a proxied app view rejecting the service auth token the PDS minted for it.
        repeat(4) {
            assertFalse(app.client.get(ProxiedRejectionUrl).status.isSuccess())
        }

        assertEquals(
            expected = 0,
            actual = server.refreshCount,
        )
        assertNotNull(dataSource.signedInAuth.first())
        assertTrue(app.client.get(XrpcUrl).status.isSuccess())
    }

    @Test
    fun failedRefreshesAheadOfExpiry_backOff() = runBlocking<Unit> {
        val server = FakeOAuthServer()
        val dataSource = savedStateDataSource()
        val app = app(
            server = server,
            dataSource = dataSource,
        )
        app.sessions.setActive(server.signIn(expiresIn = 30.seconds))

        server.failTokenRequests {
            respond(
                content = "",
                status = HttpStatusCode.ServiceUnavailable,
            )
        }
        // The access token still works, so requests succeed without hitting the token
        // endpoint again until the backoff passes.
        repeat(3) {
            assertTrue(app.client.get(XrpcUrl).status.isSuccess())
        }

        assertEquals(
            expected = 1,
            actual = server.failedTokenRequestCount,
        )
        assertNotNull(dataSource.signedInAuth.first())
    }

    @Test
    fun switchingToAPastSession_keepsTokensItRefreshed() = runBlocking {
        val server = FakeOAuthServer()
        val dataSource = savedStateDataSource()
        val app = app(
            server = server,
            dataSource = dataSource,
        )
        app.sessions.setActive(server.signIn(did = PastUserDid))
        app.sessions.setActive(server.signIn())

        // A request made in the past session refreshes its tokens.
        server.expireAccessToken(did = PastUserDid)
        val pastSession = SessionContext.Previous(
            profileId = ProfileId(PastUserDid),
            profileData = assertNotNull(
                dataSource.savedState.value.profileData(ProfileId(PastUserDid)),
            ),
        )
        withContext(pastSession) {
            assertTrue(app.client.get(XrpcUrl).status.isSuccess())
        }

        assertTrue(app.sessions.switchTo(ProfileId(PastUserDid)))
        assertStoredTokensAreCurrent(
            server = server,
            dataSource = dataSource,
            did = PastUserDid,
        )
    }

    /**
     * Checks the persisted tokens hold the refresh token the server will accept next, then
     * simulates a process restart to show the session survives it.
     */
    private suspend fun assertStoredTokensAreCurrent(
        server: FakeOAuthServer,
        dataSource: DataStoreSavedStateDataSource,
        did: String = UserDid,
    ) {
        val stored = dataSource.signedInAuth.first()
        assertNotNull(stored)
        assertEquals(
            expected = server.currentRefreshToken(did),
            actual = (stored as SavedState.AuthTokens.Authenticated.DPoP).refresh,
        )

        server.expireAccessToken(did)
        val restarted = app(
            server = server,
            dataSource = dataSource,
        )
        assertTrue(restarted.client.get(XrpcUrl).status.isSuccess())
        assertNotNull(dataSource.signedInAuth.first())
    }

    // Only one DataStore may be active per file, so each one in a test needs its own [name].
    private fun savedStateDataSource(
        name: String = "saved-state",
    ) = DataStoreSavedStateDataSource(
        path = File.System((directory / "$name.pb").toString()),
        fileManager = createFileManager(fileSystem = FileSystem.SYSTEM),
        protoBuf = ProtoBuf { },
        encryption = SavedStateEncryption.None,
        appMainScope = scope,
        ioDispatcher = Dispatchers.Default,
    )

    /**
     * Wires the app's auth graph the way DataBindings does. Calling it again on the same
     * [dataSource] simulates a process restart, since nothing but saved state carries over.
     */
    private fun app(
        server: FakeOAuthServer,
        dataSource: DataStoreSavedStateDataSource,
    ): TestApp {
        val nonces = DPoPNonces()
        val sessions = PersistedAuthSessions(
            appMainScope = scope,
            ioDispatcher = Dispatchers.Default,
            savedStateDataSource = dataSource,
            tokenRefresher = NetworkTokenRefresher(
                httpClient = server.httpClient(),
                identityResolver = FakeIdentityResolver,
                nonces = nonces,
            ),
        )
        val sessionManager = PersistedSessionManager(
            httpClient = server.httpClient(),
            savedStateDataSource = dataSource,
            identityResolver = FakeIdentityResolver,
            oauthRedirect = OauthRedirect.ExternalRedirect,
            authSessions = sessions,
            nonces = nonces,
        )
        return TestApp(
            sessions = sessions,
            newClient = {
                server.httpClient().config {
                    sessionManager.manage(config = this)
                }
            },
        )
    }

    private class TestApp(
        val sessions: AuthSessions,
        val newClient: () -> HttpClient,
    ) {
        val client = newClient()
    }
}

/**
 * An authorization server and a PDS on separate hosts, each issuing its own DPoP nonce. Like
 * the reference implementation, refresh tokens rotate on every use and a rotated one is
 * rejected. The PDS asks for a new nonce only in the `WWW-Authenticate` header.
 */
private class FakeOAuthServer {

    private class Session(
        var accessToken: String,
        var refreshToken: String,
    )

    private val mutex = Mutex()
    private val sessions = mutableMapOf<String, Session>()
    private var issued = 0
    private var authNonce = "auth-nonce-0"
    private var pdsNonce = "pds-nonce-0"

    private var tokenFailure: TokenFailure? = null
    private var rejectsDPoPProofs = false

    private var heldResourceResponse: CompletableDeferred<Unit>? = null
    private val heldResourceRequest = CompletableDeferred<Unit>()
    private var heldTokenResponse: CompletableDeferred<Unit>? = null
    private val heldTokenRequest = CompletableDeferred<Unit>()

    var refreshCount = 0
        private set
    var rejectedRefreshCount = 0
        private set
    var failedTokenRequestCount = 0
        private set
    var rejectedAccessTokenCount = 0
        private set
    var authNonceChallengeCount = 0
        private set
    var pdsNonceChallengeCount = 0
        private set

    suspend fun signIn(
        did: String = UserDid,
        expiresIn: Duration? = null,
    ): SavedState.AuthTokens.Authenticated.DPoP {
        val session = mutex.withLock {
            Session(
                accessToken = nextToken("access"),
                refreshToken = nextToken("refresh"),
            ).also { sessions[did] = it }
        }
        val keyPair = DpopKeyPair.generateKeyPair()
        return SavedState.AuthTokens.Authenticated.DPoP(
            authProfileId = ProfileId(did),
            auth = session.accessToken,
            refresh = session.refreshToken,
            pdsUrl = PdsUrl,
            clientId = "https://heron.test/client-metadata.json",
            nonce = "",
            keyPair = SavedState.AuthTokens.Authenticated.DPoP.DERKeyPair(
                publicKey = keyPair.publicKey(DpopKeyPair.PublicKeyFormat.DER),
                privateKey = keyPair.privateKey(DpopKeyPair.PrivateKeyFormat.DER),
            ),
            issuerEndpoint = AuthUrl,
            expiresAt = expiresIn?.let { Clock.System.now() + it },
        )
    }

    suspend fun currentRefreshToken(
        did: String = UserDid,
    ) = mutex.withLock { sessions.getValue(did).refreshToken }

    suspend fun expireAccessToken(
        did: String = UserDid,
    ) = mutex.withLock { sessions.getValue(did).accessToken = nextToken("expired") }

    suspend fun revokeRefreshToken(
        did: String = UserDid,
    ) = mutex.withLock { sessions.getValue(did).refreshToken = nextToken("revoked") }

    suspend fun rotatePdsNonce() = mutex.withLock { pdsNonce = nextToken("pds-nonce") }

    // Answers token requests with [failure] instead, until cleared with null.
    fun failTokenRequests(failure: TokenFailure?) {
        tokenFailure = failure
    }

    fun rejectDPoPProofs() {
        rejectsDPoPProofs = true
    }

    fun holdNextResourceResponse(release: CompletableDeferred<Unit>) {
        heldResourceResponse = release
    }

    suspend fun awaitHeldResourceRequest() = heldResourceRequest.await()

    fun holdNextTokenResponse(release: CompletableDeferred<Unit>) {
        heldTokenResponse = release
    }

    suspend fun awaitHeldTokenRequest() = heldTokenRequest.await()

    fun httpClient() = HttpClient(MockEngine { request -> handle(request) }) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(BlueskyJson)
        }
    }

    private suspend fun MockRequestHandleScope.handle(
        request: HttpRequestData,
    ): HttpResponseData = when (request.url.host) {
        AuthHost -> handleAuthorizationServer(request)
        PdsHost -> handlePds(request)
        else -> error("Unexpected host ${request.url.host}")
    }

    private suspend fun MockRequestHandleScope.handleAuthorizationServer(
        request: HttpRequestData,
    ): HttpResponseData {
        val path = request.url.encodedPath
        if (path.endsWith("/.well-known/oauth-authorization-server")) return json(
            nonce = authNonce,
            body = """
                {
                  "issuer": "$AuthUrl",
                  "scopes_supported": ["atproto", "transition:generic", "transition:chat.bsky"],
                  "code_challenge_methods_supported": ["S256"],
                  "authorization_endpoint": "$AuthUrl/oauth/authorize",
                  "token_endpoint": "$AuthUrl/oauth/token",
                  "revocation_endpoint": "$AuthUrl/oauth/revoke",
                  "pushed_authorization_request_endpoint": "$AuthUrl/oauth/par"
                }
            """.trimIndent(),
        )
        check(path.endsWith("/oauth/token")) { "Unexpected path $path" }
        tokenFailure?.let { failure ->
            mutex.withLock { failedTokenRequestCount++ }
            return failure.invoke(this)
        }

        val refreshToken = (request.body as OutgoingContent.ByteArrayContent)
            .bytes()
            .decodeToString()
            .parseUrlEncodedParameters()["refresh_token"]

        val session = mutex.withLock {
            if (request.dpopNonce != authNonce) {
                authNonceChallengeCount++
                return json(
                    nonce = authNonce,
                    body = """{"error":"use_dpop_nonce","message":"Authorization server requires nonce"}""",
                    status = HttpStatusCode.BadRequest,
                )
            }
            val session = sessions.values.firstOrNull { it.refreshToken == refreshToken }
            if (session == null) {
                rejectedRefreshCount++
                return json(
                    nonce = authNonce,
                    body = """{"error":"invalid_grant","message":"refresh token replayed"}""",
                    status = HttpStatusCode.BadRequest,
                )
            }
            refreshCount++
            // The rotation commits before the response is sent.
            session.accessToken = nextToken("access")
            session.refreshToken = nextToken("refresh")
            session
        }
        val did = mutex.withLock { sessions.entries.first { it.value === session }.key }

        heldTokenResponse?.let { release ->
            heldTokenResponse = null
            heldTokenRequest.complete(Unit)
            release.await()
        }
        return json(
            nonce = authNonce,
            body = """
                {
                  "access_token": "${session.accessToken}",
                  "refresh_token": "${session.refreshToken}",
                  "expires_in": 900,
                  "scope": "atproto transition:generic transition:chat.bsky",
                  "sub": "$did",
                  "token_type": "DPoP"
                }
            """.trimIndent(),
        )
    }

    private suspend fun MockRequestHandleScope.handlePds(
        request: HttpRequestData,
    ): HttpResponseData {
        val accessToken = request.headers[HttpHeaders.Authorization]?.removePrefix("DPoP ")
        val authorized = mutex.withLock {
            if (request.dpopNonce != pdsNonce) {
                pdsNonceChallengeCount++
                return respond(
                    content = "",
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf(
                        HttpHeaders.WWWAuthenticate to listOf("""DPoP error="use_dpop_nonce""""),
                        DPoPNonceHeader to listOf(pdsNonce),
                    ),
                )
            }
            sessions.values.any { it.accessToken == accessToken }
                .also { if (!it) rejectedAccessTokenCount++ }
        }

        if (rejectsDPoPProofs) return respond(
            content = "",
            status = HttpStatusCode.Unauthorized,
            headers = headersOf(
                HttpHeaders.WWWAuthenticate to listOf("""DPoP error="invalid_dpop_proof""""),
                DPoPNonceHeader to listOf(pdsNonce),
            ),
        )

        if (request.url.encodedPath.endsWith(ProxiedRejectionPath)) return json(
            nonce = pdsNonce,
            body = """{"error":"InvalidToken","message":"Bad service auth token"}""",
            status = HttpStatusCode.Unauthorized,
        )

        if (request.url.encodedPath.endsWith(UnauthorizedFeedPath)) return json(
            nonce = pdsNonce,
            body = """{"error":"AuthMissing","message":"Feed generator rejected the request"}""",
            status = HttpStatusCode.Unauthorized,
        )

        // The token is checked on arrival, then the response may be held back.
        heldResourceResponse?.let { release ->
            heldResourceResponse = null
            heldResourceRequest.complete(Unit)
            release.await()
        }
        yield()
        return if (authorized) json(
            nonce = pdsNonce,
            body = "{}",
        )
        else json(
            nonce = pdsNonce,
            body = """{"error":"ExpiredToken","message":"Token has expired"}""",
            status = HttpStatusCode.BadRequest,
        )
    }

    private fun nextToken(
        prefix: String,
    ) = "$prefix-${++issued}"

    private fun MockRequestHandleScope.json(
        nonce: String,
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(
        content = body,
        status = status,
        headers = headersOf(
            HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
            DPoPNonceHeader to listOf(nonce),
        ),
    )
}

private typealias TokenFailure = suspend MockRequestHandleScope.() -> HttpResponseData

private val JsonHeaders = headersOf(
    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
)

// The nonce claim of the request's DPoP proof.
private val HttpRequestData.dpopNonce: String?
    get() = headers["DPoP"]
        ?.split('.')
        ?.getOrNull(1)
        ?.let {
            Base64.UrlSafe
                .withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
                .decode(it)
                .decodeToString()
        }
        ?.let { BlueskyJson.parseToJsonElement(it).jsonObject["nonce"] as? JsonPrimitive }
        ?.content

// Providers can only be registered once per process.
@OptIn(CryptographyProviderApi::class)
private val registerCryptographyProvider: Unit by lazy {
    CryptographySystem.registerProvider(lazyOf(platformCryptographyProvider()), 0)
}

private object FakeIdentityResolver : IdentityResolver {
    override suspend fun resolvePds(did: Did) = null
    override suspend fun resolveServer(handle: Handle): Server? = null
    override suspend fun resolveFeedServiceDid(feedUri: FeedGeneratorUri): Did? = null
}

private const val AuthHost = "auth.heron.test"
private const val PdsHost = "pds.heron.test"
private const val AuthUrl = "https://$AuthHost"
private const val PdsUrl = "https://$PdsHost"
private const val UserDid = "did:plc:heronracetest"
private const val PastUserDid = "did:plc:heronpastsession"
private const val DPoPNonceHeader = "DPoP-Nonce"
private const val UnauthorizedFeedPath = "app.bsky.feed.getFeed"
private const val XrpcUrl = "$PdsUrl/xrpc/app.bsky.actor.getPreferences"
private const val UnauthorizedFeedUrl = "$PdsUrl/xrpc/$UnauthorizedFeedPath"

// Proxied to Heron's app view.
private const val ProxiedRejectionPath = "social.heron.gif.searchGifs"
private const val ProxiedRejectionUrl = "$PdsUrl/xrpc/$ProxiedRejectionPath"
