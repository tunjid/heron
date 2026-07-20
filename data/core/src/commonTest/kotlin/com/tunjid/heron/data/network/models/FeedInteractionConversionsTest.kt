/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.data.network.models

import app.bsky.feed.Token
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.FeedReqId
import com.tunjid.heron.data.core.types.PostUri
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedInteractionConversionsTest {

    @Test
    fun event_mapsToExpectedToken() {
        val expected = mapOf(
            FeedGenerator.Interaction.Event.Request.Less to Token.RequestLess,
            FeedGenerator.Interaction.Event.Request.More to Token.RequestMore,
            FeedGenerator.Interaction.Event.Click.Item to Token.ClickthroughItem,
            FeedGenerator.Interaction.Event.Click.Author to Token.ClickthroughAuthor,
            FeedGenerator.Interaction.Event.Click.Reposter to Token.ClickthroughReposter,
            FeedGenerator.Interaction.Event.Click.Embed to Token.ClickthroughEmbed,
            FeedGenerator.Interaction.Event.Engagement.Seen to Token.InteractionSeen,
            FeedGenerator.Interaction.Event.Engagement.Like to Token.InteractionLike,
            FeedGenerator.Interaction.Event.Engagement.Repost to Token.InteractionRepost,
            FeedGenerator.Interaction.Event.Engagement.Reply to Token.InteractionReply,
            FeedGenerator.Interaction.Event.Engagement.Quote to Token.InteractionQuote,
            FeedGenerator.Interaction.Event.Engagement.Share to Token.InteractionShare,
        )

        expected.forEach { (event, token) ->
            assertEquals(
                expected = token,
                actual = event.toToken(),
            )
        }
    }

    @Test
    fun asNetworkInteraction_echoesItemContextAndReqId() {
        val interaction = FeedGenerator.Interaction.Engagement(
            feedUri = FeedGeneratorUri("at://feed/generator/1"),
            postUri = PostUri("at://post/123"),
            event = FeedGenerator.Interaction.Event.Engagement.Like,
            feedContext = "ctx-abc",
            reqId = FeedReqId("req-123"),
        )

        val network = interaction.asNetworkInteraction()

        assertEquals(
            expected = "at://post/123",
            actual = network.item?.atUri,
        )
        assertEquals(
            expected = Token.InteractionLike.value,
            actual = network.event,
        )
        assertEquals(
            expected = "ctx-abc",
            actual = network.feedContext,
        )
        assertEquals(
            expected = "req-123",
            actual = network.reqId,
        )
    }
}
