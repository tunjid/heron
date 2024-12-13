package com.tunjid.heron.data.database.entities.profile

import androidx.room.Entity
import com.tunjid.heron.data.core.types.Id

@Entity(
    tableName = "profileFollows",
    primaryKeys = [
        "profileId",
        "followedProfileId",
    ]
)
data class ProfileFollowsEntity(
    val profileId: Id,
    val followedProfileId: Id,
    val follows: Boolean,
)