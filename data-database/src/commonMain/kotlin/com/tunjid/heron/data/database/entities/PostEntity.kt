/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.data.database.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.UnknownEmbed
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Entity(
    tableName = "posts",
)
data class PostEntity(
    @PrimaryKey
    val cid: Id,
    val uri: Uri?,
    val authorId: Id?,
    val replyCount: Long?,
    val repostCount: Long?,
    val likeCount: Long?,
    val quoteCount: Long?,
    val indexedAt: Instant,
//    public val record: JsonContent,
//    public val embed: PostViewEmbedUnion?,
//    public val viewer: ViewerState? = null,
//    public val labels: List<Label> = emptyList(),
//    public val threadgate: ThreadgateView? = null,
)

fun emptyPostEntity(
    id: Id
) = PostEntity(
    cid = id,
    uri = null,
    authorId = null,
    replyCount = null,
    repostCount = null,
    likeCount = null,
    quoteCount = null,
    indexedAt = Clock.System.now(),
)

fun PostEntity.asExternalModel(
    handle: Id
) = Post(
    cid = cid,
    uri = uri,
    author =
    if (authorId == null) null
    else Profile(
        did = authorId,
        handle = handle,
        displayName = null,
        description = null,
        avatar = null,
        banner = null,
        followersCount = null,
        followsCount = null,
        postsCount = null,
        joinedViaStarterPack = null,
        indexedAt = null,
        createdAt = null,
    ),
    replyCount = replyCount,
    repostCount = repostCount,
    likeCount = likeCount,
    quoteCount = quoteCount,
    indexedAt = indexedAt,
    embed = UnknownEmbed,
)

data class PopulatedPostEntity(
    @Embedded
    val entity: PostEntity,
    @Relation(
        parentColumn = "authorId",
        entityColumn = "did"
    )
    val author: ProfileEntity?,
    @Relation(
        parentColumn = "cid",
        entityColumn = "fullSize",
        associateBy = Junction(
            value = PostImageEntity::class,
            parentColumn = "postId",
            entityColumn = "imageUri",
        ),
    )
    val images: List<ImageEntity>,
    @Relation(
        parentColumn = "cid",
        entityColumn = "cid",
        associateBy = Junction(
            value = PostVideoEntity::class,
            parentColumn = "postId",
            entityColumn = "videoId",
        ),
    )
    val videos: List<VideoEntity>,
    @Relation(
        parentColumn = "cid",
        entityColumn = "uri",
        associateBy = Junction(
            value = PostExternalEmbedEntity::class,
            parentColumn = "postId",
            entityColumn = "externalEmbedUri",
        ),
    )
    val externalEmbeds: List<ExternalEmbedEntity>,
)

fun PopulatedPostEntity.asExternalModel() = Post(
    cid = entity.cid,
    uri = entity.uri,
    replyCount = entity.replyCount,
    repostCount = entity.repostCount,
    likeCount = entity.likeCount,
    quoteCount = entity.quoteCount,
    indexedAt = entity.indexedAt,
    author = author?.asExternalModel(),
    embed = when {
        externalEmbeds.isNotEmpty() -> externalEmbeds.first().asExternalModel()
        videos.isNotEmpty() -> videos.first().asExternalModel()
        images.isNotEmpty() -> ImageList(
            images = images.map(ImageEntity::asExternalModel)
        )

        else -> null
    },
)
