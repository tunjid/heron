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

package com.tunjid.heron.data.database.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.MessageId
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.messageembeds.MessageFeedGeneratorEntity
import com.tunjid.heron.data.database.entities.messageembeds.MessageListEntity
import com.tunjid.heron.data.database.entities.messageembeds.MessagePostEntity
import com.tunjid.heron.data.database.entities.messageembeds.MessageStarterPackEntity
import kotlinx.datetime.Instant

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["senderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sentAt"]),
    ],
)
data class MessageEntity(
    @PrimaryKey
    val id: MessageId,
    val rev: String,
    val text: String,
    val senderId: ProfileId,
    val conversationId: ConversationId,
    val isDeleted: Boolean,
    val sentAt: Instant,
) {
    data class RecordData(
        val base64EncodedRecord: String?,
    )
}

data class PopulatedMessageEntity(
    @Embedded
    val entity: MessageEntity,
    @Relation(
        parentColumn = "senderId",
        entityColumn = "did",
    )
    val sender: ProfileEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "messageId",
    )
    val reactions: List<MessageReactionEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "messageId",
    )
    val feed: MessageFeedGeneratorEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "messageId",
    )
    val list: MessageListEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "messageId",
    )
    val starterPack: MessageStarterPackEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "messageId",
    )
    val post: MessagePostEntity?,
)

fun PopulatedMessageEntity.asExternalModel(
    feedGenerator: FeedGenerator? = null,
    list: FeedList? = null,
    starterPack: StarterPack? = null,
    post: Post? = null,
) = Message(
    id = entity.id,
    conversationId = entity.conversationId,
    text = entity.text,
    sentAt = entity.sentAt,
    isDeleted = entity.isDeleted,
    sender = sender.asExternalModel(),
    feedGenerator = feedGenerator,
    list = list,
    starterPack = starterPack,
    post = post,
    reactions = reactions.map {
        Message.Reaction(
            value = it.value,
            senderId = it.senderId,
            createdAt = it.createdAt,
        )
    },
)
