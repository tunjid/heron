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
import com.tunjid.heron.data.core.types.PostUri

@Entity(
    tableName = "postThreads",
    foreignKeys =
        [
            ForeignKey(
                entity = PostEntity::class,
                parentColumns = ["uri"],
                childColumns = ["parentPostUri"],
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE,
            ),
            ForeignKey(
                entity = PostEntity::class,
                parentColumns = ["uri"],
                childColumns = ["postUri"],
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE,
            ),
        ],
    primaryKeys = ["parentPostUri", "postUri"],
    indices = [Index(value = ["parentPostUri"]), Index(value = ["postUri"])],
)
data class PostThreadEntity(val parentPostUri: PostUri, val postUri: PostUri)

data class PostThreadAndGenerationEntity(
    @Embedded val entity: PostThreadEntity,
    val generation: Long,
)
