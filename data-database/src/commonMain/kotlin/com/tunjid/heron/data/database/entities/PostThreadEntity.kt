package com.tunjid.heron.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import com.tunjid.heron.data.core.types.Id

@Entity(
    tableName = "postThreads",
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["cid"],
            childColumns = ["rootPostId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["cid"],
            childColumns = ["parentPostId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["cid"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    primaryKeys = [
        "rootPostId",
        "parentPostId",
        "postId",
    ],
)
data class PostThreadEntity(
    val rootPostId: Id,
    val parentPostId: Id,
    val postId: Id,
)