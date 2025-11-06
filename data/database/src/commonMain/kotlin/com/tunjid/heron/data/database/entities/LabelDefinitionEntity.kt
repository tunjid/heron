package com.tunjid.heron.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.tunjid.heron.data.core.types.ProfileId

@Entity(
    tableName = "labelDefinitions",
    primaryKeys = ["creatorId", "identifier"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["creatorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["creatorId"]),
        Index(value = ["identifier"]),
    ],
)
data class LabelDefinitionEntity(
    val creatorId: ProfileId,
    val identifier: String,
    val adultOnly: Boolean,
    val blurs: String,
    val defaultSetting: String,
    val severity: String,
    val localeInfoCbor: String,
)
