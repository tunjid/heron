package com.tunjid.heron.models.polymorphic.message

import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.fakes.sampleMessageReactionAdd
import com.tunjid.heron.fakes.sampleMessageReactionRemove
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class MessageUpdateReactionSerializationTest {

    @ParameterizedTest(name = "[{index}] {1} can be serialized with {0}")
    @MethodSource("reactionCases")
    fun `round trip Message UpdateReaction`(
        format: SerializationTestHelper.Format,
        original: Message.UpdateReaction,
    ) {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Message.UpdateReaction.serializer(),
        )
        assertEquals(original, decoded)
    }

    companion object {
        @JvmStatic
        fun reactionCases(): List<Arguments> {
            val reactions = listOf(
                sampleMessageReactionAdd(),
                sampleMessageReactionRemove(),
            )
            return SerializationTestHelper.Format.entries.flatMap { format ->
                reactions.map { r -> Arguments.of(format, r) }
            }
        }
    }
}
