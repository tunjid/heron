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

import com.tunjid.heron.data.core.models.Link
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.types.ConversationId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
class SendMessageWritableTest {

    @Test
    fun queueId_withoutLocalId_matchesIdFromBeforeLocalIdExisted() {
        val writable = Writable.Send(
            request = messageCreate(
                text = "hello",
                localId = null,
            ),
        )

        assertEquals(
            expected = "send-message-Create(conversationId=convo-1, text=hello, links=[], recordReference=null)",
            actual = writable.queueId,
        )
    }

    @Test
    fun queueId_withLocalId_includesLocalId() {
        val writable = Writable.Send(
            request = messageCreate(
                text = "hello",
                localId = "local-1",
            ),
        )

        assertEquals(
            expected = "send-message-Create(conversationId=convo-1, text=hello, links=[], recordReference=null, localId=local-1)",
            actual = writable.queueId,
        )
    }

    @Test
    fun queueId_withoutLocalId_leavesMatchingTextIntact() {
        val text = "a, localId=null)"
        val writable = Writable.Send(
            request = messageCreate(
                text = text,
                localId = null,
            ),
        )

        assertEquals(
            expected = "send-message-Create(conversationId=convo-1, text=$text, links=[], recordReference=null)",
            actual = writable.queueId,
        )
    }

    @Test
    fun queueId_identicalMessagesWithDifferentLocalIds_doNotCollide() {
        val first = Writable.Send(
            request = messageCreate(
                text = "ok",
                localId = "local-1",
            ),
        )
        val second = Writable.Send(
            request = messageCreate(
                text = "ok",
                localId = "local-2",
            ),
        )

        assertNotEquals(
            illegal = first.queueId,
            actual = second.queueId,
        )
    }

    @Test
    fun send_roundTripsAsPersistedWritable_withStableQueueId() {
        listOf(null, "local-1").forEach { localId ->
            val writable: Writable = Writable.Send(
                request = messageCreate(
                    text = "hello",
                    localId = localId,
                ),
            )

            val bytes = ProtoBuf.encodeToByteArray(Writable.serializer(), writable)
            val decoded = ProtoBuf.decodeFromByteArray(Writable.serializer(), bytes)

            assertEquals(
                expected = writable,
                actual = decoded,
            )
            assertEquals(
                expected = writable.queueId,
                actual = decoded.queueId,
            )
        }
    }

    @Test
    fun send_persistedBeforeLocalIdExisted_decodesWithLegacyQueueId() {
        val legacy = LegacyMessageCreate(
            conversationId = ConversationId("convo-1"),
            text = "hello",
            links = emptyList(),
            recordReference = null,
        )

        val bytes = ProtoBuf.encodeToByteArray(LegacyMessageCreate.serializer(), legacy)
        val decoded = ProtoBuf.decodeFromByteArray(Message.Create.serializer(), bytes)

        assertNull(decoded.localId)
        assertEquals(
            expected = "send-message-Create(conversationId=convo-1, text=hello, links=[], recordReference=null)",
            actual = Writable.Send(request = decoded).queueId,
        )
    }
}

/**
 * [Message.Create] as it was before [Message.Create.localId] was added.
 */
@Serializable
private data class LegacyMessageCreate(
    val conversationId: ConversationId,
    val text: String,
    val links: List<Link>,
    val recordReference: Record.Reference?,
)

private fun messageCreate(
    text: String,
    localId: String?,
) = Message.Create(
    conversationId = ConversationId("convo-1"),
    text = text,
    links = emptyList(),
    recordReference = null,
    localId = localId,
)
