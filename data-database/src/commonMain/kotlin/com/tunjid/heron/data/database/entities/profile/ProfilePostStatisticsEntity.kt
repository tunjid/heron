package com.tunjid.heron.data.database.entities.profile

import androidx.room.Entity
import com.tunjid.heron.data.core.types.Id

@Entity(
    tableName = "profilePostStatistics",
    primaryKeys = [
        "profileId",
        "postId",
    ]
)
data class ProfilePostStatisticsEntity(
    val profileId: Id,
    val postId: Id,
    val liked: Boolean,
    val reposted: Boolean,
)