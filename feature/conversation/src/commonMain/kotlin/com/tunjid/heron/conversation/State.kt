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

import androidx.compose.ui.text.input.TextFieldValue
import com.tunjid.heron.conversation.di.conversationId
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.repository.MessageQuery
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.models
import com.tunjid.heron.scaffold.navigation.sharedElementPrefix
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.ui.text.Memo
import com.tunjid.heron.ui.text.TextFieldValueSerializer
import com.tunjid.treenav.strings.Route
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    @Transient val signedInProfile: Profile? = null,
    val sharedElementPrefix: String,
    val id: ConversationId,
    val members: List<Profile> = emptyList(),
    val pendingItems: List<MessageItem.Pending> = emptyList(),
    override val tilingData: TilingState.Data<MessageQuery, MessageItem>,
    @Serializable(with = TextFieldValueSerializer::class)
    val inputText: TextFieldValue = TextFieldValue(),
    @Transient val sharedRecord: SharedRecord = SharedRecord.None,
    @Transient val messages: List<Memo> = emptyList(),
) : TilingState<MessageQuery, MessageItem>

fun State(route: Route) =
    State(
        id = route.conversationId,
        sharedElementPrefix = route.sharedElementPrefix,
        members = route.models.filterIsInstance<Profile>(),
        tilingData =
            TilingState.Data(
                currentQuery =
                    MessageQuery(
                        conversationId = route.conversationId,
                        data =
                            CursorQuery.Data(
                                page = 0,
                                cursorAnchor = Clock.System.now(),
                                limit = 15,
                            ),
                    )
            ),
    )

@Serializable
sealed class SharedRecord {
    @Serializable data object None : SharedRecord()

    @Serializable data class Pending(val record: Record.Embeddable) : SharedRecord()

    @Serializable data object Consumed : SharedRecord()
}

val SharedRecord.pendingRecord
    get() =
        when (this) {
            SharedRecord.Consumed -> null
            SharedRecord.None -> null
            is SharedRecord.Pending -> record
        }

@Serializable
sealed class MessageItem {
    @Serializable data class Sent(val message: Message) : MessageItem()

    @Serializable
    data class Pending(val sender: Profile, val message: Message.Create, val sentAt: Instant) :
        MessageItem()
}

val MessageItem.sender
    get() =
        when (this) {
            is MessageItem.Pending -> sender
            is MessageItem.Sent -> message.sender
        }

val MessageItem.id
    get() =
        when (this) {
            is MessageItem.Pending -> sentAt.toString()
            is MessageItem.Sent -> message.id.id
        }

val MessageItem.text
    get() =
        when (this) {
            is MessageItem.Pending -> message.text
            is MessageItem.Sent -> message.text
        }

val MessageItem.links
    get() =
        when (this) {
            is MessageItem.Pending -> emptyList()
            is MessageItem.Sent -> message.metadata?.links.orEmpty()
        }

val MessageItem.conversationId
    get() =
        when (this) {
            is MessageItem.Pending -> message.conversationId
            is MessageItem.Sent -> message.conversationId
        }

val MessageItem.sentAt
    get() =
        when (this) {
            is MessageItem.Pending -> sentAt
            is MessageItem.Sent -> message.sentAt
        }

val MessageItem.reactions
    get() =
        when (this) {
            is MessageItem.Pending -> emptyList()
            is MessageItem.Sent -> message.reactions
        }

fun Message.hasEmojiReaction(emoji: String): Boolean = reactions.any { it.value == emoji }

sealed class Action(val key: String) {

    data class Tile(val tilingAction: TilingState.Action) : Action(key = "Tile")

    data class SendPostInteraction(val interaction: Post.Interaction) :
        Action(key = "SendPostInteraction")

    data class SnackbarDismissed(val message: Memo) : Action(key = "SnackbarDismissed")

    data class SendMessage(val message: Message.Create) : Action(key = "SendMessage")

    data class TextChanged(val inputText: TextFieldValue) : Action(key = "TextChanged")

    data class UpdateMessageReaction(val reaction: Message.UpdateReaction) :
        Action(key = "UpdateMessageReaction")

    sealed class SharedRecord : Action(key = "SharedRecord") {
        data class Add(val uri: RecordUri) : SharedRecord()

        data object Remove : SharedRecord()
    }

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop

        data class To(val delegate: NavigationAction.Destination) :
            Navigate(), NavigationAction by delegate
    }
}
