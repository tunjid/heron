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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.MessageId
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import kotlin.time.Instant

private val PreviewTimestamp = Instant.parse("2024-01-01T09:41:00Z")

private fun previewProfile(
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
    indexedAt = PreviewTimestamp,
    createdAt = PreviewTimestamp,
    metadata = Profile.Metadata(
        createdListCount = 0,
        createdFeedGeneratorCount = 0,
        createdStarterPackCount = 0,
        chat = Profile.ChatInfo(
            allowed = Profile.ChatInfo.Allowed.NoOne,
        ),
    ),
)

private fun sentMessage(
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
        sentAt = PreviewTimestamp,
        embeddedRecord = null,
        reactions = reactions,
        metadata = null,
    ),
)

/**
 * Reproduces the per message [Row] layout from [ConversationScreen]'s `Message`
 * composable, swapping the shared element avatar for a static placeholder so the
 * stateless [AuthorAndTextMessage] can be previewed without a `PaneScaffoldState`.
 */
@Composable
private fun PreviewMessageRow(
    item: MessageItem,
    side: Side,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .padding(
                top = if (isLastMessageByAuthor) 8.dp else 0.dp,
                start = 16.dp,
                end = 16.dp,
            )
            .fillMaxWidth(),
        horizontalArrangement = side,
    ) {
        if (isLastMessageByAuthor) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .align(Alignment.Top),
            )
        }
        Spacer(
            modifier = Modifier
                .width(if (isLastMessageByAuthor) 16.dp else 34.dp),
        )
        AuthorAndTextMessage(
            item = item,
            side = side,
            isFirstMessageByAuthor = isFirstMessageByAuthor,
            isLastMessageByAuthor = isLastMessageByAuthor,
            onMessageLongPressed = {},
            onLinkTargetClicked = {},
        )
    }
}

@Composable
private fun PreviewConversation() {
    val me = previewProfile(displayName = "Me", id = "me")
    val them = previewProfile(displayName = "Joel Muraguri-Wanjiku", id = "joel")
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                PreviewMessageRow(
                    item = sentMessage(
                        id = "1",
                        sender = them,
                        text = "Hey! Are we still on for the design review later today?",
                    ),
                    side = Side.Receiver,
                    isFirstMessageByAuthor = true,
                    isLastMessageByAuthor = true,
                )
                PreviewMessageRow(
                    item = sentMessage(
                        id = "2",
                        sender = me,
                        text = "Yes — 3pm works for me",
                        reactions = listOf(
                            Message.Reaction(
                                value = "👍",
                                senderId = ProfileId("joel"),
                                createdAt = PreviewTimestamp,
                            ),
                        ),
                    ),
                    side = Side.Sender,
                    isFirstMessageByAuthor = true,
                    isLastMessageByAuthor = true,
                )
                PreviewMessageRow(
                    item = sentMessage(
                        id = "3",
                        sender = them,
                        text = "Perfect, I'll share the Figma link in a moment",
                    ),
                    side = Side.Receiver,
                    isFirstMessageByAuthor = true,
                    isLastMessageByAuthor = true,
                )
            }
        }
    }
}

/**
 * Forces a system font scale by overriding [LocalDensity]'s `fontScale`, which the
 * Skia/Desktop preview renderer honours as ordinary composition. The Desktop
 * pipeline does not apply the `@Preview(fontScale = …)` parameter, so we drive it
 * here instead to exercise accessibility font scaling.
 */
@Composable
private fun FontScaled(
    fontScale: Float,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = fontScale,
        ),
        content = content,
    )
}

@Preview(name = "Direct messages - font 1.0x", widthDp = 360)
@Composable
internal fun ConversationFontScale100Preview() {
    FontScaled(fontScale = 1f) { PreviewConversation() }
}

@Preview(name = "Direct messages - font 1.5x", widthDp = 360)
@Composable
internal fun ConversationFontScale150Preview() {
    FontScaled(fontScale = 1.5f) { PreviewConversation() }
}

@Preview(name = "Direct messages - font 2.0x", widthDp = 360)
@Composable
internal fun ConversationFontScale200Preview() {
    FontScaled(fontScale = 2f) { PreviewConversation() }
}
