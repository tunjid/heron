package com.tunjid.heron.data.database.entities.profile

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.types.Id
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
    val liked: Boolean,
    val reposted: Boolean,
    val threadMuted: Boolean,
    val replyDisabled: Boolean,
    val embeddingDisabled: Boolean,
    val pinned: Boolean,
)