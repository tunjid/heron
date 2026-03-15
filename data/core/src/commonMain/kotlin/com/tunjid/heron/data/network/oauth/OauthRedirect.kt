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

import com.tunjid.heron.data.core.models.SessionRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Handles OAuth redirect callbacks. On desktop, this is a loopback HTTP server
 * that listens for the OAuth redirect. On other platforms, redirects are handled
 * externally by the OS (e.g., deep links on Android).
 */
abstract class OauthRedirect {

    abstract val clientId: String
    abstract val initializedClient: OAuthClient

    abstract val sessionRequests: Flow<SessionRequest.Oauth>

    abstract suspend fun initializeOAuthClient(): OAuthClient

    /**
     * Redirects are handled externally by the OS (e.g., deep links on Android,
     * universal links on iOS).
     */
    data object ExternalRedirect : OauthRedirect() {

        override val clientId: String
            get() = HeronOauthClient.clientId

        override val initializedClient: OAuthClient
            get() = HeronOauthClient

        override val sessionRequests: Flow<SessionRequest.Oauth> = emptyFlow()

        override suspend fun initializeOAuthClient() = HeronOauthClient
    }
}

internal expect fun oauthRedirect(): OauthRedirect

private val HeronOauthClient = OAuthClient(
    clientId = "https://heron.tunji.dev/oauth-client.json",
    redirectUri = "https://heron.tunji.dev/oauth/callback",
)
