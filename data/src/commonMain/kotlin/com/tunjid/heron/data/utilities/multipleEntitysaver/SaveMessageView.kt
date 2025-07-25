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
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.FeedGeneratorId
import com.tunjid.heron.data.core.types.ListId
import com.tunjid.heron.data.core.types.MessageId
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.StarterPackId
import com.tunjid.heron.data.database.entities.MessageEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.messageembeds.MessageFeedGeneratorEntity
import com.tunjid.heron.data.database.entities.messageembeds.MessageListEntity
import com.tunjid.heron.data.database.entities.messageembeds.MessagePostEntity
import com.tunjid.heron.data.database.entities.messageembeds.MessageStarterPackEntity
import sh.christian.ozone.api.Did

internal fun MultipleEntitySaver.add(
    viewingProfileId: ProfileId?,
    conversationId: ConversationId,
    messageView: MessageView,
) {
    add(
        entity = emptyProfileEntity(messageView.sender.did)
    )

    add(
        MessageEntity(
            id = messageView.id.let(::MessageId),
            rev = messageView.rev,
            text = messageView.text,
            senderId = messageView.sender.did.did.let(::ProfileId),
            conversationId = conversationId,
            isDeleted = false,
            sentAt = messageView.sentAt,
        )
    )

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
                        feedGeneratorId = record.value.cid.cid.let(::FeedGeneratorId),
                    )
                )
            }

            is RecordViewRecordUnion.GraphListView -> {
                add(
                    listView = record.value,
                )
                add(
                    entity = MessageListEntity(
                        messageId = messageView.id.let(::MessageId),
                        listId = record.value.cid.cid.let(::ListId),
                    )
                )
            }

            is RecordViewRecordUnion.GraphStarterPackViewBasic -> {
                add(
                    starterPackView = record.value,
                )
                add(
                    entity = MessageStarterPackEntity(
                        messageId = messageView.id.let(::MessageId),
                        starterPackId = record.value.cid.cid.let(::StarterPackId),
                    )
                )
            }

            is RecordViewRecordUnion.LabelerLabelerView,
            is RecordViewRecordUnion.Unknown,
            is RecordViewRecordUnion.ViewBlocked,
            is RecordViewRecordUnion.ViewDetached,
            is RecordViewRecordUnion.ViewNotFound -> {
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
                postId = postView.cid.cid.let(::PostId),
            )
        )
    }
}

internal fun MultipleEntitySaver.add(
    conversationId: ConversationId,
    deletedMessageView: DeletedMessageView,
) {
    add(
        entity = emptyProfileEntity(deletedMessageView.sender.did)
    )

    add(
        MessageEntity(
            id = deletedMessageView.id.let(::MessageId),
            rev = deletedMessageView.rev,
            text = "",
            isDeleted = true,
            senderId = deletedMessageView.sender.did.did.let(::ProfileId),
            conversationId = conversationId,
            sentAt = deletedMessageView.sentAt,
        )
    )
}

private fun emptyProfileEntity(
    did: Did,
) = ProfileEntity(
    did = ProfileId(did.did),
    handle = Constants.unknownAuthorHandle,
    displayName = null,
    description = null,
    avatar = null,
    banner = null,
    followersCount = null,
    followsCount = null,
    postsCount = null,
    joinedViaStarterPack = null,
    indexedAt = null,
    createdAt = null,
    associated = ProfileEntity.Associated(
        createdListCount = 0,
        createdFeedGeneratorCount = 0,
        createdStarterPackCount = 0,
    ),
)