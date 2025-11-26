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
import androidx.room.Junction
import androidx.room.Relation
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ThreadGate
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ThreadGateId
import com.tunjid.heron.data.core.types.ThreadGateUri
import kotlinx.datetime.Instant

@Entity(
    tableName = "threadGates",
    primaryKeys = [
        "uri",
    ],
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["uri"],
            childColumns = ["gatedPostUri"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["uri"]),
        Index(value = ["cid"]),
        Index(value = ["gatedPostUri"]),
    ],
)
data class ThreadGateEntity(
    val cid: ThreadGateId,
    val uri: ThreadGateUri,
    val gatedPostUri: PostUri,
    val createdAt: Instant,
    @Embedded
    val allowed: Allowed?,
) {
    data class Allowed(
        val allowsFollowing: Boolean,
        val allowsFollowers: Boolean,
        val allowsMentioned: Boolean,
    )
}

data class PopulatedThreadGateEntity(
    @Embedded
    val entity: ThreadGateEntity,
    @Relation(
        parentColumn = "uri",
        entityColumn = "uri",
        associateBy = Junction(
            value = ThreadGateAllowedListEntity::class,
            parentColumn = "threadGateUri",
            entityColumn = "allowedListUri",
        ),
    )
    val allowedLists: List<ListEntity>,
    @Relation(
        parentColumn = "uri",
        entityColumn = "uri",
        associateBy = Junction(
            value = ThreadGateHiddenPostEntity::class,
            parentColumn = "threadGateUri",
            entityColumn = "hiddenPostUri",
        ),
    )
    val hiddenPosts: List<PostEntity>,
)

fun PopulatedThreadGateEntity.asExternalModel(
    creator: Profile,
) = ThreadGate(
    uri = entity.uri,
    gatedPostUri = entity.gatedPostUri,
    allowed = entity.allowed?.let { allowed ->
        ThreadGate.Allowed(
            allowsFollowing = allowed.allowsFollowing,
            allowsFollowers = allowed.allowsFollowers,
            allowsMentioned = allowed.allowsMentioned,
            allowedLists = allowedLists.map { listEntity ->
                listEntity.asExternalModel(
                    creator = creator,
                    labels = emptyList(),
                )
            },
        )
    },
)
