package com.tunjid.heron.data.database.entities.profile

import androidx.room.Entity
import androidx.room.ForeignKey
import com.tunjid.heron.data.core.models.ProfileRelationship
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.database.entities.ProfileEntity

@Entity(
    tableName = "profileProfileRelationships",
    primaryKeys = [
        "profileId",
        "otherProfileId",
    ],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["otherProfileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProfileProfileRelationshipsEntity(
    val profileId: Id,
    val otherProfileId: Id,
    val blocking: Boolean,
    val muted: Boolean,
    val follows: Boolean,
) {
    data class Partial(
        val profileId: Id,
        val otherProfileId: Id,
        val follows: Boolean,
    )
}

internal fun ProfileProfileRelationshipsEntity.partial() =
    ProfileProfileRelationshipsEntity.Partial(
        profileId = profileId,
        otherProfileId = otherProfileId,
        follows = follows,
    )

fun ProfileProfileRelationshipsEntity.asExternalModel() =
    ProfileRelationship(
        blocking = blocking,
        muted = muted,
        follows = follows,
    )
