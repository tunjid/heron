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

package com.tunjid.heron.data.repository

import chat.bsky.convo.GetMessagesQueryParams
import chat.bsky.convo.GetMessagesResponse
import chat.bsky.convo.GetMessagesResponseMessageUnion
import chat.bsky.convo.ListConvosQueryParams
import chat.bsky.convo.ListConvosResponse
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.database.daos.MessageDao
import com.tunjid.heron.data.database.entities.PopulatedConversationEntity
import com.tunjid.heron.data.database.entities.PopulatedMessageEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.nextCursorFlow
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class ConversationQuery(
    override val data: CursorQuery.Data,
) : CursorQuery

@Serializable
data class MessageQuery(
    val conversationId: ConversationId,
    override val data: CursorQuery.Data,
) : CursorQuery


interface MessageRepository {

    fun conversations(
        query: ConversationQuery,
        cursor: Cursor,
    ): Flow<CursorList<Conversation>>

    fun messages(
        query: MessageQuery,
        cursor: Cursor,
    ): Flow<CursorList<Message>>

}

internal class OfflineMessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val savedStateRepository: SavedStateRepository,
) : MessageRepository {

    override fun conversations(
        query: ConversationQuery,
        cursor: Cursor
    ): Flow<CursorList<Conversation>> =
        combine(
            messageDao.conversations(
                offset = query.data.offset,
                limit = query.data.limit,
            )
                .map { populatedConversationEntities ->
                    populatedConversationEntities.map(PopulatedConversationEntity::asExternalModel)
                },
            nextCursorFlow(
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    networkService.api.listConvos(
                        params = ListConvosQueryParams(
                            limit = query.data.limit,
                            cursor = cursor.value,
                        )
                    )
                },
                nextCursor = ListConvosResponse::cursor,
                onResponse = {
                    val signedInProfileId = savedStateRepository.signedInProfileId

                    multipleEntitySaverProvider.saveInTransaction {
                        convos.forEach {
                            add(
                                viewingProfileId = signedInProfileId,
                                convoView = it,
                            )
                        }
                    }
                },
            ),
            ::CursorList
        )
            .distinctUntilChanged()

    override fun messages(
        query: MessageQuery,
        cursor: Cursor
    ): Flow<CursorList<Message>> =
        combine(
            messageDao.messages(
                conversationId = query.conversationId.id,
                before = query.data.cursorAnchor,
                offset = query.data.offset,
                limit = query.data.limit,
            )
                .map { populatedMessageEntities ->
                    populatedMessageEntities.map(PopulatedMessageEntity::asExternalModel)
                },
            nextCursorFlow(
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    networkService.api.getMessages(
                        params = GetMessagesQueryParams(
                            convoId = query.conversationId.id,
                            limit = query.data.limit,
                            cursor = cursor.value,
                        )
                    )
                },
                nextCursor = GetMessagesResponse::cursor,
                onResponse = {
                    multipleEntitySaverProvider.saveInTransaction {
                        messages.forEach {
                            when (it) {
                                is GetMessagesResponseMessageUnion.DeletedMessageView -> add(
                                    conversationId = query.conversationId,
                                    deletedMessageView = it.value,
                                )

                                is GetMessagesResponseMessageUnion.MessageView -> add(
                                    conversationId = query.conversationId,
                                    messageView = it.value,
                                )

                                is GetMessagesResponseMessageUnion.Unknown -> Unit
                            }
                        }
                    }
                },
            ),
            ::CursorList
        )
            .distinctUntilChanged()
}
