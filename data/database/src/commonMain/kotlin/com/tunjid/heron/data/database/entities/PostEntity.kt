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
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
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
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["uri"]),
        Index(value = ["cid"]),
        Index(value = ["authorId"]),
    ],
)
data class PostEntity(
    val cid: PostId,
    @PrimaryKey
    val uri: PostUri,
    val authorId: ProfileId,
    val replyCount: Long?,
    val repostCount: Long?,
    val likeCount: Long?,
    val quoteCount: Long?,
    val bookmarkCount: Long?,
    val indexedAt: Instant,
    @Embedded
    val record: RecordData?,
) {
    data class RecordData(
        val text: String,
        val base64EncodedRecord: String?,
        val embeddedRecordUri: RecordUri?,
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
    bookmarkCount = null,
    indexedAt = Clock.System.now(),
    record = null,
)

data class PopulatedPostEntity(
    @Embedded
    val entity: PostEntity,
    @Relation(
        parentColumn = "authorId",
        entityColumn = "did",
    )
    val author: ProfileEntity?,
    @Embedded
    val viewerStats: PostViewerStatisticsEntity?,
    @Relation(
        parentColumn = "uri",
        entityColumn = "fullSize",
        associateBy = Junction(
            value = PostImageEntity::class,
            parentColumn = "postUri",
            entityColumn = "imageUri",
        ),
    )
    val images: List<ImageEntity>,
    @Relation(
        parentColumn = "uri",
        entityColumn = "cid",
        associateBy = Junction(
            value = PostVideoEntity::class,
            parentColumn = "postUri",
            entityColumn = "videoId",
        ),
    )
    val videos: List<VideoEntity>,
    @Relation(
        parentColumn = "uri",
        entityColumn = "uri",
        associateBy = Junction(
            value = PostExternalEmbedEntity::class,
            parentColumn = "postUri",
            entityColumn = "externalEmbedUri",
        ),
    )
    val externalEmbeds: List<ExternalEmbedEntity>,
    @Relation(
        parentColumn = "uri",
        entityColumn = "uri",
    )
    val labelEntities: List<LabelEntity>,
    @Relation(
        parentColumn = "authorId",
        entityColumn = "uri",
        associateBy = Junction(
            value = ProfileEntity::class,
            parentColumn = "did",
            entityColumn = "did",
        ),
    )
    val authorLabelEntities: List<LabelEntity>,
)

data class EmbeddedPopulatedPostEntity(
    @Embedded
    val entity: PopulatedPostEntity,
    val parentPostUri: PostUri,
    val embeddedPostUri: PostUri,
)

data class ThreadedPostEntity(
    @Embedded
    val entity: PostEntity,
    val generation: Long,
    val rootPostUri: PostUri?,
)

fun PopulatedPostEntity.asExternalModel(
    embeddedRecord: Record?,
) = Post(
    cid = entity.cid,
    uri = entity.uri,
    replyCount = entity.replyCount.orZero(),
    repostCount = entity.repostCount.orZero(),
    likeCount = entity.likeCount.orZero(),
    quoteCount = entity.quoteCount.orZero(),
    bookmarkCount = entity.bookmarkCount.orZero(),
    indexedAt = entity.indexedAt,
    author = author.asExternalModel(
        labels = authorLabelEntities.map(LabelEntity::asExternalModel),
    ),
    embed = when {
        externalEmbeds.isNotEmpty() -> externalEmbeds.first().asExternalModel()
        videos.isNotEmpty() -> videos.first().asExternalModel()
        images.isNotEmpty() -> ImageList(
            images = images.map(ImageEntity::asExternalModel),
        )

        else -> null
    },
    quote = null,
    record = entity.record?.asExternalModel(),
    viewerStats = viewerStats?.asExternalModel(),
    labels = labelEntities.map(LabelEntity::asExternalModel),
    embeddedRecord = embeddedRecord,
)

fun PostViewerStatisticsEntity.asExternalModel() =
    Post.ViewerStats(
        likeUri = likeUri,
        repostUri = repostUri,
        threadMuted = threadMuted,
        replyDisabled = replyDisabled,
        embeddingDisabled = embeddingDisabled,
        pinned = pinned,
        bookmarked = bookmarked,
    )

fun PostEntity.RecordData.asExternalModel(): Post.Record? =
    base64EncodedRecord
        ?.fromBase64EncodedUrl<Post.Record>()

private fun Long?.orZero() = this ?: 0L
