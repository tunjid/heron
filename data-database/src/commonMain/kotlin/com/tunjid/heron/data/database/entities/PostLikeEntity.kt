package com.tunjid.heron.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.tunjid.heron.data.core.types.Id
import kotlinx.datetime.Instant

@Entity(
    tableName = "postLikes",
    primaryKeys = ["postId", "authorId"],
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["cid"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["postId"]),
        Index(value = ["authorId"]),
        Index(value = ["indexedAt"]),
    ],
)
data class PostLikeEntity(
    val postId: Id,
    val authorId: Id,
    val createdAt: Instant,
    val indexedAt: Instant,
)