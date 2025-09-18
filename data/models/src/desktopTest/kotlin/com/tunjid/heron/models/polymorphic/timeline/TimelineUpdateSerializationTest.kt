package com.tunjid.heron.models.polymorphic.timeline

import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.fakes.FakeTimeline
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.assertEquals
import kotlinx.serialization.serializer
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class TimelineUpdateSerializationTest {

    @ParameterizedTest(name = "[{index}] {1} can be serialized with {0}")
    @MethodSource("timelineUpdateCases")
    fun `round trip Timeline Update`(
        format: SerializationTestHelper.Format,
        original: Timeline.Update,
    ) {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = serializer<Timeline.Update>(),
        )
        assertEquals(original, decoded)
    }

    companion object {
        @JvmStatic
        private fun timelineUpdateCases(): List<Arguments> {
            val fakeHome = FakeTimeline.following

            val bulkUpdate = Timeline.Update.Bulk(
                timelines = listOf(fakeHome),
            )

            val pinUpdate = Timeline.Update.OfFeedGenerator.Pin(
                uri = FeedGeneratorUri("at://feed/generator/1"),
            )

            val saveUpdate = Timeline.Update.OfFeedGenerator.Save(
                uri = FeedGeneratorUri("at://feed/generator/2"),
            )

            val removeUpdate = Timeline.Update.OfFeedGenerator.Remove(
                uri = FeedGeneratorUri("at://feed/generator/3"),
            )

            val updates = listOf(
                bulkUpdate,
                pinUpdate,
                saveUpdate,
                removeUpdate,
            )

            return SerializationTestHelper.Format.entries.flatMap { format ->
                updates.map { update -> Arguments.of(format, update) }
            }
        }
    }
}
