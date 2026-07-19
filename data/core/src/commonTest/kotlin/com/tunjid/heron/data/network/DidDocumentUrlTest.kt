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
import kotlin.test.assertEquals
import kotlin.test.assertNull
import sh.christian.ozone.api.Did

class DidDocumentUrlTest {

    @Test
    fun didPlc_resolvesThroughPlcDirectory() {
        assertEquals(
            expected = "https://plc.directory/did:plc:ewvi7nxzyoun6zhxrhs64oiz",
            actual = Did("did:plc:ewvi7nxzyoun6zhxrhs64oiz").didDocumentUrl(),
        )
    }

    @Test
    fun didWeb_bareHost_resolvesThroughWellKnown() {
        assertEquals(
            expected = "https://pds.example.com/.well-known/did.json",
            actual = Did("did:web:pds.example.com").didDocumentUrl(),
        )
    }

    @Test
    fun didWeb_hostWithPercentEncodedPort_isDecoded() {
        assertEquals(
            expected = "https://localhost:3000/.well-known/did.json",
            actual = Did("did:web:localhost%3A3000").didDocumentUrl(),
        )
    }

    @Test
    fun didWeb_withPathSegments_resolvesThroughPathDidJson() {
        assertEquals(
            expected = "https://example.com/user/alice/did.json",
            actual = Did("did:web:example.com:user:alice").didDocumentUrl(),
        )
    }

    @Test
    fun didWeb_withPercentEncodedPathSegments_isDecoded() {
        assertEquals(
            expected = "https://example.com/user/alice/did.json",
            actual = Did("did:web:example.com:user:%61lice").didDocumentUrl(),
        )
    }

    @Test
    fun unsupportedDidMethod_resolvesToNull() {
        assertNull(
            Did("did:key:z6MkhaXgBZDvotDkL5257faiztiGiC2QtKLGpbnnEGta2doK").didDocumentUrl(),
        )
    }
}
