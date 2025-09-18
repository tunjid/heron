package com.tunjid.heron.models.polymorphic.timeline

import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class TimelinePresentationSerializationTest {

    @ParameterizedTest(name = "[{index}] {1} can be serialized with {0}")
    @MethodSource("presentationCases")
    fun `round trip Timeline Presentation`(
        format: SerializationTestHelper.Format,
        original: Timeline.Presentation,
    ) {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Timeline.Presentation.serializer(),
        )
        assertEquals(original, decoded)
    }

    companion object {
        @JvmStatic
        private fun presentationCases(): List<Arguments> {
            val presentations = listOf(
                Timeline.Presentation.Text.WithEmbed,
                Timeline.Presentation.Media.Expanded,
                Timeline.Presentation.Media.Condensed,
                Timeline.Presentation.Media.Grid,
            )
            return SerializationTestHelper.Format.entries.flatMap { format ->
                presentations.map { pres -> Arguments.of(format, pres) }
            }
        }
    }
}
