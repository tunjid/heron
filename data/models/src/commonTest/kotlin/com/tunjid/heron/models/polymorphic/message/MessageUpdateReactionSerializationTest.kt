package com.tunjid.heron.models.polymorphic.message

import app.cash.burst.Burst
import app.cash.burst.burstValues
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.fakes.sampleMessageReactionAdd
import com.tunjid.heron.fakes.sampleMessageReactionRemove
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.Test
import kotlin.test.assertEquals

@Burst
class MessageUpdateReactionSerializationTest(
    val format: SerializationTestHelper.Format = burstValues(
        SerializationTestHelper.Format.CBOR,
        SerializationTestHelper.Format.PROTOBUF,
    ),
    val original: Message.UpdateReaction = burstValues(
        sampleMessageReactionAdd(),
        sampleMessageReactionRemove(),
    ),
) {
    @Test
    fun roundTrip() {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Message.UpdateReaction.serializer(),
        )
        assertEquals(original, decoded)
    }
}
