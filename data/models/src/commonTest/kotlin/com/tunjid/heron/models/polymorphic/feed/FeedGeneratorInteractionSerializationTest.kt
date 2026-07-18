package com.tunjid.heron.models.polymorphic.feed

import app.cash.burst.Burst
import app.cash.burst.burstValues
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.fakes.sampleFeedInteraction
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.Test
import kotlin.test.assertEquals

@Burst
class FeedGeneratorInteractionSerializationTest(
    val format: SerializationTestHelper.Format = burstValues(
        SerializationTestHelper.Format.CBOR,
        SerializationTestHelper.Format.PROTOBUF,
    ),
    val event: FeedGenerator.Interaction.Event = burstValues(
        FeedGenerator.Interaction.Event.Request.Less,
        FeedGenerator.Interaction.Event.Request.More,
        FeedGenerator.Interaction.Event.Click.Item,
        FeedGenerator.Interaction.Event.Click.Author,
        FeedGenerator.Interaction.Event.Click.Reposter,
        FeedGenerator.Interaction.Event.Click.Embed,
        FeedGenerator.Interaction.Event.Engagement.Seen,
        FeedGenerator.Interaction.Event.Engagement.Like,
        FeedGenerator.Interaction.Event.Engagement.Repost,
        FeedGenerator.Interaction.Event.Engagement.Reply,
        FeedGenerator.Interaction.Event.Engagement.Quote,
        FeedGenerator.Interaction.Event.Engagement.Share,
    ),
) {
    @Test
    fun roundTrip() {
        val original = sampleFeedInteraction(event = event)
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = FeedGenerator.Interaction.serializer(),
        )
        assertEquals(original, decoded)
    }
}
