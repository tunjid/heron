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

import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.FeedReqId
import com.tunjid.heron.data.core.types.PostUri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
class FeedInteractionWritableTest {

    @Test
    fun requestFeedInteraction_roundTripsAsPersistedWritable() {
        val interaction = FeedGenerator.Interaction.Request(
            feedUri = FeedGeneratorUri("at://feed/generator/1"),
            postUri = PostUri("at://post/123"),
            event = FeedGenerator.Interaction.Event.Request.Less,
            feedContext = "ctx-abc",
            reqId = FeedReqId("req-123"),
        )
        val writable: Writable = Writable.FeedInteraction(
            requests = listOf(interaction),
        )

        val bytes = ProtoBuf.encodeToByteArray(Writable.serializer(), writable)
        val decoded = ProtoBuf.decodeFromByteArray(Writable.serializer(), bytes)

        assertEquals(
            expected = writable,
            actual = decoded,
        )
        val feedInteraction = assertIs<Writable.FeedInteraction>(decoded)
        assertEquals(
            expected = interaction,
            actual = feedInteraction.interactions.single(),
        )
        assertEquals(
            expected = writable.queueId,
            actual = feedInteraction.queueId,
        )
    }
}
