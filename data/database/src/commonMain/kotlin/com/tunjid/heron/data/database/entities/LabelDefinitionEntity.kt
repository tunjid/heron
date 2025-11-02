package com.tunjid.heron.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.tunjid.heron.data.core.types.ProfileId
import kotlinx.serialization.Serializable

@Entity(
    tableName = "labelDefinitions",
    primaryKeys = ["labelerId", "identifier"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["labelerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["labelerId"]),
        Index(value = ["identifier"]),
    ],
)
data class LabelDefinitionEntity(
    val labelerId: ProfileId,
    val identifier: String,
    val adultOnly: Boolean,
    val blurs: String,
    val defaultSetting: String,
    val severity: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val localeInfo: ByteArray,
) {

    @Serializable
    data class LocaleInfo(
        val lang: String,
        val name: String,
        val description: String,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as LabelDefinitionEntity

        if (adultOnly != other.adultOnly) return false
        if (labelerId != other.labelerId) return false
        if (identifier != other.identifier) return false
        if (blurs != other.blurs) return false
        if (defaultSetting != other.defaultSetting) return false
        if (severity != other.severity) return false
        if (!localeInfo.contentEquals(other.localeInfo)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = adultOnly.hashCode()
        result = 31 * result + labelerId.hashCode()
        result = 31 * result + identifier.hashCode()
        result = 31 * result + blurs.hashCode()
        result = 31 * result + defaultSetting.hashCode()
        result = 31 * result + severity.hashCode()
        result = 31 * result + localeInfo.contentHashCode()
        return result
    }
}
