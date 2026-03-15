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

import com.tunjid.heron.data.core.models.Server
import com.tunjid.heron.data.core.models.SessionRequest
import com.tunjid.heron.data.core.types.GenericUri
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A loopback HTTP server that listens on 127.0.0.1 for OAuth redirects.
 * The port is dynamically assigned by the OS.
 */
private class LoopbackRedirect : OauthRedirect() {

    private var server: EmbeddedServer<*, *>? = null
    private val mutex = Mutex()
    private val requests = MutableSharedFlow<SessionRequest.Oauth>(
        extraBufferCapacity = 1,
    )

    override val clientId: String = HeronDesktopOauthClient.clientId

    override var initializedClient: OAuthClient = HeronDesktopOauthClient

    override val sessionRequests: Flow<SessionRequest.Oauth> = requests

    override suspend fun initializeOAuthClient(): OAuthClient = mutex.withLock {
        stop()

        val embeddedServer = embeddedServer(
            factory = CIO,
            host = LoopbackHost,
            port = 0,
        ) {
            routing {
                get(CallbackPath) {
                    val issuer = call.parameters[IssuerParameter] ?: return@get call.respondText(
                        text = ErrorHtml,
                        contentType = ContentType.Text.Html,
                        status = HttpStatusCode.BadRequest,
                    )
                    call.respondText(
                        text = SuccessHtml,
                        contentType = ContentType.Text.Html,
                    )
                    requests.tryEmit(
                        SessionRequest.Oauth(
                            callbackUri = GenericUri(call.request.local.uri),
                            server = Server(
                                endpoint = issuer,
                                supportsOauth = true,
                            ),
                        ),
                    )
                    stop()
                }
            }
        }
        embeddedServer.start()
        server = embeddedServer

        val resolvedPort = embeddedServer.engine.resolvedConnectors()
            .first()
            .port

        return OAuthClient(
            clientId = clientId,
            redirectUri = "http://$LoopbackHost:$resolvedPort$CallbackPath",
        ).also(::initializedClient::set)
    }

    private suspend fun stop() {
        server?.stopSuspend(
            gracePeriodMillis = ServerStopGracePeriodMillis,
            timeoutMillis = ServerStopTimeoutMillis,
        )
        server = null
    }
}

internal actual fun oauthRedirect(): OauthRedirect =
    LoopbackRedirect()

private val HeronDesktopOauthClient = OAuthClient(
    clientId = "https://heron.tunji.dev/oauth-client-metadata.json",
    redirectUri = "http://$LoopbackHost$CallbackPath",
)

private val SuccessHtml = """
    <!DOCTYPE html>
    <html>
    <body style="display:flex;justify-content:center;align-items:center;height:100vh;font-family:system-ui;">
    <p>Sign-in successful. You can close this tab and return to Heron.</p>
    </body>
    </html>
""".trimIndent()

private val ErrorHtml = """
    <!DOCTYPE html>
    <html>
    <body style="display:flex;justify-content:center;align-items:center;height:100vh;font-family:system-ui;">
    <p>Sign-in failed. Please close this tab and try again.</p>
    </body>
    </html>
""".trimIndent()

private const val LoopbackHost = "127.0.0.1"
private const val CallbackPath = "/oauth/callback"
private const val IssuerParameter = "iss"

private const val ServerStopGracePeriodMillis = 1000L
private const val ServerStopTimeoutMillis = 1000L
