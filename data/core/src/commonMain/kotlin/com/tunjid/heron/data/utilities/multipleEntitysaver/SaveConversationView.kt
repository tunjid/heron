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

package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.actor.ProfileViewBasic
import chat.bsky.convo.ConvoView
import chat.bsky.convo.ConvoViewLastMessageUnion
import chat.bsky.convo.ConvoViewLastReactionUnion
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.MessageId
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.ConversationEntity
import com.tunjid.heron.data.database.entities.ConversationMembersEntity
import com.tunjid.heron.data.database.entities.MessageReactionEntity
import kotlin.time.Instant

internal fun MultipleEntitySaver.add(viewingProfileId: ProfileId?, convoView: ConvoView) {
    viewingProfileId ?: return
    convoView.members.forEach { member ->
        add(
            viewingProfileId = viewingProfileId,
            profileView =
                ProfileViewBasic(
                    did = member.did,
                    handle = member.handle,
                    displayName = member.displayName,
                    avatar = member.avatar,
                    associated = member.associated,
                    viewer = member.viewer,
                    labels = member.labels,
                    createdAt = Instant.DISTANT_PAST,
                    verification = member.verification,
                ),
        )
        add(
            entity =
                ConversationMembersEntity(
                    conversationId = convoView.id.let(::ConversationId),
                    conversationOwnerId = viewingProfileId,
                    memberId = member.did.did.let(::ProfileId),
                )
        )
    }
    val lastMessageId =
        when (val lastMessage = convoView.lastMessage) {
            is ConvoViewLastMessageUnion.Unknown,
            null -> null
            is ConvoViewLastMessageUnion.DeletedMessageView -> {
                add(
                    conversationId = convoView.id.let(::ConversationId),
                    viewingProfileId = viewingProfileId,
                    deletedMessageView = lastMessage.value,
                )
                lastMessage.value.id.let(::MessageId)
            }

            is ConvoViewLastMessageUnion.MessageView -> {
                add(
                    viewingProfileId = viewingProfileId,
                    conversationId = convoView.id.let(::ConversationId),
                    messageView = lastMessage.value,
                )
                lastMessage.value.id.let(::MessageId)
            }
        }

    val lastReactedToMessageId =
        when (val lastReaction = convoView.lastReaction) {
            is ConvoViewLastReactionUnion.Unknown,
            null -> null

            is ConvoViewLastReactionUnion.MessageAndReactionView -> {
                add(
                    viewingProfileId = viewingProfileId,
                    conversationId = convoView.id.let(::ConversationId),
                    messageView = lastReaction.value.message,
                )
                add(
                    MessageReactionEntity(
                        value = lastReaction.value.reaction.value,
                        messageId = lastReaction.value.message.id.let(::MessageId),
                        senderId = lastReaction.value.reaction.sender.did.did.let(::ProfileId),
                        createdAt = lastReaction.value.reaction.createdAt,
                    )
                )
                lastReaction.value.message.id.let(::MessageId)
            }
        }

    add(
        ConversationEntity(
            id = convoView.id.let(::ConversationId),
            rev = convoView.rev,
            ownerId = viewingProfileId,
            lastMessageId = lastMessageId,
            lastReactedToMessageId = lastReactedToMessageId,
            muted = convoView.muted,
            status = convoView.status?.value,
            unreadCount = convoView.unreadCount,
        )
    )
}
