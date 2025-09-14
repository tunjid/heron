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

import com.tunjid.heron.data.network.oauth.OAuthScope.Companion.Generic
import kotlinx.serialization.Serializable

/**
 * OAuth scopes allow more granular control over the resources and actions a client is granted access to.
 */
@Serializable
data class OAuthScope(val value: String) {
    companion object {
        /**
         * This scope is required for all atproto OAuth sessions. The semantics are somewhat similar to the `openid` scope:
         * inclusion of it confirms that the client is using the `atproto` profile of OAuth and will comply with all the
         * requirements laid out in this specification. No access to any atproto-specific PDS resources will be granted
         * without this scope included.
         */
        val AtProto = OAuthScope("atproto")

        /**
         * Broad PDS account permissions, equivalent to the previous "App Password" authorization level:
         *
         * - Write (create/update/delete) any repository record type
         * - Upload blobs (media files)
         * - Read and write any personal preferences
         * - API endpoints and service proxying for most Lexicon endpoints, to any service provider (identified by DID)
         * - Ability to generate service auth tokens for the specific API endpoints the client has access to
         */
        val Generic = OAuthScope("transition:generic")

        /**
         * Equivalent to adding the "DM Access" toggle for "App Passwords"
         *
         * - API endpoints and service proxying for the `chat.bsky` Lexicons specifically
         * - Ability to generate service auth tokens for the `chat.bsky` Lexicons
         * - This scope depends on and does not function without [Generic]
         */
        val BlueskyChat = OAuthScope("transition:chat.bsky")

        /**
         * Access to the account email address
         *
         * - Email address (and confirmation status) gets included in response to `com.atproto.server.getSession` endpoint
         */
        val Email = OAuthScope("transition:email")
    }
}
