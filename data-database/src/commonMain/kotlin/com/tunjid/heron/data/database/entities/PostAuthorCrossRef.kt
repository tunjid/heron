package com.tunjid.heron.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.Id

/**
 * Cross reference for many to many relationship between [Post] and [authorEntity]
 */
@Entity(
    tableName = "posts_authors",
    primaryKeys = ["post_id", "author_id"],
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["cid"],
            childColumns = ["post_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["author_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["post_id"]),
        Index(value = ["author_id"]),
    ],
)
data class PostAuthorCrossRef(
    @ColumnInfo(name = "post_id")
    val postId: Id,
    @ColumnInfo(name = "author_id")
    val authorId: Id,
): EmbedEntityCrossRef