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
import com.tunjid.heron.data.core.models.ProfileVerification
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.ProfileVerificationUri
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import kotlin.time.Instant

@Entity(
    tableName = "profileVerifications",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["verifiedProfileId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["issuerProfileId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["verifiedProfileId"]),
        Index(value = ["issuerProfileId"]),
        Index(value = ["createdAt"]),
    ],
)
data class ProfileVerificationEntity(
    @PrimaryKey
    val uri: ProfileVerificationUri,
    val verifiedProfileId: ProfileId,
    val issuerProfileId: ProfileId,
    val isValid: Boolean,
    val createdAt: Instant,
)

data class PopulatedProfileVerificationEntity(
    @Embedded
    val entity: ProfileVerificationEntity,
    @Relation(
        parentColumn = "issuerProfileId",
        entityColumn = "did",
    )
    val issuer: ProfileEntity?,
    @Relation(
        parentColumn = "verifiedProfileId",
        entityColumn = "did",
    )
    val subject: ProfileEntity?,
)

fun PopulatedProfileVerificationEntity.asExternalModel() =
    ProfileVerification(
        uri = entity.uri,
        issuer = issuer.asExternalModel(),
        subject = subject.asExternalModel(),
        isValid = entity.isValid,
        createdAt = entity.createdAt,
    )
