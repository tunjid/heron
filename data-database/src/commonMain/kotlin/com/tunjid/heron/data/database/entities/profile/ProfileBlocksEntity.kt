package com.tunjid.heron.data.database.entities.profile

import androidx.room.Entity
import com.tunjid.heron.data.core.types.Id

@Entity(
    tableName = "profileBlocks",
    primaryKeys = [
        "profileId",
        "blockedProfileId",
    ]
)
data class ProfileBlocksEntity(
    val profileId: Id,
    val blockedProfileId: Id,
    val blocked: Boolean,
)