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

import kotlinx.serialization.Serializable

/**
 * Represents an OAuth client with its client ID and redirect URI.
 *
 * @param clientId The unique identifier for the OAuth client.
 * @param redirectUri The URI to which the authorization server will redirect the user after authorization.
 *                    This URI must match one of the redirect URIs registered for the client.
 */
@Serializable
data class OAuthClient(
    val clientId: String,
    val redirectUri: String,
)
