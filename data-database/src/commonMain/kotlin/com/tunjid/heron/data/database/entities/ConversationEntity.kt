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

package com.tunjid.heron.data.database.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.MessageId


@Entity(
    tableName = "conversations",
)
data class ConversationEntity(
    @PrimaryKey
    val id: ConversationId,
    val rev: String,
    val lastMessageId: MessageId? = null,
    val lastReactedToMessageId: MessageId? = null,
    val muted: Boolean,
    val status: String? = null,
    val unreadCount: Long,
)

data class PopulatedConversationEntity(
    @Embedded
    val entity: ConversationEntity,
    @Embedded(prefix = "lastMessage_")
    val lastMessageEntity: MessageEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "did",
        associateBy = Junction(
            value = ConversationMembersEntity::class,
            parentColumn = "conversationId",
            entityColumn = "memberId",
        ),
    )
    val memberEntities: List<ProfileEntity>,
)

fun PopulatedConversationEntity.asExternalModel() =
    Conversation(
        id = entity.id,
        muted = entity.muted,
        unreadCount = entity.unreadCount,
        members = memberEntities.map(ProfileEntity::asExternalModel),
        lastMessage = lastMessageEntity?.let { message ->
            memberEntities.firstOrNull {
                it.did == message.senderId
            }
                ?.let { sender ->
                    Message(
                        id = message.id,
                        conversationId = message.conversationId,
                        text = message.text,
                        sentAt = message.sentAt,
                        isDeleted = message.isDeleted,
                        sender = sender.asExternalModel(),
                        feedGenerator = null,
                        list = null,
                        starterPack = null,
                        post = null,
                    )
                }
        },
    )