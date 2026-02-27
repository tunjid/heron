package com.tunjid.heron.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileId
import kotlin.time.Instant

@Entity(
    tableName = "profile_statuses",
    primaryKeys = ["profileId"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProfileStatusEntity(
    val profileId: ProfileId,
    val statusValue: String,
    val embedUri: String? = null,
    val embedTitle: String? = null,
    val embedDescription: String? = null,
    val embedThumb: ImageUri? = null,
    val expiresAt: Instant? = null,
    val isActive: Boolean? = null,
    val isDisabled: Boolean? = null,
)

fun ProfileStatusEntity.asExternalModel() = Profile.ProfileStatus(
    status = statusValue,
    embed = if (embedUri != null && embedTitle != null && embedDescription != null)
        Profile.ProfileStatus.Embed(
            uri = embedUri,
            title = embedTitle,
            description = embedDescription,
            thumb = embedThumb,
        )
    else null,
    expiresAt = expiresAt,
    isActive = isActive,
    isDisabled = isDisabled,
)

fun Profile.ProfileStatus.asEntity(profileId: ProfileId) = ProfileStatusEntity(
    profileId = profileId,
    statusValue = status,
    embedUri = embed?.uri,
    embedTitle = embed?.title,
    embedDescription = embed?.description,
    embedThumb = embed?.thumb,
    expiresAt = expiresAt,
    isActive = isActive,
    isDisabled = isDisabled,
)
