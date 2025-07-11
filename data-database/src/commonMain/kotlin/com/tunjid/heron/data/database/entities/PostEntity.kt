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
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.postembeds.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.ImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.PostImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostVideoEntity
import com.tunjid.heron.data.database.entities.postembeds.VideoEntity
import com.tunjid.heron.data.database.entities.postembeds.asExternalModel
import com.tunjid.heron.data.database.entities.profile.PostViewerStatisticsEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Entity(
    tableName = "posts",
)
data class PostEntity(
    @PrimaryKey
    val cid: PostId,
    val uri: PostUri,
    val authorId: ProfileId,
    val replyCount: Long?,
    val repostCount: Long?,
    val likeCount: Long?,
    val quoteCount: Long?,
    val indexedAt: Instant,
    @Embedded
    val record: RecordData?,
//    public val labels: List<Label> = emptyList(),
//    public val threadgate: ThreadgateView? = null,
) {
    data class RecordData(
        val text: String,
        val base64EncodedRecord: String?,
        val createdAt: Instant,
    )
}

fun emptyPostEntity(
    id: PostId,
    uri: PostUri,
    authorId: ProfileId,
) = PostEntity(
    cid = id,
    uri = uri,
    authorId = authorId,
    replyCount = null,
    repostCount = null,
    likeCount = null,
    quoteCount = null,
    indexedAt = Clock.System.now(),
    record = null,
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
        entityColumn = "postId",
    )
    val viewerStats: PostViewerStatisticsEntity?,
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

data class EmbeddedPopulatedPostEntity(
    @Embedded
    val entity: PopulatedPostEntity,
    val postId: PostId,
    val embeddedPostId: PostId,
)

data class ThreadedPostEntity(
    @Embedded
    val entity: PostEntity,
    val generation: Long,
    val postId: PostId,
    val parentPostId: PostId?,
    val rootPostId: PostId?,
    val sort1: Long?,
)

fun PopulatedPostEntity.asExternalModel(
    quote: Post?,
) = Post(
    cid = entity.cid,
    uri = entity.uri,
    replyCount = entity.replyCount.orZero(),
    repostCount = entity.repostCount.orZero(),
    likeCount = entity.likeCount.orZero(),
    quoteCount = entity.quoteCount.orZero(),
    indexedAt = entity.indexedAt,
    author = author.asExternalModel(),
    embed = when {
        externalEmbeds.isNotEmpty() -> externalEmbeds.first().asExternalModel()
        videos.isNotEmpty() -> videos.first().asExternalModel()
        images.isNotEmpty() -> ImageList(
            images = images.map(ImageEntity::asExternalModel)
        )

        else -> null
    },
    quote = quote,
    record = entity.record?.asExternalModel(),
    viewerStats = viewerStats?.asExternalModel(),
)

fun PostViewerStatisticsEntity.asExternalModel() =
    Post.ViewerStats(
        likeUri = likeUri,
        repostUri = repostUri,
        threadMuted = threadMuted,
        replyDisabled = replyDisabled,
        embeddingDisabled = embeddingDisabled,
        pinned = pinned,
    )

fun PostEntity.RecordData.asExternalModel(): Post.Record? =
    base64EncodedRecord
        ?.fromBase64EncodedUrl<Post.Record>()

private fun Long?.orZero() = this ?: 0L