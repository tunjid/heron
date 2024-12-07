package com.tunjid.heron.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.Uri
import kotlinx.datetime.Instant

@Entity(
    tableName = "feedFirstRequests",
)
data class FeedFirstRequestInstantEntity(
    @PrimaryKey
    val feedUri: Uri,
    val firstRequestInstant: Instant,
)