package com.tunjid.heron.models.polymorphic.timeline

import app.cash.burst.Burst
import app.cash.burst.burstValues
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.Test
import kotlin.test.assertEquals

@Burst
class TimelinePresentationSerializationTest(
    val format: SerializationTestHelper.Format = burstValues(
        SerializationTestHelper.Format.CBOR,
        SerializationTestHelper.Format.PROTOBUF,
    ),
    val original: Timeline.Presentation = burstValues(
        Timeline.Presentation.Text.WithEmbed,
        Timeline.Presentation.Media.Expanded,
        Timeline.Presentation.Media.Condensed,
        Timeline.Presentation.Media.Grid,
    ),
) {
    @Test
    fun roundTrip() {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Timeline.Presentation.serializer(),
        )
        assertEquals(original, decoded)
    }
}
