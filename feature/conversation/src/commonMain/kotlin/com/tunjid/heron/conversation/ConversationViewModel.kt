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
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Stable
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
                        is Action.MarkConversationRead -> action.flow.launchMarkConversationReadMutations(
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
    state: State.Mutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser.launchedCollect {
    state.signedInProfile = it
}

context(productionScope: CoroutineScope)
private fun launchConversationMutations(
    state: State.Mutable,
    messagesRepository: MessageRepository,
) = messagesRepository.conversation(state.id).launchedCollect { conversation ->
    // Keep the route-seeded stub until the conversation is available locally, otherwise a
    // not-yet-persisted convo would emit null and blank out the title/members.
    if (conversation != null) state.conversation = conversation
}

context(productionScope: CoroutineScope)
private fun Flow<Action>.launchConversationUpdateMutations(
    state: State.Mutable,
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
private fun Flow<Action.MarkConversationRead>.launchMarkConversationReadMutations(
    state: State.Mutable,
    messagesRepository: MessageRepository,
) = launchedCollectLatest { action ->
    messagesRepository.markConversationRead(
        conversationId = state.id,
        messageId = action.messageId,
    )
}

context(productionScope: CoroutineScope)
private fun launchPendingMessageFlushMutations(
    state: State.Mutable,
    writeQueue: WriteQueue,
) = combine(
    writeQueue.queueChanges,
    writeQueue.failedWrites,
    ::Pair,
).launchedCollect { (writes, failedWrites) ->
    val queuedIds = writes.mapTo(mutableSetOf(), Writable::queueId)
    val failedIds = failedWrites.mapTo(mutableSetOf()) { it.writable.queueId }
    val sentLocalIds = state.tilingData.items.sentLocalIds()
    state.pendingItems = state.pendingItems.filter { item ->
        val queueId = Writable.Send(item.message).queueId
        when {
            queuedIds.contains(queueId) -> true
            failedIds.contains(queueId) -> false
            // The write has been dequeued, but the message it saved may not have reached
            // the tiled list yet. Keep the pending item until it has.
            else ->
                item.message.localId
                    ?.let { it !in sentLocalIds }
                    ?: false
        }
    }
}

private suspend fun consumeSharedUri(
    state: State.Mutable,
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
    state: State.Mutable,
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
    state: State.Mutable,
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
    state: State.Mutable,
) = launchedCollect { event ->
    state.messages -= event.message
}

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateMessageReaction>.launchUpdateMessageReactionMutations(
    state: State.Mutable,
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
    state: State.Mutable,
) = launchedCollectLatest { action ->
    state.inputText = action.inputText
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SharedRecord>.launchRecordSharingMutations(
    state: State.Mutable,
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

@OptIn(ExperimentalUuidApi::class)
context(productionScope: CoroutineScope)
private fun Flow<Action.SendMessage>.launchSendMessageMutations(
    state: State.Mutable,
    writeQueue: WriteQueue,
    navActions: (NavigationMutation) -> Unit,
) = launchedCollect { action ->
    val message = when (action.message.localId) {
        null -> action.message.copy(localId = Uuid.random().toString())
        else -> action.message
    }
    val pendingItem = MessageItem.Pending(
        sender = state.signedInProfile ?: stubProfile(
            did = ProfileId(""),
            handle = ProfileHandle(id = ""),
        ),
        message = message,
        sentAt = Clock.System.now(),
    )

    state.sharedRecord = when (message.recordReference) {
        null -> state.sharedRecord
        else -> SharedRecord.Consumed
    }
    state.inputText = TextFieldValue()
    state.pendingItems += pendingItem

    if (message.recordReference != null) navActions(ConsumeSharedUriQueryParam)

    // Write the message
    val writable = Writable.Send(message)
    val status = writeQueue.enqueue(writable)
    val memo = writable.writeStatusMessage(status)
    if (memo != null) state.messages += memo
}

context(productionScope: CoroutineScope)
private fun Flow<Action.Tile>.launchMessagingTilingMutations(
    state: State.Mutable,
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
    )

internal val State.mergedMessages: TiledList<MessageQuery, MessageItem>
    get() {
        val items = tilingData.items
        // Pending items whose sent message is already in the tiled list are superseded by it.
        val sentLocalIds = items.sentLocalIds()
        val unsentPendingItems = pendingItems.filter { item ->
            item.message.localId !in sentLocalIds
        }
        return when {
            items.isEmpty() -> buildTiledList {
                addAll(
                    query = tilingData.currentQuery,
                    items = unsentPendingItems.sortedByDescending(MessageItem::sentAt),
                )
            }

            else -> buildTiledList {
                (0..<items.tileCount).forEach { tileIndex ->
                    val tile = items.tileAt(tileIndex)
                    val tileSublist = items.subList(tile.start, tile.end)
                    if (tileIndex == 0) addAll(
                        query = items.queryAtTile(tileIndex),
                        // Add pending items to the newest chunk and sort
                        items = (tileSublist + unsentPendingItems).sortedByDescending(MessageItem::sentAt),
                    )
                    else addAll(
                        query = items.queryAtTile(tileIndex),
                        items = tileSublist,
                    )
                }
            }
        }
    }

private fun TiledList<MessageQuery, MessageItem>.sentLocalIds(): Set<String> =
    mapNotNullTo(mutableSetOf()) { item ->
        (item as? MessageItem.Sent)?.message?.localId
    }

private val ConsumeSharedUriQueryParam = removeQueryParamsFromCurrentRoute(setOf("sharedUri"))
