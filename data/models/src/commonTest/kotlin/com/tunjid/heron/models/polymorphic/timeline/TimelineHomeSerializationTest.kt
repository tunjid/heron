package com.tunjid.heron.models.polymorphic.timeline

import app.cash.burst.Burst
import app.cash.burst.burstValues
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.fakes.FakeTimeline
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.Test
import kotlin.test.assertEquals

@Burst
class TimelineHomeSerializationTest(
    val format: SerializationTestHelper.Format =
        burstValues(SerializationTestHelper.Format.CBOR, SerializationTestHelper.Format.PROTOBUF),
    val original: Timeline.Home =
        burstValues(FakeTimeline.following, FakeTimeline.listTimeline, FakeTimeline.feedTimeline),
) {
    @Test
    fun roundTrip() {
        val decoded =
            SerializationTestHelper.roundTrip(
                format = format,
                value = original,
                serializer = Timeline.Home.serializer(),
            )
        assertEquals(original, decoded)
    }
}
