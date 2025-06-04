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

package com.tunjid.heron.data.database.entities.profile

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.database.entities.PostEntity

@Entity(
    tableName = "postViewerStatistics",
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["cid"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PostViewerStatisticsEntity(
    @PrimaryKey
    val postId: PostId,
    @ColumnInfo(defaultValue = "NULL")
    val likeUri: GenericUri?,
    @ColumnInfo(defaultValue = "NULL")
    val repostUri: GenericUri?,
    val threadMuted: Boolean,
    val replyDisabled: Boolean,
    val embeddingDisabled: Boolean,
    val pinned: Boolean,
) {
    sealed class Partial {
        abstract val postId: PostId

        data class Like(
            override val postId: PostId,
            val likeUri: GenericUri?,
        ) : Partial()

        data class Repost(
            override val postId: PostId,
            val repostUri: GenericUri?,
        ) : Partial()

        fun asFull() = PostViewerStatisticsEntity(
            postId = postId,
            likeUri = when (this) {
                is Like -> likeUri
                is Repost -> null
            },
            repostUri = when (this) {
                is Like -> null
                is Repost -> repostUri
            },
            threadMuted = false,
            replyDisabled = false,
            embeddingDisabled = false,
            pinned = false,
        )
    }
}