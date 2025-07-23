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

import app.bsky.embed.RecordViewRecordEmbedUnion
import app.bsky.embed.RecordViewRecordUnion
import app.bsky.feed.PostView
import app.bsky.feed.PostViewEmbedUnion
import chat.bsky.convo.DeletedMessageView
import chat.bsky.convo.MessageView
import chat.bsky.convo.MessageViewEmbedUnion
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.MessageId
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.MessageEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
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
            is RecordViewRecordUnion.FeedGeneratorView -> add(
                feedGeneratorView = record.value,
            )

            is RecordViewRecordUnion.GraphListView -> add(
                listView = record.value,
            )

            is RecordViewRecordUnion.GraphStarterPackViewBasic -> add(
                starterPackView = record.value,
            )

            is RecordViewRecordUnion.LabelerLabelerView,
            is RecordViewRecordUnion.Unknown,
            is RecordViewRecordUnion.ViewBlocked,
            is RecordViewRecordUnion.ViewDetached,
            is RecordViewRecordUnion.ViewNotFound -> Unit

            is RecordViewRecordUnion.ViewRecord -> add(
                viewingProfileId = viewingProfileId,
                record = record,
            )
        }

        null -> Unit
    }
}

private fun MultipleEntitySaver.add(
    viewingProfileId: ProfileId?,
    record: RecordViewRecordUnion.ViewRecord,
) {
    record.value.embeds.forEach { embed ->
        add(
            viewingProfileId = viewingProfileId,
            postView = PostView(
                uri = record.value.uri,
                cid = record.value.cid,
                author = record.value.author,
                record = record.value.value,
                embed = when (embed) {
                    is RecordViewRecordEmbedUnion.ExternalView -> PostViewEmbedUnion.ExternalView(
                        value = embed.value
                    )

                    is RecordViewRecordEmbedUnion.ImagesView -> PostViewEmbedUnion.ImagesView(
                        value = embed.value
                    )

                    is RecordViewRecordEmbedUnion.RecordView -> PostViewEmbedUnion.RecordView(
                        value = embed.value
                    )

                    is RecordViewRecordEmbedUnion.RecordWithMediaView -> PostViewEmbedUnion.RecordWithMediaView(
                        value = embed.value
                    )

                    is RecordViewRecordEmbedUnion.Unknown -> PostViewEmbedUnion.Unknown(
                        value = embed.value
                    )

                    is RecordViewRecordEmbedUnion.VideoView -> PostViewEmbedUnion.VideoView(
                        value = embed.value
                    )
                },
                replyCount = record.value.replyCount,
                repostCount = record.value.repostCount,
                likeCount = record.value.likeCount,
                quoteCount = record.value.quoteCount,
                indexedAt = record.value.indexedAt,
                viewer = null,
                labels = record.value.labels,
                threadgate = null,
            ),
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