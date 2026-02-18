/*
 *    MIT License
 *
 *    Copyright (c) 2023 Christian De Angelis
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy
 *    of this software and associated documentation files (the "Software"), to deal
 *    in the Software without restriction, including without limitation the rights
 *    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *    copies of the Software, and to permit persons to whom the Software is
 *    furnished to do so, subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included in all
 *    copies or substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *    SOFTWARE.
 */

package com.tunjid.heron.data.network.oauth

import com.tunjid.heron.data.network.oauth.OAuthScope.Companion.Generic
import kotlinx.serialization.Serializable

/**
 * OAuth scopes allow more granular control over the resources and actions a client is granted
 * access to.
 */
@Serializable
data class OAuthScope(val value: String) {
    companion object {
        /**
         * This scope is required for all atproto OAuth sessions. The semantics are somewhat similar
         * to the `openid` scope: inclusion of it confirms that the client is using the `atproto`
         * profile of OAuth and will comply with all the requirements laid out in this
         * specification. No access to any atproto-specific PDS resources will be granted without
         * this scope included.
         */
        val AtProto = OAuthScope("atproto")

        /**
         * Broad PDS account permissions, equivalent to the previous "App Password" authorization
         * level:
         * - Write (create/update/delete) any repository record type
         * - Upload blobs (media files)
         * - Read and write any personal preferences
         * - API endpoints and service proxying for most Lexicon endpoints, to any service provider
         *   (identified by DID)
         * - Ability to generate service auth tokens for the specific API endpoints the client has
         *   access to
         */
        val Generic = OAuthScope("transition:generic")

        /**
         * Equivalent to adding the "DM Access" toggle for "App Passwords"
         * - API endpoints and service proxying for the `chat.bsky` Lexicons specifically
         * - Ability to generate service auth tokens for the `chat.bsky` Lexicons
         * - This scope depends on and does not function without [Generic]
         */
        val BlueskyChat = OAuthScope("transition:chat.bsky")

        /**
         * Access to the account email address
         * - Email address (and confirmation status) gets included in response to
         *   `com.atproto.server.getSession` endpoint
         */
        val Email = OAuthScope("transition:email")
    }
}
