/*
 *    Copyright 2026 Adetunji Dahunsi
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

package com.tunjid.heron.conversation

import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.MessageId
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import kotlin.time.Instant

/**
 * Sample data for the conversation [@Preview][androidx.compose.ui.tooling.preview.Preview]
 * composables in this module.
 */
internal object PreviewFixtures {

    val timestamp: Instant = Instant.parse("2024-01-01T09:41:00Z")

    val me: Profile = profile(displayName = "Me", id = "me")

    val them: Profile = profile(displayName = "Richard Carleton", id = "richardcarleton")

    private val thumbsUp = Message.Reaction(
        value = "👍",
        senderId = them.did,
        createdAt = timestamp,
    )

    val designReviewQuestion: MessageItem.Sent = sentMessage(
        id = "1",
        sender = them,
        text = "Hey! Are we still on for the design review later today?",
    )

    val confirmation: MessageItem.Sent = sentMessage(
        id = "2",
        sender = me,
        text = "Yes — 3pm works for me",
        reactions = listOf(thumbsUp),
    )

    val figmaFollowUp: MessageItem.Sent = sentMessage(
        id = "3",
        sender = them,
        text = "Perfect, I'll share the Figma link in a moment",
    )

    fun profile(
        displayName: String,
        id: String,
    ): Profile = Profile(
        did = ProfileId(id),
        handle = ProfileHandle("$id.bsky.social"),
        displayName = displayName,
        description = null,
        avatar = null,
        banner = null,
        followersCount = 0,
        followsCount = 0,
        postsCount = 0,
        joinedViaStarterPack = null,
        indexedAt = timestamp,
        createdAt = timestamp,
        metadata = Profile.Metadata(
            createdListCount = 0,
            createdFeedGeneratorCount = 0,
            createdStarterPackCount = 0,
            chat = Profile.ChatInfo(
                allowed = Profile.ChatInfo.Allowed.NoOne,
            ),
        ),
    )

    fun sentMessage(
        id: String,
        sender: Profile,
        text: String,
        reactions: List<Message.Reaction> = emptyList(),
    ): MessageItem.Sent = MessageItem.Sent(
        message = Message(
            id = MessageId(id),
            conversationId = ConversationId("convo-preview"),
            text = text,
            sender = sender,
            isDeleted = false,
            sentAt = timestamp,
            embeddedRecord = null,
            reactions = reactions,
            metadata = null,
        ),
    )
}
