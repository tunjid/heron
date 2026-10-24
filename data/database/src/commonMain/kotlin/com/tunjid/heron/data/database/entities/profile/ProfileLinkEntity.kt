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

package com.tunjid.heron.data.database.entities.profile

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.tunjid.heron.data.core.models.ProfileLink
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.ProfileLinkId
import com.tunjid.heron.data.core.types.ProfileLinkUri
import com.tunjid.heron.data.database.entities.LabelEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.asActiveExternalModels

@Entity(
    tableName = "profileLinks",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["profileId"]),
    ],
)
data class ProfileLinkEntity(
    @PrimaryKey
    val uri: ProfileLinkUri,
    val cid: ProfileLinkId,
    val profileId: ProfileId,
    val url: GenericUri,
    val title: String?,
    val icon: ImageUri?,
    val sortOrder: Int,
)

data class PopulatedProfileLinkEntity(
    @Embedded
    val entity: ProfileLinkEntity,
    @Relation(
        parentColumn = "uri",
        entityColumn = "uri",
    )
    val labelEntities: List<LabelEntity>,
)

fun PopulatedProfileLinkEntity.asExternalModel() =
    ProfileLink(
        uri = entity.uri,
        url = entity.url,
        title = entity.title,
        icon = entity.icon,
        labels = labelEntities.asActiveExternalModels(),
    )
