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

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContainsIssuerTest {

    private val issuer = "https://bsky.social"

    @Test
    fun exactIssuer_isContained() {
        assertTrue(
            listOf("https://bsky.social").containsIssuer(issuer = issuer),
        )
    }

    @Test
    fun cosmeticDifferences_areSameOrigin() {
        assertTrue(
            listOf("https://bsky.social/").containsIssuer(issuer = issuer),
        )
        assertTrue(
            listOf("https://bsky.social:443").containsIssuer(issuer = issuer),
        )
        assertTrue(
            listOf("https://BSKY.social").containsIssuer(issuer = issuer),
        )
    }

    @Test
    fun matchAmongOtherServers_isContained() {
        assertTrue(
            listOf(
                "https://entryway.example.com",
                "https://bsky.social",
            ).containsIssuer(issuer = issuer),
        )
    }

    @Test
    fun differentOrigin_isNotContained() {
        assertFalse(
            listOf("https://evil.example.com").containsIssuer(issuer = issuer),
        )
        assertFalse(
            listOf("http://bsky.social").containsIssuer(issuer = issuer),
        )
        assertFalse(
            listOf("https://bsky.social:8443").containsIssuer(issuer = issuer),
        )
    }

    @Test
    fun noAuthorizationServers_isNotContained() {
        assertFalse(
            emptyList<String>().containsIssuer(issuer = issuer),
        )
    }
}
