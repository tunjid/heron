/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.data.database.entities

import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.tunjid.heron.data.core.models.ListMember
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.ListMemberUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.StarterPackId
import com.tunjid.heron.data.core.types.StarterPackUri
import kotlin.time.Instant

@Entity(
    tableName = "starterPacks",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["creatorId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["uri"]),
        Index(value = ["cid"]),
        Index(value = ["creatorId"]),
        Index(value = ["indexedAt"]),
        Index(value = ["createdAt"]),
    ],
)
data class StarterPackEntity(
    val cid: StarterPackId,
    @PrimaryKey
    val uri: StarterPackUri,
    val creatorId: ProfileId,
    val listUri: ListUri?,
    val name: String,
    val description: String?,
    val joinedWeekCount: Long?,
    val joinedAllTimeCount: Long?,
    val indexedAt: Instant,
    val createdAt: Instant,
)

data class PopulatedStarterPackEntity(
    @Embedded
    val entity: StarterPackEntity,
    @Relation(
        parentColumn = "creatorId",
        entityColumn = "did",
    )
    val creator: ProfileEntity?,
    @Relation(
        parentColumn = "listUri",
        entityColumn = "uri",
    )
    val list: ListEntity?,
    @Relation(
        parentColumn = "uri",
        entityColumn = "uri",
    )
    val labelEntities: List<LabelEntity>,
    @Relation(
        entity = StarterPackMemberView::class,
        parentColumn = "listUri",
        entityColumn = "listUri",
    )
    val members: List<PopulatedStarterPackMemberEntity>,
) : PopulatedRecordEntity {
    override val recordUri: EmbeddableRecordUri
        get() = entity.uri
}

fun PopulatedStarterPackEntity.asExternalModel() =
    StarterPack(
        cid = entity.cid,
        uri = entity.uri,
        name = entity.name,
        description = entity.description,
        creator = creator.asExternalModel(),
        list = creator?.let { profileEntity ->
            list?.asExternalModel(
                creator = profileEntity.asExternalModel(),
                labels = emptyList(),
            )
        },
        joinedWeekCount = entity.joinedWeekCount,
        joinedAllTimeCount = entity.joinedAllTimeCount,
        indexedAt = entity.indexedAt,
        labels = labelEntities.asActiveExternalModels(),
        members = members
            .sortedByDescending { it.member.createdAt }
            .map { populatedMember ->
                ListMember(
                    uri = populatedMember.member.uri,
                    subject = populatedMember.subject.asExternalModel(),
                    listUri = populatedMember.member.listUri,
                    createdAt = populatedMember.member.createdAt,
                    viewerState = null,
                )
            },
    )

@DatabaseView(
    viewName = "starterPackMembers",
    value = """
        SELECT uri, listUri, subjectId, createdAt
        FROM (
            SELECT
                uri,
                listUri,
                subjectId,
                createdAt,
                ROW_NUMBER() OVER (
                    PARTITION BY listUri
                    ORDER BY createdAt DESC
                ) AS memberRank
            FROM listMembers
        )
        WHERE memberRank <= 10
    """,
)
data class StarterPackMemberView(
    val uri: ListMemberUri,
    val listUri: ListUri,
    val subjectId: ProfileId,
    val createdAt: Instant,
)

data class PopulatedStarterPackMemberEntity(
    @Embedded
    val member: StarterPackMemberView,
    @Relation(
        parentColumn = "subjectId",
        entityColumn = "did",
    )
    val subject: ProfileEntity?,
)
