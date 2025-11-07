package com.tunjid.heron.data.database.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Relation
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId

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
    val cid: String,
    val uri: GenericUri,
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
)

fun PopulatedLabelerEntity.asExternalModel(): Labeler = Labeler(
    uri = entity.uri,
    creatorId = entity.creatorId,
    definitions = definitions.map { it.asExternalModel() },
    values = definitions.map {
        Label.Value(it.identifier)
    },
)
