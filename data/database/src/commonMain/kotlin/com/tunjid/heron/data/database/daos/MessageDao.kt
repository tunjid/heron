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

package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tunjid.heron.data.core.types.MessageId
import com.tunjid.heron.data.database.entities.ConversationEntity
import com.tunjid.heron.data.database.entities.ConversationMembersEntity
import com.tunjid.heron.data.database.entities.MessageEntity
import com.tunjid.heron.data.database.entities.MessageReactionEntity
import com.tunjid.heron.data.database.entities.PopulatedConversationEntity
import com.tunjid.heron.data.database.entities.PopulatedMessageEntity
import com.tunjid.heron.data.database.entities.messageembeds.MessageFeedGeneratorEntity
import com.tunjid.heron.data.database.entities.messageembeds.MessageListEntity
import com.tunjid.heron.data.database.entities.messageembeds.MessagePostEntity
import com.tunjid.heron.data.database.entities.messageembeds.MessageStarterPackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query(
        """
            SELECT conversations.*,
                lastMessage.id AS lastMessage_id,
                lastMessage.rev AS lastMessage_rev,
                lastMessage.text AS lastMessage_text,
                lastMessage.senderId AS lastMessage_senderId,
                lastMessage.conversationId AS lastMessage_conversationId,
                lastMessage.conversationOwnerId AS lastMessage_conversationOwnerId,
                lastMessage.isDeleted AS lastMessage_isDeleted,
                lastMessage.sentAt AS lastMessage_sentAt,
                lastMessageReactedTo.id AS lastMessageReactedTo_id,
                lastMessageReactedTo.rev AS lastMessageReactedTo_rev,
                lastMessageReactedTo.text AS lastMessageReactedTo_text,
                lastMessageReactedTo.senderId AS lastMessageReactedTo_senderId,
                lastMessageReactedTo.conversationId AS lastMessageReactedTo_conversationId,
                lastMessageReactedTo.conversationOwnerId AS lastMessageReactedTo_conversationOwnerId,
                lastMessageReactedTo.isDeleted AS lastMessageReactedTo_isDeleted,
                lastMessageReactedTo.sentAt AS lastMessageReactedTo_sentAt,
                lastReaction.messageId AS lastReaction_messageId,
                lastReaction.value AS lastReaction_value,
                lastReaction.senderId AS lastReaction_senderId,
                lastReaction.createdAt AS lastReaction_createdAt,
                MAX(COALESCE(lastReaction.createdAt, lastMessage.sentAt), lastMessage.sentAt) AS sort
                FROM conversations
            INNER JOIN messages AS lastMessage
            ON lastMessageId = lastMessage.id
            LEFT JOIN messages AS lastMessageReactedTo
            ON lastReactedToMessageId = lastMessageReactedTo.id
            LEFT JOIN (
                SELECT * FROM messageReactions AS lastReaction
                ORDER BY createdAt
                DESC
                LIMIT 1
            ) AS lastReaction
            ON lastReactedToMessageId = lastReaction.messageId
            WHERE ownerId = :ownerId
            ORDER BY sort
            DESC
            LIMIT :limit
            OFFSET :offset
        """
    )
    fun conversations(
        ownerId: String,
        limit: Long,
        offset: Long,
    ): Flow<List<PopulatedConversationEntity>>

    @Query(
        """
            SELECT * FROM messages
            WHERE conversationId = :conversationId
            AND conversationOwnerId = :conversationOwnerId
            ORDER BY sentAt
            DESC
            LIMIT :limit
            OFFSET :offset
        """
    )
    fun messages(
        conversationId: String,
        conversationOwnerId: String,
        limit: Long,
        offset: Long,
    ): Flow<List<PopulatedMessageEntity>>

    @Upsert suspend fun upsertConversations(entities: List<ConversationEntity>)

    @Upsert suspend fun upsertConversationMembers(entities: List<ConversationMembersEntity>)

    @Upsert suspend fun upsertMessages(entities: List<MessageEntity>)

    @Upsert suspend fun upsertMessageReactions(entities: List<MessageReactionEntity>)

    @Upsert suspend fun upsertMessageFeeds(entities: List<MessageFeedGeneratorEntity>)

    @Upsert suspend fun upsertMessageLists(entities: List<MessageListEntity>)

    @Upsert suspend fun upsertMessageStarterPacks(entities: List<MessageStarterPackEntity>)

    @Upsert suspend fun upsertMessagePosts(entities: List<MessagePostEntity>)

    @Query(
        """
        DELETE FROM conversations
    """
    )
    suspend fun deleteAllConversations()

    @Query(
        """
        DELETE FROM messages
        WHERE conversationId = :conversationId
    """
    )
    suspend fun deleteAllMessages(conversationId: String)

    @Query(
        """
        DELETE FROM messageReactions
        WHERE messageId in (:messageIds)
    """
    )
    suspend fun deleteMessageReactions(messageIds: Collection<MessageId>)
}
