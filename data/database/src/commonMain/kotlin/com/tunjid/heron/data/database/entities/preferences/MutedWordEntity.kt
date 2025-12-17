package com.tunjid.heron.data.database.entities.preferences

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.core.types.MutedWordId
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.ProfileEntity
import kotlin.time.Instant

@Entity(
    tableName = "muted_words",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["viewingProfileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["viewingProfileId"]),
        Index(value = ["value"]),
        Index(value = ["expiresAt"]),
    ],
)
data class MutedWordEntity(
    @PrimaryKey
    val id: MutedWordId,
    val value: String,
    val viewingProfileId: ProfileId,
    val targetsCbor: String,
    val actorTargetCbor: String? = null,
    val expiresAt: Instant,
)

fun MutedWordEntity.asExternalModel(): MutedWordPreference =
    MutedWordPreference(
        value = value,
        targets = targetsCbor.fromBase64EncodedUrl(),
        actorTarget = actorTargetCbor?.fromBase64EncodedUrl(),
        expiresAt = expiresAt,
    )
