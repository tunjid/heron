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
import androidx.room.Index
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.LikeUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RepostUri
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.ProfileEntity

@Entity(
    tableName = "postViewerStatistics",
    primaryKeys = [
        "postUri",
        "viewingProfileId",
    ],
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["uri"],
            childColumns = ["postUri"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["viewingProfileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["postUri"]),
        Index(value = ["viewingProfileId"]),
    ],
)
data class PostViewerStatisticsEntity(
    val postUri: PostUri,
    val viewingProfileId: ProfileId,
    @ColumnInfo(defaultValue = "NULL")
    val likeUri: LikeUri?,
    @ColumnInfo(defaultValue = "NULL")
    val repostUri: RepostUri?,
    val threadMuted: Boolean,
    val replyDisabled: Boolean,
    val embeddingDisabled: Boolean,
    val pinned: Boolean,
    @ColumnInfo(defaultValue = "0")
    val bookmarked: Boolean,
) {
    sealed class Partial {
        abstract val postUri: PostUri
        abstract val viewingProfileId: ProfileId

        data class Like(
            override val postUri: PostUri,
            override val viewingProfileId: ProfileId,
            val likeUri: LikeUri?,
        ) : Partial()

        data class Repost(
            override val postUri: PostUri,
            override val viewingProfileId: ProfileId,
            val repostUri: RepostUri?,
        ) : Partial()

        data class Bookmark(
            override val postUri: PostUri,
            override val viewingProfileId: ProfileId,
            val bookmarked: Boolean,
        ) : Partial()

        fun asFull() = PostViewerStatisticsEntity(
            postUri = postUri,
            viewingProfileId = viewingProfileId,
            likeUri = when (this) {
                is Like -> likeUri
                is Repost -> null
                is Bookmark -> null
            },
            repostUri = when (this) {
                is Like -> null
                is Repost -> repostUri
                is Bookmark -> null
            },
            threadMuted = false,
            replyDisabled = false,
            embeddingDisabled = false,
            pinned = false,
            bookmarked = this is Bookmark && bookmarked,
        )
    }
}
