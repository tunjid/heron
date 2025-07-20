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
import com.tunjid.heron.data.database.entities.ConversationEntity
import com.tunjid.heron.data.database.entities.MessageEntity

@Dao
interface MessageDao {

    @Upsert
    suspend fun upsertConversations(
        entities: List<ConversationEntity>,
    )

    @Upsert
    suspend fun upsertMessages(
        entities: List<MessageEntity>,
    )

    @Query(
        """
        DELETE FROM conversations
    """
    )
    suspend fun deleteAllConversations(
        sourceId: String,
    )

    @Query(
        """
        DELETE FROM messages
        WHERE conversationId = :conversationId
    """
    )
    suspend fun deleteAllMessages(
        conversationId: String,
    )
}
