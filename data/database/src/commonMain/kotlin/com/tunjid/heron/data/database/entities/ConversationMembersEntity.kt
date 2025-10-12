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

import androidx.room.Entity
import androidx.room.ForeignKey
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.ProfileId

@Entity(
    tableName = "conversationMembers",
    primaryKeys = [
        "conversationId",
        "memberId",
    ],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = [
                "id",
                "ownerId",
            ],
            childColumns = [
                "conversationId",
                "conversationOwnerId",
            ],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ConversationMembersEntity(
    val conversationId: ConversationId,
    val conversationOwnerId: ProfileId,
    val memberId: ProfileId,
)
