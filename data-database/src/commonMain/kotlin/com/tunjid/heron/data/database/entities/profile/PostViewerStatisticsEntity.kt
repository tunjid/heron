package com.tunjid.heron.data.database.entities.profile

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
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
    val postId: Id,
    @ColumnInfo(defaultValue = "NULL")
    val likeUri: Uri?,
    @ColumnInfo(defaultValue = "NULL")
    val repostUri: Uri?,
    val threadMuted: Boolean,
    val replyDisabled: Boolean,
    val embeddingDisabled: Boolean,
    val pinned: Boolean,
) {
    sealed class Partial {
        abstract val postId: Id

        data class Like(
            override val postId: Id,
            val likeUri: Uri?,
        ) : Partial()

        data class Repost(
            override val postId: Id,
            val repostUri: Uri?,
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