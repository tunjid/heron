package com.tunjid.heron.models.polymorphic.timeline

import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.fakes.FakeTimeline
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class TimelineStarterPackSerializationTest {

    @ParameterizedTest(name = "[{index}] {1} can be serialized with {0}")
    @MethodSource("starterPackCases")
    fun `round trip Timeline StarterPack`(
        format: SerializationTestHelper.Format,
        original: Timeline.StarterPack,
    ) {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Timeline.StarterPack.serializer(),
        )
        assertEquals(original, decoded)
    }

    companion object {
        @JvmStatic
        private fun starterPackCases(): List<Arguments> {
            val starterPack = FakeTimeline.starterPackTimeline
            return SerializationTestHelper.Format.entries.map { format ->
                Arguments.of(format, starterPack)
            }
        }
    }
}
