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

import androidx.room.Entity
import androidx.room.ForeignKey
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.ProfileEntity

@Entity(
    tableName = "profileViewerStates",
    primaryKeys = [
        "profileId",
        "otherProfileId",
    ],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["otherProfileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProfileViewerStateEntity(
    val profileId: Id,
    val otherProfileId: Id,
    val muted: Boolean?,
    val mutedByList: Id?,
    val blockedBy: Boolean?,
    val blocking: Uri?,
    val blockingByList: Id?,
    val following: Uri?,
    val followedBy: Uri?,
) {
    data class Partial(
        val profileId: Id,
        val otherProfileId: Id,
        val following: Uri?,
        val followedBy: Uri?,
    )
}

internal fun ProfileViewerStateEntity.partial() =
    ProfileViewerStateEntity.Partial(
        profileId = profileId,
        otherProfileId = otherProfileId,
        following = following,
        followedBy = followedBy,
    )

fun ProfileViewerStateEntity.asExternalModel() =
    ProfileViewerState(
        following = following,
        followedBy = followedBy,
    )
