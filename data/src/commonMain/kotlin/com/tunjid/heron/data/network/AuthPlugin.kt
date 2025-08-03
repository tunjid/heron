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
import com.tunjid.heron.data.repository.SavedState
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.body
import io.ktor.client.call.save
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.Url
import io.ktor.http.encodedPath
import io.ktor.http.set
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CancellationException
import sh.christian.ozone.api.response.AtpErrorDescription

class ErrorInterceptorConfig {
    internal var networkErrorConverter: ((String) -> AtpErrorDescription)? = null
    internal var readAuth: (suspend () -> SavedState.AuthTokens?)? = null
    internal var saveAuth: (suspend (SavedState.AuthTokens?) -> Unit)? = null
}

/**
 * Invalidates session cookies that have expired or are otherwise invalid
 */
internal class AuthPlugin(
    private val networkErrorConverter: ((String) -> AtpErrorDescription)?,
    private val readAuth: (suspend () -> SavedState.AuthTokens?)?,
    private val saveAuth: (suspend (SavedState.AuthTokens?) -> Unit)?,
) {

    companion object : HttpClientPlugin<ErrorInterceptorConfig, AuthPlugin> {
        override val key: AttributeKey<AuthPlugin> =
            AttributeKey("ClientNetworkErrorInterceptor")

        override fun prepare(block: ErrorInterceptorConfig.() -> Unit): AuthPlugin {
            val config = ErrorInterceptorConfig().apply(block)
            return AuthPlugin(
                networkErrorConverter = config.networkErrorConverter,
                readAuth = config.readAuth,
                saveAuth = config.saveAuth,
            )
        }

        override fun install(
            plugin: AuthPlugin,
            scope: HttpClient,
        ) {
            scope.plugin(HttpSend).intercept { context ->
                val authTokens = plugin.readAuth?.invoke()

                if (ChatProxyPaths.any(predicate = context.url.encodedPath::endsWith)) {
                    context.headers.append(
                        name = AtProtoProxyHeader,
                        value = ChatAtProtoProxyHeaderValue,
                    )
                    authTokens?.didDoc
                        ?.service
                        ?.firstOrNull()
                        ?.let {
                            context.url.set(
                                host = Url(urlString = it.serviceEndpoint).host
                            )
                        }
                }

                if (authTokens == null && SignedOutPaths.any(predicate = context.url.encodedPath::endsWith)) {
                    context.url.set(
                        host = Url(urlString = SignedOutUrl).host
                    )
                }

                if (!context.headers.contains(Authorization)) {
                    authTokens?.auth?.let { context.bearerAuth(it) }
                }

                var result: HttpClientCall = execute(context)
                if (result.response.status != BadRequest) {
                    return@intercept result
                }

                // Cache the response in memory since we will need to decode it potentially more than once.
                result = result.save()

                val response = runCatching<AtpErrorDescription?> {
                    plugin.networkErrorConverter?.invoke(result.response.bodyAsText())
                }

                if (response.getOrNull()?.error == "ExpiredToken") {
                    try {
                        scope.post(RefreshTokenEndpoint) {
                            plugin.readAuth?.invoke()?.refresh?.let { bearerAuth(it) }
                        }.body<RefreshSessionResponse>()
                            .let { refreshed ->
                                val newAccessToken = refreshed.accessJwt
                                val newRefreshToken = refreshed.refreshJwt

                                plugin.saveAuth?.invoke(
                                    SavedState.AuthTokens(
                                        authProfileId = ProfileId(refreshed.did.did),
                                        auth = newAccessToken,
                                        refresh = newRefreshToken,
                                        didDoc = SavedState.AuthTokens.DidDoc.fromJsonContentOrEmpty(
                                            jsonContent = refreshed.didDoc,
                                        ),
                                    )
                                )
                                context.headers.remove(Authorization)
                                context.bearerAuth(newAccessToken)
                                result = execute(context)
                            }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Throwable) {
                        // Delete existing token and force a log out
                        plugin.saveAuth?.invoke(null)
                    }
                }
                result
            }
        }
    }
}

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
    "app.bsky.feed.getAuthorFeed",
    "app.bsky.feed.getFeed",
    "app.bsky.feed.getPostThread",
    "app.bsky.feed.getFeedGenerator",
    "app.bsky.unspecced.getPopularFeedGenerators",
    "app.bsky.unspecced.getTrends",
)

private const val AtProtoProxyHeader = "Atproto-Proxy"
private const val ChatAtProtoProxyHeaderValue = "did:web:api.bsky.chat#bsky_chat"
private const val SignedOutUrl = "https://public.api.bsky.app"
private const val RefreshTokenEndpoint = "/xrpc/com.atproto.server.refreshSession"
