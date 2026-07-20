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

import app.bsky.feed.Interaction as BskyInteraction
import app.bsky.feed.Token
import com.tunjid.heron.data.core.models.FeedGenerator
import sh.christian.ozone.api.AtUri

/**
 * Maps a Heron [FeedGenerator.Interaction.Event] to the generated `app.bsky.feed.Token` it
 * corresponds to. Kept in the data layer so the domain model stays free of lexicon types.
 */
internal fun FeedGenerator.Interaction.Event.toToken(): Token =
    when (this) {
        FeedGenerator.Interaction.Event.Request.Less -> Token.RequestLess
        FeedGenerator.Interaction.Event.Request.More -> Token.RequestMore
        FeedGenerator.Interaction.Event.Click.Item -> Token.ClickthroughItem
        FeedGenerator.Interaction.Event.Click.Author -> Token.ClickthroughAuthor
        FeedGenerator.Interaction.Event.Click.Reposter -> Token.ClickthroughReposter
        FeedGenerator.Interaction.Event.Click.Embed -> Token.ClickthroughEmbed
        FeedGenerator.Interaction.Event.Engagement.Seen -> Token.InteractionSeen
        FeedGenerator.Interaction.Event.Engagement.Like -> Token.InteractionLike
        FeedGenerator.Interaction.Event.Engagement.Repost -> Token.InteractionRepost
        FeedGenerator.Interaction.Event.Engagement.Reply -> Token.InteractionReply
        FeedGenerator.Interaction.Event.Engagement.Quote -> Token.InteractionQuote
        FeedGenerator.Interaction.Event.Engagement.Share -> Token.InteractionShare
    }

internal fun FeedGenerator.Interaction.asNetworkInteraction(): BskyInteraction =
    BskyInteraction(
        item = AtUri(postUri.uri),
        event = event.toToken().value,
        feedContext = feedContext,
        reqId = reqId?.value,
    )
