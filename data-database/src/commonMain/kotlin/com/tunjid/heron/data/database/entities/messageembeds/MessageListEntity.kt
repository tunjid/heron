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

package com.tunjid.heron.data.database.entities.messageembeds

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.MessageId
import com.tunjid.heron.data.database.entities.ListEntity
import com.tunjid.heron.data.database.entities.MessageEntity


@Entity(
    tableName = "messageLists",
    primaryKeys = [
        "messageId",
        "listUri"
    ],
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["uri"],
            childColumns = ["listUri"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["messageId"]),
        Index(value = ["listUri"]),
    ],
)
data class MessageListEntity(
    val messageId: MessageId,
    val listUri: ListUri,
)


