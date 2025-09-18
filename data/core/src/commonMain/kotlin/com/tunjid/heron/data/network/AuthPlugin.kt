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

import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.utilities.InvalidTokenException
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.save
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.http.set
import sh.christian.ozone.api.response.AtpErrorDescription
import sh.christian.ozone.api.response.AtpException

/**
 * Invalidates session cookies that have expired or are otherwise invalid
 */
internal fun atProtoAuth(
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

    if (currentDPoPNonce == existingToken.nonce) return null

    return existingToken.copy(nonce = currentDPoPNonce)
}

private fun HttpRequestBuilder.clearAuth() {
    headers.remove(Authorization)
    headers.remove(DPoP)
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
private const val ExpiredTokenError = "ExpiredToken"
private const val InvalidTokenError = "invalid_token"
private const val UseDPoPNonce = "use_dpop_nonce"
private const val DPoPNonceHeaderKey = "DPoP-Nonce"
