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

package com.tunjid.heron.conversation

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.core.types.asEmbeddableRecordUriOrNull
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.MessageQuery
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.data.utilities.writequeue.toSubscriptionWritable
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.tiling.launchTilingMutations
import com.tunjid.heron.tiling.mapCursorList
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.tiling.updateItems
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.timeline.utilities.shareUri
import com.tunjid.heron.timeline.utilities.writeStatusMessage
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.scaffold.navigation.removeQueryParamsFromCurrentRoute
import com.tunjid.heron.ui.scaffold.navigation.sharedUri
import com.tunjid.heron.ui.stateproduction.RouteStateHolder
import com.tunjid.heron.ui.text.withFormattedTextPost
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.buildTiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.plus
import com.tunjid.tiler.tiledListOf
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal interface ConversationStateHolder :
    RouteStateHolder,
    ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface ConversationViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualConversationViewModel
}

@Stable
class ActualConversationViewModel(
    mutator: ActionSuspendingStateMutator<Action, State>,
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    ConversationStateHolder,
    ActionSuspendingStateMutator<Action, State> by mutator {

    @AssistedInject
    constructor(
        authRepository: AuthRepository,
        recordRepository: RecordRepository,
        messagesRepository: MessageRepository,
        writeQueue: WriteQueue,
        navActions: (NavigationMutation) -> Unit,
        @Assisted scope: CoroutineScope,
        @Assisted route: Route,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = State(route).toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            producer = { state, actions ->
                launchLoadProfileMutations(
                    state = state,
                    authRepository = authRepository,
                )
                launchPendingMessageFlushMutations(
                    state = state,
                    writeQueue = writeQueue,
                )
                launch { messagesRepository.monitorConversationLogs() }
                launchConversationMutations(
                    state = state,
                    messagesRepository = messagesRepository,
                )
                launch {
                    consumeSharedUri(
                        state = state,
                        sharedUri = route.sharedUri,
                        overrideExisting = false,
                        recordRepository = recordRepository,
                    )
                }
                actions.launchMutationsIn(
                    productionScope = this,
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.Navigate -> action.flow.collect {
                            navActions(it.navigationMutation)
                        }
                        is Action.SendPostInteraction -> action.flow.launchPostInteractionMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                        is Action.TogglePublicationSubscription -> action.flow.launchTogglePublicationSubscriptionMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                        is Action.SharedRecord -> action.flow.launchRecordSharingMutations(
                            state = state,
                            recordRepository = recordRepository,
                            navActions = navActions,
                        )
                        is Action.SendMessage -> action.flow.launchSendMessageMutations(
                            state = state,
                            writeQueue = writeQueue,
                            navActions = navActions,
                        )
                        is Action.TextChanged -> action.flow.launchInputTextChangeMutations(state)
                        is Action.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(state)
                        is Action.UpdateMessageReaction -> action.flow.launchUpdateMessageReactionMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                        is Action.AcceptConversation -> action.flow.launchConversationUpdateMutations(
                            state = state,
                            writeQueue = writeQueue,
                            navActions = navActions,
                        )
                        is Action.LeaveConversation -> action.flow.launchConversationUpdateMutations(
                            state = state,
                            writeQueue = writeQueue,
                            navActions = navActions,
                        )
                        is Action.ToggleMute -> action.flow.launchConversationUpdateMutations(
                            state = state,
                            writeQueue = writeQueue,
                            navActions = navActions,
                        )
                        is Action.Tile -> action.flow.launchMessagingTilingMutations(
                            state = state,
                            messagesRepository = messagesRepository,
                        )
                    }
                }
            },
        ),
        scope = scope,
    )
}

context(productionScope: CoroutineScope)
private fun launchLoadProfileMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser.launchedCollect {
    state.signedInProfile = it
}

context(productionScope: CoroutineScope)
private fun launchConversationMutations(
    state: State.SnapshotMutable,
    messagesRepository: MessageRepository,
) = messagesRepository.conversation(state.id).launchedCollect { conversation ->
    // Keep the route-seeded stub until the conversation is available locally, otherwise a
    // not-yet-persisted convo would emit null and blank out the title/members.
    if (conversation != null) state.conversation = conversation
}

context(productionScope: CoroutineScope)
private fun Flow<Action>.launchConversationUpdateMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
    navActions: (NavigationMutation) -> Unit,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
        Writable.ConversationUpdate(
            update = when (action) {
                Action.AcceptConversation -> Conversation.Update.Accept(state.id)
                Action.LeaveConversation -> Conversation.Update.Leave(state.id)
                is Action.ToggleMute -> Conversation.Update.Mute(
                    conversationId = state.id,
                    muted = action.muted,
                )
                else -> error("Unexpected conversation action: $action")
            },
        )
    },
    postEnqueue = { action, memo ->
        if (memo != null) state.messages += memo
        // Leaving deletes the conversation locally, so navigate away from it.
        if (action is Action.LeaveConversation) navActions(Action.Navigate.Pop.navigationMutation)
    },
)

context(productionScope: CoroutineScope)
private fun launchPendingMessageFlushMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = writeQueue.queueChanges.launchedCollect { writes ->
    val queuedIds = writes.mapTo(mutableSetOf(), Writable::queueId)
    val updatedPendingMessages = state.pendingItems.filter { item ->
        queuedIds.contains(Writable.Send(item.message).queueId)
    }
    state.pendingItems = updatedPendingMessages
    state.tilingData.updateItems {
        items.mergePendingMessages(currentQuery, updatedPendingMessages)
    }
}

private suspend fun consumeSharedUri(
    state: State.SnapshotMutable,
    sharedUri: Uri?,
    overrideExisting: Boolean,
    recordRepository: RecordRepository,
) {
    if (sharedUri == null) return
    val recordUri = sharedUri.asEmbeddableRecordUriOrNull() ?: return
    val shouldFetch = when (state.sharedRecord) {
        SharedRecord.Consumed,
        is SharedRecord.Pending,
        -> overrideExisting
        SharedRecord.None -> true
    }
    if (!shouldFetch) return

    // There's a server side bug where non post record embeds aren't resolved
    // to their actual types. For these, just send a link to the record in the message.
    if (recordUri !is PostUri) {
        state.inputText = TextFieldValue(recordUri.shareUri().uri).withFormattedTextPost()
        state.sharedRecord = SharedRecord.Consumed
    } else {
        // Take only one emission so user changes do not override it
        val record = recordRepository.embeddableRecord(recordUri).first()
        if (record is Record.Embeddable.Native) state.sharedRecord = SharedRecord.Pending(record)
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SendPostInteraction>.launchPostInteractionMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { Writable.Interaction(it.interaction) },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.TogglePublicationSubscription>.launchTogglePublicationSubscriptionMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { it.publication.toSubscriptionWritable() },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchedCollect { event ->
    state.messages -= event.message
}

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateMessageReaction>.launchUpdateMessageReactionMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { Writable.Reaction(it.reaction) },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.TextChanged>.launchInputTextChangeMutations(
    state: State.SnapshotMutable,
) = launchedCollectLatest { action ->
    state.inputText = action.inputText
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SharedRecord>.launchRecordSharingMutations(
    state: State.SnapshotMutable,
    recordRepository: RecordRepository,
    navActions: (NavigationMutation) -> Unit,
) = launchedCollectLatest { action ->
    when (action) {
        is Action.SharedRecord.Add -> consumeSharedUri(
            state = state,
            sharedUri = action.uri,
            overrideExisting = true,
            recordRepository = recordRepository,
        )
        Action.SharedRecord.Remove -> {
            state.sharedRecord = SharedRecord.Consumed
            navActions(ConsumeSharedUriQueryParam)
        }
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SendMessage>.launchSendMessageMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
    navActions: (NavigationMutation) -> Unit,
) = launchedCollect { action ->
    val pendingItem = MessageItem.Pending(
        sender = state.signedInProfile ?: stubProfile(
            did = ProfileId(""),
            handle = ProfileHandle(id = ""),
        ),
        message = action.message,
        sentAt = Clock.System.now(),
    )

    state.sharedRecord = when (action.message.recordReference) {
        null -> state.sharedRecord
        else -> SharedRecord.Consumed
    }
    state.inputText = TextFieldValue()
    state.pendingItems += pendingItem
    state.tilingData.updateItems {
        // Add the pending item to the chat
        val currentItems = items
        val tileCount = currentItems.tileCount
        val lastQuery = when {
            tileCount > 0 -> currentItems.queryAtTile(tileCount - 1)
            else -> state.tilingData.currentQuery
        }
        currentItems + tiledListOf(lastQuery to pendingItem)
    }

    if (action.message.recordReference != null) navActions(ConsumeSharedUriQueryParam)

    // Write the message
    val writable = Writable.Send(action.message)
    val status = writeQueue.enqueue(writable)
    val memo = writable.writeStatusMessage(status)
    if (memo != null) state.messages += memo
}

context(productionScope: CoroutineScope)
private fun Flow<Action.Tile>.launchMessagingTilingMutations(
    state: State.SnapshotMutable,
    messagesRepository: MessageRepository,
) = map { it.tilingAction }
    .launchTilingMutations(
        state = state,
        updateQueryData = { copy(data = it) },
        refreshQuery = { copy(data = data.reset()) },
        cursorListLoader = messagesRepository::messages.mapCursorList<MessageQuery, Message, MessageItem>(
            MessageItem::Sent,
        ),
        onNewItems = { items -> items.distinctBy(MessageItem::id) },
        onWriteItems = { deduped ->
            // Receiver is State.SnapshotMutable; runs on the production dispatcher.
            // pendingItems and tilingData.currentQuery are read here race-free with the
            // launchSendMessageMutations / launchPendingMessageFlushMutations writers.
            // Database refreshes can happen at any time. Add pending items.
            if (pendingItems.isEmpty()) deduped
            else deduped.mergePendingMessages(
                currentQuery = tilingData.currentQuery,
                pendingItems = pendingItems,
            )
        },
    )

private fun TiledList<MessageQuery, MessageItem>.mergePendingMessages(
    currentQuery: MessageQuery,
    pendingItems: List<MessageItem.Pending>,
): TiledList<MessageQuery, MessageItem> = when {
    isEmpty() -> buildTiledList {
        addAll(
            query = currentQuery,
            items = pendingItems,
        )
    }

    else -> buildTiledList {
        (0..<tileCount).forEach { tileIndex ->
            val tile = tileAt(tileIndex)
            val lastTileIndex = tileCount - 1
            val tileSublist = subList(tile.start, tile.end)
            if (tileIndex == lastTileIndex) addAll(
                query = queryAtTile(tileIndex),
                // Add pending items to the last chunk and sort
                items = (tileSublist + pendingItems).sortedBy(MessageItem::sentAt),
            )
            else addAll(
                query = queryAtTile(tileIndex),
                items = tileSublist,
            )
        }
    }
}

private val ConsumeSharedUriQueryParam = removeQueryParamsFromCurrentRoute(setOf("sharedUri"))
