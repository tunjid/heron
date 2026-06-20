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

package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.MessageId
import com.tunjid.heron.data.core.types.ProfileId
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: MessageId,
    val conversationId: ConversationId,
    val text: String,
    // Null for system messages (group member joins/leaves, edits, etc.) which have no sender.
    val sender: Profile?,
    val isDeleted: Boolean,
    val sentAt: Instant,
    val embeddedRecord: Record.Embeddable?,
    val reactions: List<Reaction>,
    val metadata: Metadata?,
    // Non-null when this is a group system message rather than a user authored message.
    val system: SystemContent? = null,
) {
    @Serializable
    data class Metadata(
        val links: List<Link>,
    ) : UrlEncodableModel

    /**
     * A system generated message describing a change to a group conversation
     * (members joining/leaving, the group being renamed or locked, etc.). Referred
     * users carry their display info, baked in at save time, so rendering needs no
     * read time profile resolution.
     */
    @Serializable
    sealed class SystemContent : UrlEncodableModel {

        @Serializable
        data class Actor(
            val did: ProfileId,
            val displayName: String?,
            val handle: String?,
        )

        @Serializable
        data class MemberAdded(
            val member: Actor,
            val addedBy: Actor,
        ) : SystemContent()

        @Serializable
        data class MemberRemoved(
            val member: Actor,
            val removedBy: Actor,
        ) : SystemContent()

        @Serializable
        data class MemberJoined(
            val member: Actor,
            val approvedBy: Actor?,
        ) : SystemContent()

        @Serializable
        data class MemberLeft(
            val member: Actor,
        ) : SystemContent()

        @Serializable
        data class GroupRenamed(
            val oldName: String?,
            val newName: String?,
        ) : SystemContent()

        @Serializable
        data class Locked(
            val by: Actor,
        ) : SystemContent()

        @Serializable
        data class Unlocked(
            val by: Actor,
        ) : SystemContent()

        @Serializable
        data class LockedPermanently(
            val by: Actor,
        ) : SystemContent()

        @Serializable
        data object JoinLinkChanged : SystemContent()

        @Serializable
        data object Unknown : SystemContent()
    }

    @Serializable
    data class Create(
        val conversationId: ConversationId,
        val text: String,
        val links: List<Link>,
        val recordReference: Record.Reference?,
    )

    @Serializable
    data class Reaction(
        val value: String,
        val senderId: ProfileId,
        val createdAt: Instant,
    )

    @Serializable
    sealed class UpdateReaction {
        abstract val convoId: ConversationId
        abstract val messageId: MessageId
        abstract val value: String

        @Serializable
        data class Add(
            override val value: String,
            override val messageId: MessageId,
            override val convoId: ConversationId,
        ) : UpdateReaction()

        @Serializable
        data class Remove(
            override val value: String,
            override val messageId: MessageId,
            override val convoId: ConversationId,
        ) : UpdateReaction()
    }
}
