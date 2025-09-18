package com.tunjid.heron.models.polymorphic.timeline

import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.fakes.FakeTimeline
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class TimelineSerializationTest {

    @ParameterizedTest(name = "[{index}] {1} can be serialized with {0}")
    @MethodSource("timelineCases")
    fun `round trip Timeline`(
        format: SerializationTestHelper.Format,
        original: Timeline,
    ) {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Timeline.serializer(),
        )
        assertEquals(original, decoded)
    }

    companion object {
        @JvmStatic
        private fun timelineCases(): List<Arguments> {
            return SerializationTestHelper.Format.entries.flatMap { format ->
                FakeTimeline.all.map { timeline ->
                    Arguments.of(format, timeline)
                }
            }
        }
    }
}
