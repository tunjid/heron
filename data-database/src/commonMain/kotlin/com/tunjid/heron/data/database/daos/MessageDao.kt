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
                lastMessage.isDeleted AS lastMessage_isDeleted,
                lastMessage.sentAt AS lastMessage_sentAt,
                lastMessageReactedTo.sentAt FROM conversations
            LEFT JOIN messages AS lastMessage
            ON lastMessageId = lastMessage.id
            LEFT JOIN messages AS lastMessageReactedTo
            ON lastMessageId = lastMessageReactedTo.id
            ORDER BY COALESCE(
                lastMessageReactedTo.sentAt,
                lastMessage.sentAt
            )
            DESC
            LIMIT :limit
            OFFSET :offset
        """
    )
    fun conversations(
        limit: Long,
        offset: Long,
    ): Flow<List<PopulatedConversationEntity>>

    @Query(
        """
            SELECT * FROM messages
            WHERE conversationId = :conversationId
            ORDER BY sentAt
            DESC
            LIMIT :limit
            OFFSET :offset
        """
    )
    fun messages(
        conversationId: String,
        limit: Long,
        offset: Long,
    ): Flow<List<PopulatedMessageEntity>>

    @Upsert
    suspend fun upsertConversations(
        entities: List<ConversationEntity>,
    )

    @Upsert
    suspend fun upsertConversationMembers(
        entities: List<ConversationMembersEntity>,
    )

    @Upsert
    suspend fun upsertMessages(
        entities: List<MessageEntity>,
    )

    @Upsert
    suspend fun upsertMessageReactions(
        entities: List<MessageReactionEntity>,
    )

    @Upsert
    suspend fun upsertMessageFeeds(
        entities: List<MessageFeedGeneratorEntity>,
    )

    @Upsert
    suspend fun upsertMessageLists(
        entities: List<MessageListEntity>,
    )

    @Upsert
    suspend fun upsertMessageStarterPacks(
        entities: List<MessageStarterPackEntity>,
    )

    @Upsert
    suspend fun upsertMessagePosts(
        entities: List<MessagePostEntity>,
    )
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
    suspend fun deleteAllMessages(
        conversationId: String,
    )

    @Query(
        """
        DELETE FROM messageReactions
        WHERE messageId in (:messageIds)
    """
    )
    suspend fun deleteMessageReactions(
        messageIds: Collection<MessageId>,
    )
}
