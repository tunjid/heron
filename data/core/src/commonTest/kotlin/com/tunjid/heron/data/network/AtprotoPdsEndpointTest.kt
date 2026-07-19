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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import sh.christian.ozone.api.Did

class AtprotoPdsEndpointTest {

    private val did = Did("did:plc:ewvi7nxzyoun6zhxrhs64oiz")

    @Test
    fun relativeServiceId_returnsEndpoint() {
        assertEquals(
            expected = "https://pds.example.com",
            actual = didDoc(
                service(
                    id = "#atproto_pds",
                    serviceEndpoint = "https://pds.example.com",
                ),
            ).atprotoPdsEndpoint(did),
        )
    }

    @Test
    fun didQualifiedServiceId_returnsEndpoint() {
        assertEquals(
            expected = "https://pds.example.com",
            actual = didDoc(
                service(
                    id = "${did.did}#atproto_pds",
                    serviceEndpoint = "https://pds.example.com",
                ),
            ).atprotoPdsEndpoint(did),
        )
    }

    @Test
    fun picksPdsAmongOtherServices() {
        assertEquals(
            expected = "https://pds.example.com",
            actual = didDoc(
                service(
                    id = "#atproto_labeler",
                    type = "AtprotoLabeler",
                    serviceEndpoint = "https://labeler.example.com",
                ),
                service(
                    id = "#atproto_pds",
                    serviceEndpoint = "https://pds.example.com",
                ),
            ).atprotoPdsEndpoint(did),
        )
    }

    @Test
    fun matchingIdButWrongType_returnsNull() {
        assertNull(
            didDoc(
                service(
                    id = "#atproto_pds",
                    type = "SomethingElse",
                    serviceEndpoint = "https://pds.example.com",
                ),
            ).atprotoPdsEndpoint(did),
        )
    }

    @Test
    fun noPdsService_returnsNull() {
        assertNull(
            didDoc(
                service(
                    id = "#atproto_labeler",
                    type = "AtprotoLabeler",
                    serviceEndpoint = "https://labeler.example.com",
                ),
            ).atprotoPdsEndpoint(did),
        )
    }

    private fun service(
        id: String,
        type: String = "AtprotoPersonalDataServer",
        serviceEndpoint: String,
    ) = SavedState.AuthTokens.DidDoc.Service(
        id = id,
        type = type,
        serviceEndpoint = serviceEndpoint,
    )

    private fun didDoc(
        vararg services: SavedState.AuthTokens.DidDoc.Service,
    ) = SavedState.AuthTokens.DidDoc(
        service = services.toList(),
    )
}
