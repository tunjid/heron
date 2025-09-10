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

import chat.bsky.convo.AddReactionRequest
import chat.bsky.convo.AddReactionResponse
import chat.bsky.convo.DeletedMessageView
import chat.bsky.convo.GetLogQueryParams
import chat.bsky.convo.GetLogResponseLogUnion as Log
import chat.bsky.convo.GetMessagesQueryParams
import chat.bsky.convo.GetMessagesResponse
import chat.bsky.convo.GetMessagesResponseMessageUnion
import chat.bsky.convo.ListConvosQueryParams
import chat.bsky.convo.ListConvosResponse
import chat.bsky.convo.LogAddReactionMessageUnion
import chat.bsky.convo.LogCreateMessageMessageUnion
import chat.bsky.convo.LogDeleteMessageMessageUnion
import chat.bsky.convo.LogRemoveReactionMessageUnion
import chat.bsky.convo.MessageInput
import chat.bsky.convo.MessageView
import chat.bsky.convo.RemoveReactionRequest
import chat.bsky.convo.RemoveReactionResponse
import chat.bsky.convo.SendMessageRequest
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Link
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.MessageDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.entities.PopulatedConversationEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.LazyList
import com.tunjid.heron.data.utilities.facet
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.nextCursorFlow
import com.tunjid.heron.data.utilities.toOutcome
import com.tunjid.heron.data.utilities.resolveLinks
import com.tunjid.heron.data.utilities.toFlowOrEmpty
import dev.zacsweers.metro.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
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

    suspend fun monitorConversationLogs()

    suspend fun sendMessage(
        message: Message.Create,
    ): Outcome

    suspend fun updateReaction(
        reaction: Message.UpdateReaction,
    ): Outcome
}

internal class OfflineMessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val postDao: PostDao,
    private val feedDao: FeedGeneratorDao,
    private val profileDao: ProfileDao,
    private val listDao: ListDao,
    private val starterPackDao: StarterPackDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val savedStateDataSource: SavedStateDataSource,
) : MessageRepository {

    override fun conversations(
        query: ConversationQuery,
        cursor: Cursor,
    ): Flow<CursorList<Conversation>> =
        combine(
            messageDao.conversations(
                offset = query.data.offset,
                limit = query.data.limit,
            )
                .map { populatedConversationEntities ->
                    populatedConversationEntities.map(PopulatedConversationEntity::asExternalModel)
                },
            networkService.nextCursorFlow(
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    listConvos(
                        params = ListConvosQueryParams(
                            limit = query.data.limit,
                            cursor = cursor.value,
                        ),
                    )
                },
                nextCursor = ListConvosResponse::cursor,
                onResponse = {
                    val signedInProfileId = savedStateDataSource.signedInProfileId

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
            ::CursorList,
        )
            .distinctUntilChanged()

    override fun messages(
        query: MessageQuery,
        cursor: Cursor,
    ): Flow<CursorList<Message>> =
        savedStateDataSource.observedSignedInProfileId.flatMapLatest { signedInProfileId ->
            combine(
                messageDao.messages(
                    conversationId = query.conversationId.id,
                    offset = query.data.offset,
                    limit = query.data.limit,
                )
                    .distinctUntilChanged()
                    .flatMapLatest { populatedMessageEntities ->
                        val feedUris = populatedMessageEntities.mapNotNull {
                            it.feed?.feedGeneratorUri
                        }
                        val listUris = populatedMessageEntities.mapNotNull {
                            it.list?.listUri
                        }
                        val starterPackUris = populatedMessageEntities.mapNotNull {
                            it.starterPack?.starterPackUri
                        }
                        val postUris = populatedMessageEntities.mapNotNull {
                            it.post?.postUri
                        }
                        combine(
                            flow = feedUris.toFlowOrEmpty(feedDao::feedGenerators),
                            flow2 = listUris.toFlowOrEmpty(listDao::lists),
                            flow3 = starterPackUris.toFlowOrEmpty(starterPackDao::starterPacks),
                            flow4 = postUris.toFlowOrEmpty { postIds ->
                                postDao.posts(
                                    viewingProfileId = signedInProfileId?.id,
                                    postUris = postIds,
                                )
                            },
                            flow5 = postUris.toFlowOrEmpty { postIds ->
                                postDao.embeddedPosts(
                                    viewingProfileId = signedInProfileId?.id,
                                    postUris = postIds,
                                )
                            },
                        ) { feeds, lists, starterPacks, posts, embeddedPosts ->
                            val urisToFeeds = feeds.associateBy { it.entity.uri }
                            val urisToLists = lists.associateBy { it.entity.uri }
                            val urisToStarterPacks = starterPacks.associateBy { it.entity.uri }
                            val urisToPosts = posts.associateBy { it.entity.uri }
                            val urisToEmbeddedPosts = embeddedPosts.associateBy { it.parentPostUri }

                            populatedMessageEntities.map { populatedMessageEntity ->
                                populatedMessageEntity.asExternalModel(
                                    feedGenerator = populatedMessageEntity.feed
                                        ?.feedGeneratorUri
                                        ?.let(urisToFeeds::get)
                                        ?.asExternalModel(),
                                    list = populatedMessageEntity.list
                                        ?.listUri
                                        ?.let(urisToLists::get)
                                        ?.asExternalModel(),
                                    starterPack = populatedMessageEntity.starterPack
                                        ?.starterPackUri
                                        ?.let(urisToStarterPacks::get)
                                        ?.asExternalModel(),
                                    post = populatedMessageEntity.post
                                        ?.postUri
                                        ?.let(urisToPosts::get)
                                        ?.asExternalModel(
                                            quote = populatedMessageEntity.post
                                                ?.postUri
                                                ?.let(urisToEmbeddedPosts::get)
                                                ?.entity
                                                ?.asExternalModel(quote = null),
                                        ),
                                )
                            }
                        }
                    },
                networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getMessages(
                            params = GetMessagesQueryParams(
                                convoId = query.conversationId.id,
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
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
                                        viewingProfileId = signedInProfileId,
                                        conversationId = query.conversationId,
                                        messageView = it.value,
                                    )

                                    is GetMessagesResponseMessageUnion.Unknown -> Unit
                                }
                            }
                        }
                    },
                ),
                ::CursorList,
            )
                .distinctUntilChanged()
        }

    override suspend fun monitorConversationLogs() {
        flow {
            while (true) {
                emit(Unit)
                delay(4.seconds)
            }
        }
            .scan<Unit, String?>(null) { latestCursor, _ ->
                val response = networkService.runCatchingWithMonitoredNetworkRetry {
                    getLog(GetLogQueryParams(latestCursor))
                }
                    .getOrNull()
                    ?: return@scan latestCursor

                if (latestCursor == null) {
                    // First run. Api sets the cursor
                    return@scan response.cursor
                }

                val logs = response.logs

                val messages = LazyList<Pair<ConversationId, MessageView>>()
                val deletedMessages = LazyList<Pair<ConversationId, DeletedMessageView>>()

                val currentCursor = logs.fold(latestCursor) { cursor, union ->
                    when (union) {
                        is Log.AcceptConvo -> maxOf(cursor, union.value.rev)
                        is Log.AddReaction -> union.maxCursor(deletedMessages, messages, cursor)
                        is Log.BeginConvo -> maxOf(cursor, union.value.rev)
                        is Log.CreateMessage -> union.maxCursor(deletedMessages, messages, cursor)
                        is Log.DeleteMessage -> union.maxCursor(deletedMessages, messages, cursor)
                        is Log.LeaveConvo -> maxOf(cursor, union.value.rev)
                        is Log.MuteConvo -> maxOf(cursor, union.value.rev)
                        is Log.ReadMessage -> maxOf(cursor, union.value.rev)
                        is Log.RemoveReaction -> union.maxCursor(deletedMessages, messages, cursor)
                        is Log.Unknown -> cursor
                        is Log.UnmuteConvo -> maxOf(cursor, union.value.rev)
                    }
                }

                // No changes
                if (currentCursor <= latestCursor) return@scan latestCursor

                val signedInProfileId = savedStateDataSource.signedInProfileId

                multipleEntitySaverProvider.saveInTransaction {
                    deletedMessages.list.forEach { (conversationId, message) ->
                        add(
                            conversationId = conversationId,
                            deletedMessageView = message,
                        )
                    }
                    messages.list.forEach { (conversationId, message) ->
                        add(
                            viewingProfileId = signedInProfileId,
                            conversationId = conversationId,
                            messageView = message,
                        )
                    }
                }
                return@scan currentCursor
            }
            .collect()
    }

    override suspend fun sendMessage(
        message: Message.Create,
    ): Outcome {
        val resolvedLinks: List<Link> = resolveLinks(
            profileDao = profileDao,
            networkService = networkService,
            links = message.links,
        )
        return networkService.runCatchingWithMonitoredNetworkRetry {
            sendMessage(
                SendMessageRequest(
                    convoId = message.conversationId.id,
                    message = MessageInput(
                        text = message.text,
                        facets = resolvedLinks.facet(),
                        embed = null,
                    ),
                ),
            )
        }.toOutcome { sentMessage ->
            val signedInProfileId = savedStateDataSource.signedInProfileId

            multipleEntitySaverProvider.saveInTransaction {
                add(
                    viewingProfileId = signedInProfileId,
                    conversationId = message.conversationId,
                    messageView = sentMessage,
                )
            }
        }
    }

    override suspend fun updateReaction(
        reaction: Message.UpdateReaction,
    ): Outcome = networkService.runCatchingWithMonitoredNetworkRetry {
        when (reaction) {
            is Message.UpdateReaction.Add -> addReaction(
                AddReactionRequest(
                    convoId = reaction.convoId.id,
                    messageId = reaction.messageId.id,
                    value = reaction.value,
                ),
            ).map(AddReactionResponse::message)

            is Message.UpdateReaction.Remove -> removeReaction(
                RemoveReactionRequest(
                    convoId = reaction.convoId.id,
                    messageId = reaction.messageId.id,
                    value = reaction.value,
                ),
            ).map(RemoveReactionResponse::message)
        }
    }.toOutcome { message ->
        val signedInProfileId = savedStateDataSource.signedInProfileId

        multipleEntitySaverProvider.saveInTransaction {
            add(
                viewingProfileId = signedInProfileId,
                conversationId = reaction.convoId,
                messageView = message,
            )
        }
    }
}

private fun Log.AddReaction.maxCursor(
    deletedMessages: LazyList<Pair<ConversationId, DeletedMessageView>>,
    messages: LazyList<Pair<ConversationId, MessageView>>,
    logRev: String,
): String {
    when (val message = value.message) {
        is LogAddReactionMessageUnion.DeletedMessageView ->
            deletedMessages.add(value.convoId.let(::ConversationId) to message.value)

        is LogAddReactionMessageUnion.MessageView ->
            messages.add(value.convoId.let(::ConversationId) to message.value)

        is LogAddReactionMessageUnion.Unknown -> Unit
    }
    return maxOf(logRev, value.rev)
}

private fun Log.CreateMessage.maxCursor(
    deletedMessages: LazyList<Pair<ConversationId, DeletedMessageView>>,
    messages: LazyList<Pair<ConversationId, MessageView>>,
    logRev: String,
): String {
    when (val message = value.message) {
        is LogCreateMessageMessageUnion.DeletedMessageView ->
            deletedMessages.add(value.convoId.let(::ConversationId) to message.value)

        is LogCreateMessageMessageUnion.MessageView ->
            messages.add(value.convoId.let(::ConversationId) to message.value)

        is LogCreateMessageMessageUnion.Unknown -> Unit
    }
    return maxOf(logRev, value.rev)
}

private fun Log.DeleteMessage.maxCursor(
    deletedMessages: LazyList<Pair<ConversationId, DeletedMessageView>>,
    messages: LazyList<Pair<ConversationId, MessageView>>,
    logRev: String,
): String {
    when (val message = value.message) {
        is LogDeleteMessageMessageUnion.DeletedMessageView ->
            deletedMessages.add(value.convoId.let(::ConversationId) to message.value)

        is LogDeleteMessageMessageUnion.MessageView ->
            messages.add(value.convoId.let(::ConversationId) to message.value)

        is LogDeleteMessageMessageUnion.Unknown -> Unit
    }
    return maxOf(logRev, value.rev)
}

private fun Log.RemoveReaction.maxCursor(
    deletedMessages: LazyList<Pair<ConversationId, DeletedMessageView>>,
    messages: LazyList<Pair<ConversationId, MessageView>>,
    logRev: String,
): String {
    when (val message = value.message) {
        is LogRemoveReactionMessageUnion.DeletedMessageView ->
            deletedMessages.add(value.convoId.let(::ConversationId) to message.value)

        is LogRemoveReactionMessageUnion.MessageView ->
            messages.add(value.convoId.let(::ConversationId) to message.value)

        is LogRemoveReactionMessageUnion.Unknown -> Unit
    }
    return maxOf(logRev, value.rev)
}
