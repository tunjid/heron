package com.tunjid.heron.models.polymorphic.timeline

import app.cash.burst.Burst
import app.cash.burst.burstValues
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.fakes.FakeTimeline
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.Test
import kotlin.test.assertEquals

@Burst
class TimelineProfileSerializationTest(
    val format: SerializationTestHelper.Format =
        burstValues(SerializationTestHelper.Format.CBOR, SerializationTestHelper.Format.PROTOBUF),
    val original: Timeline.Profile =
        burstValues(
            Timeline.Profile(
                profileId = FakeTimeline.profileTimeline.profileId,
                type = Timeline.Profile.Type.Posts,
                lastRefreshed = FakeTimeline.profileTimeline.lastRefreshed,
                itemsAvailable = FakeTimeline.profileTimeline.itemsAvailable,
                presentation = FakeTimeline.profileTimeline.presentation,
            ),
            Timeline.Profile(
                profileId = FakeTimeline.profileTimeline.profileId,
                type = Timeline.Profile.Type.Replies,
                lastRefreshed = FakeTimeline.profileTimeline.lastRefreshed,
                itemsAvailable = FakeTimeline.profileTimeline.itemsAvailable,
                presentation = FakeTimeline.profileTimeline.presentation,
            ),
            Timeline.Profile(
                profileId = FakeTimeline.profileTimeline.profileId,
                type = Timeline.Profile.Type.Media,
                lastRefreshed = FakeTimeline.profileTimeline.lastRefreshed,
                itemsAvailable = FakeTimeline.profileTimeline.itemsAvailable,
                presentation = FakeTimeline.profileTimeline.presentation,
            ),
        ),
) {
    @Test
    fun roundTrip() {
        val decoded =
            SerializationTestHelper.roundTrip(
                format = format,
                value = original,
                serializer = Timeline.Profile.serializer(),
            )
        assertEquals(original, decoded)
    }
}
