package com.tunjid.heron.fakes

import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.FeedReqId
import com.tunjid.heron.data.core.types.PostUri

fun sampleFeedInteraction(
    event: FeedGenerator.Interaction.Event = FeedGenerator.Interaction.Event.Engagement.Seen,
): FeedGenerator.Interaction {
    val feedUri = FeedGeneratorUri("at://feed/generator/1")
    val postUri = PostUri("at://post/123")
    val feedContext = "ctx-abc"
    val reqId = FeedReqId("req-123")
    return when (event) {
        is FeedGenerator.Interaction.Event.Request -> FeedGenerator.Interaction.Request(
            feedUri = feedUri,
            postUri = postUri,
            event = event,
            feedContext = feedContext,
            reqId = reqId,
        )
        is FeedGenerator.Interaction.Event.Click -> FeedGenerator.Interaction.Click(
            feedUri = feedUri,
            postUri = postUri,
            event = event,
            feedContext = feedContext,
            reqId = reqId,
        )
        is FeedGenerator.Interaction.Event.Engagement -> FeedGenerator.Interaction.Engagement(
            feedUri = feedUri,
            postUri = postUri,
            event = event,
            feedContext = feedContext,
            reqId = reqId,
        )
    }
}
