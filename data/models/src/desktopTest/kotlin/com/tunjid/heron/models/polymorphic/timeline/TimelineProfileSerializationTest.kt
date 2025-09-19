package com.tunjid.heron.models.polymorphic.timeline

import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.fakes.FakeTimeline
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class TimelineProfileSerializationTest {

    @ParameterizedTest(name = "[{index}] {1} can be serialized with {0}")
    @MethodSource("timelineProfileCases")
    fun `round trip Timeline Profile`(
        format: SerializationTestHelper.Format,
        original: Timeline.Profile,
    ) {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Timeline.Profile.serializer(),
        )
        assertEquals(original, decoded)
    }

    companion object {
        @JvmStatic
        private fun timelineProfileCases(): List<Arguments> {
            val profileTypes = Timeline.Profile.Type.entries.toTypedArray()
            val profiles = profileTypes.map { type ->
                Timeline.Profile(
                    profileId = FakeTimeline.profileTimeline.profileId,
                    type = type,
                    lastRefreshed = FakeTimeline.profileTimeline.lastRefreshed,
                    presentation = FakeTimeline.profileTimeline.presentation,
                )
            }
            return SerializationTestHelper.Format.entries.flatMap { format ->
                profiles.map { profile -> Arguments.of(format, profile) }
            }
        }
    }
}
