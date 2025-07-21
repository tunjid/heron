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
import kotlinx.datetime.Instant

internal fun MultipleEntitySaver.add(
    viewingProfileId: ProfileId?,
    convoView: ConvoView,
) {
    convoView.members.forEach { member ->
        add(
            viewingProfileId = viewingProfileId,
            profileView = ProfileViewBasic(
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
            entity = ConversationMembersEntity(
                conversationId = convoView.id.let(::ConversationId),
                memberId = member.did.did.let(::ProfileId),
            )
        )
    }
    val lastMessage = when (val lastMessage = convoView.lastMessage) {
        is ConvoViewLastMessageUnion.DeletedMessageView,
        is ConvoViewLastMessageUnion.Unknown,
        null -> null

        is ConvoViewLastMessageUnion.MessageView -> lastMessage.value
    }?.also {
        add(
            conversationId = convoView.id.let(::ConversationId),
            messageView = it,
        )
    }

    val lastReactedToMessage = when (val lastReaction = convoView.lastReaction) {
        is ConvoViewLastReactionUnion.Unknown,
        null -> null

        is ConvoViewLastReactionUnion.MessageAndReactionView -> {
            // TODO: save reaction
            lastReaction.value.message

        }
    }?.also {
        add(
            conversationId = convoView.id.let(::ConversationId),
            messageView = it,
        )
    }

    add(
        ConversationEntity(
            id = convoView.id.let(::ConversationId),
            rev = convoView.rev,
            lastMessageId = lastMessage?.id?.let(::MessageId),
            lastReactedToMessageId = lastReactedToMessage?.id?.let(::MessageId),
            muted = convoView.muted,
            status = convoView.status?.value,
            unreadCount = convoView.unreadCount,
        )
    )
}