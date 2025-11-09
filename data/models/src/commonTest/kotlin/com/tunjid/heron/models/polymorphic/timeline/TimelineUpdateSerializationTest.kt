package com.tunjid.heron.models.polymorphic.timeline

import app.cash.burst.Burst
import app.cash.burst.burstValues
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.fakes.FakeTimeline
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.Test
import kotlin.test.assertEquals

@Burst
class TimelineUpdateSerializationTest(
    val format: SerializationTestHelper.Format = burstValues(
        SerializationTestHelper.Format.CBOR,
        SerializationTestHelper.Format.PROTOBUF,
    ),
    val original: Timeline.Update = burstValues(
        Timeline.Update.OfFeedGenerator.Bulk(
            timelines = listOf(FakeTimeline.following),
        ),
        Timeline.Update.OfFeedGenerator.Pin(
            uri = FeedGeneratorUri("at://feed/generator/1"),
        ),
        Timeline.Update.OfFeedGenerator.Save(
            uri = FeedGeneratorUri("at://feed/generator/2"),
        ),
        Timeline.Update.OfFeedGenerator.Remove(
            uri = FeedGeneratorUri("at://feed/generator/3"),
        ),
    ),
) {
    @Test
    fun roundTrip() {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Timeline.Update.serializer(),
        )
        assertEquals(original, decoded)
    }
}
