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

package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.embed.RecordViewRecordEmbedUnion as MessagePost
import app.bsky.embed.RecordViewRecordUnion
import app.bsky.feed.PostView
import app.bsky.feed.PostViewEmbedUnion as TimelinePost
import chat.bsky.convo.DeletedMessageView
import chat.bsky.convo.MessageView
import chat.bsky.convo.MessageViewEmbedUnion
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.MessageId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.database.entities.MessageEntity
import com.tunjid.heron.data.database.entities.MessageReactionEntity
import com.tunjid.heron.data.database.entities.messageembeds.MessageFeedGeneratorEntity
import com.tunjid.heron.data.database.entities.messageembeds.MessageListEntity
import com.tunjid.heron.data.database.entities.messageembeds.MessagePostEntity
import com.tunjid.heron.data.database.entities.messageembeds.MessageStarterPackEntity

internal fun MultipleEntitySaver.add(
    viewingProfileId: ProfileId?,
    conversationId: ConversationId,
    messageView: MessageView,
) {
    viewingProfileId ?: return
    add(
        entity = emptyProfileEntity(messageView.sender.did),
    )

    add(
        MessageEntity(
            id = messageView.id.let(::MessageId),
            rev = messageView.rev,
            text = messageView.text,
            senderId = messageView.sender.did.did.let(::ProfileId),
            conversationId = conversationId,
            conversationOwnerId = viewingProfileId,
            isDeleted = false,
            sentAt = messageView.sentAt,
        ),
    )
    messageView.reactions.forEach { reactionView ->
        add(
            MessageReactionEntity(
                value = reactionView.value,
                messageId = messageView.id.let(::MessageId),
                senderId = reactionView.sender.did.did.let(::ProfileId),
                createdAt = reactionView.createdAt,
            ),
        )
    }

    when (val embed = messageView.embed) {
        is MessageViewEmbedUnion.Unknown -> Unit
        is MessageViewEmbedUnion.View -> when (val record = embed.value.record) {
            is RecordViewRecordUnion.FeedGeneratorView -> {
                add(
                    feedGeneratorView = record.value,
                )
                add(
                    entity = MessageFeedGeneratorEntity(
                        messageId = messageView.id.let(::MessageId),
                        feedGeneratorUri = record.value.uri.atUri.let(::FeedGeneratorUri),
                    ),
                )
            }

            is RecordViewRecordUnion.GraphListView -> {
                add(
                    listView = record.value,
                )
                add(
                    entity = MessageListEntity(
                        messageId = messageView.id.let(::MessageId),
                        listUri = record.value.uri.atUri.let(::ListUri),
                    ),
                )
            }

            is RecordViewRecordUnion.GraphStarterPackViewBasic -> {
                add(
                    starterPackView = record.value,
                )
                add(
                    entity = MessageStarterPackEntity(
                        messageId = messageView.id.let(::MessageId),
                        starterPackUri = record.value.uri.atUri.let(::StarterPackUri),
                    ),
                )
            }

            is RecordViewRecordUnion.LabelerLabelerView,
            is RecordViewRecordUnion.Unknown,
            is RecordViewRecordUnion.ViewBlocked,
            is RecordViewRecordUnion.ViewDetached,
            is RecordViewRecordUnion.ViewNotFound,
            -> {
                Unit
            }

            is RecordViewRecordUnion.ViewRecord -> {
                add(
                    viewingProfileId = viewingProfileId,
                    messageId = messageView.id.let(::MessageId),
                    record = record,
                )
            }
        }

        null -> Unit
    }
}

private fun MultipleEntitySaver.add(
    viewingProfileId: ProfileId?,
    messageId: MessageId,
    record: RecordViewRecordUnion.ViewRecord,
) {
    record.value.embeds.forEach { embed ->
        val postView = PostView(
            uri = record.value.uri,
            cid = record.value.cid,
            author = record.value.author,
            record = record.value.value,
            embed = when (embed) {
                is MessagePost.ExternalView -> TimelinePost.ExternalView(embed.value)
                is MessagePost.ImagesView -> TimelinePost.ImagesView(embed.value)
                is MessagePost.RecordView -> TimelinePost.RecordView(embed.value)
                is MessagePost.RecordWithMediaView -> TimelinePost.RecordWithMediaView(embed.value)
                is MessagePost.Unknown -> TimelinePost.Unknown(embed.value)
                is MessagePost.VideoView -> TimelinePost.VideoView(embed.value)
            },
            replyCount = record.value.replyCount,
            repostCount = record.value.repostCount,
            likeCount = record.value.likeCount,
            quoteCount = record.value.quoteCount,
            indexedAt = record.value.indexedAt,
            viewer = null,
            labels = record.value.labels,
            threadgate = null,
        )
        add(
            viewingProfileId = viewingProfileId,
            postView = postView,
        )
        add(
            entity = MessagePostEntity(
                messageId = messageId,
                postUri = postView.uri.atUri.let(::PostUri),
            ),
        )
    }
}

internal fun MultipleEntitySaver.add(
    viewingProfileId: ProfileId?,
    conversationId: ConversationId,
    deletedMessageView: DeletedMessageView,
) {
    viewingProfileId ?: return

    add(
        entity = emptyProfileEntity(deletedMessageView.sender.did),
    )

    add(
        MessageEntity(
            id = deletedMessageView.id.let(::MessageId),
            rev = deletedMessageView.rev,
            text = "",
            isDeleted = true,
            senderId = deletedMessageView.sender.did.did.let(::ProfileId),
            conversationId = conversationId,
            conversationOwnerId = viewingProfileId,
            sentAt = deletedMessageView.sentAt,
        ),
    )
}
