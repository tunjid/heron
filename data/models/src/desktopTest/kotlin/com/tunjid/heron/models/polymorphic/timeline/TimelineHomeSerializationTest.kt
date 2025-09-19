package com.tunjid.heron.models.polymorphic.timeline

import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.fakes.FakeTimeline
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class TimelineHomeSerializationTest {

    @ParameterizedTest(name = "[{index}] {1} can be serialized with {0}")
    @MethodSource("timelineHomeCases")
    fun `round trip Timeline Home`(
        format: SerializationTestHelper.Format,
        original: Timeline.Home,
    ) {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Timeline.Home.serializer(),
        )
        assertEquals(original, decoded)
    }

    companion object {
        @JvmStatic
        private fun timelineHomeCases(): List<Arguments> {
            val homes = listOf(
                FakeTimeline.following,
                FakeTimeline.listTimeline,
                FakeTimeline.feedTimeline,
            )
            return SerializationTestHelper.Format.entries.flatMap { format ->
                homes.map { home -> Arguments.of(format, home) }
            }
        }
    }
}
