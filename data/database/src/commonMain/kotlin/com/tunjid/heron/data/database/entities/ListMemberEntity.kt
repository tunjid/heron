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

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.tunjid.heron.data.core.models.ListMember
import com.tunjid.heron.data.core.types.ListMemberUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.profile.ProfileViewerStateEntity
import com.tunjid.heron.data.database.entities.profile.asExternalModel
import kotlin.time.Instant

@Entity(
    tableName = "listMembers",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["uri"],
            childColumns = ["listUri"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["createdAt"]),
    ],
)
data class ListMemberEntity(
    @PrimaryKey
    val uri: ListMemberUri,
    val listUri: ListUri,
    val subjectId: ProfileId,
    val createdAt: Instant,
)

data class PopulatedListMemberEntity(
    @Embedded
    val entity: ListMemberEntity,
    @Embedded
    val viewerStateEntity: ProfileViewerStateEntity?,
    @Relation(
        parentColumn = "subjectId",
        entityColumn = "did",
    )
    val subject: ProfileEntity?,
)

fun PopulatedListMemberEntity.asExternalModel() =
    ListMember(
        uri = entity.uri,
        subject = subject.asExternalModel(),
        listUri = entity.listUri,
        createdAt = entity.createdAt,
        viewerState = viewerStateEntity?.asExternalModel(),
    )
