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
    tableName = "postAuthors",
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
    ],
)
data class PostAuthorCrossRef(
    val postId: Id,
    val authorId: Id,
): EmbedEntityCrossRef