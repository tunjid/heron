package com.tunjid.heron.data.database.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Relation
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.LabelerId
import com.tunjid.heron.data.core.types.LabelerUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri

@Entity(
    tableName = "labelers",
    primaryKeys = [
        "uri",
    ],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["creatorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["uri"]),
        Index(value = ["creatorId"]),
    ],
)
data class LabelerEntity(
    val cid: LabelerId,
    val uri: LabelerUri,
    val creatorId: ProfileId,
    val likeCount: Long?,
)

data class PopulatedLabelerEntity(
    @Embedded
    val entity: LabelerEntity,
    @Relation(
        parentColumn = "creatorId",
        entityColumn = "did",
    )
    val creator: ProfileEntity?,
    @Relation(
        parentColumn = "creatorId",
        entityColumn = "creatorId",
    )
    val definitions: List<LabelDefinitionEntity>,
) : PopulatedRecordEntity {
    override val recordUri: RecordUri
        get() = entity.uri
}

fun PopulatedLabelerEntity.asExternalModel(): Labeler = Labeler(
    uri = entity.uri,
    cid = entity.cid,
    likeCount = entity.likeCount,
    creator = creator
        ?.asExternalModel()
        ?: stubProfile(
            did = entity.creatorId,
            handle = ProfileHandle(""),
        ),
    definitions = definitions
        .map(LabelDefinitionEntity::asExternalModel),
    values = definitions.map {
        Label.Value(it.identifier)
    },
)
