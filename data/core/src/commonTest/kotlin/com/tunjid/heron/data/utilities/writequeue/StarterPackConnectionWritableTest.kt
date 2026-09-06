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

package com.tunjid.heron.data.utilities.writequeue

import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.StarterPackId
import com.tunjid.heron.data.core.types.StarterPackUri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
class StarterPackConnectionWritableTest {

    @Test
    fun followStarterPackConnection_roundTripsAsPersistedWritable() {
        val connection = Profile.Connection.FollowStarterPack(
            signedInProfileId = ProfileId("did:example:me"),
            starterPackUri = StarterPackUri(
                "at://did:example:creator/${StarterPackUri.NAMESPACE}/3333333333333",
            ),
            starterPackCid = StarterPackId("bafyreiexamplestarterpackcid"),
            listUri = ListUri("at://did:example:creator/${ListUri.NAMESPACE}/4444444444444"),
        )
        val writable: Writable = Writable.Connection(connection)

        val bytes = ProtoBuf.encodeToByteArray(Writable.serializer(), writable)
        val decoded = ProtoBuf.decodeFromByteArray(Writable.serializer(), bytes)

        assertEquals(
            expected = writable,
            actual = decoded,
        )
        val connectionWritable = assertIs<Writable.Connection>(decoded)
        assertEquals(
            expected = connection,
            actual = connectionWritable.connection,
        )
        assertEquals(
            expected = "follow-starter-pack-${connection.starterPackUri}",
            actual = connectionWritable.queueId,
        )
    }
}
