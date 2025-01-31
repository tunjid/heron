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

package com.tunjid.heron.data.network.models

import app.bsky.feed.PostView
import app.bsky.feed.PostViewEmbedUnion
import app.bsky.feed.ReplyRefParentUnion
import app.bsky.feed.ReplyRefRootUnion
import app.bsky.feed.ViewerState
import app.bsky.richtext.Facet
import app.bsky.richtext.FacetFeatureUnion
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.toUrlEncodedBase64
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.postembeds.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.ImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostEmbed
import com.tunjid.heron.data.database.entities.postembeds.PostExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.PostImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostVideoEntity
import com.tunjid.heron.data.database.entities.postembeds.VideoEntity
import com.tunjid.heron.data.database.entities.postembeds.asExternalModel
import com.tunjid.heron.data.database.entities.profile.PostViewerStatisticsEntity
import sh.christian.ozone.api.model.JsonContent
import app.bsky.feed.Post as BskyPost


internal fun PostEntity.postVideoEntity(
    embedEntity: VideoEntity,
) = PostVideoEntity(
    postId = cid,
    videoId = embedEntity.cid,
)

internal fun PostEntity.postImageEntity(
    embedEntity: ImageEntity,
) = PostImageEntity(
    postId = cid,
    imageUri = embedEntity.fullSize,
)

internal fun PostEntity.postExternalEmbedEntity(
    embedEntity: ExternalEmbedEntity,
) = PostExternalEmbedEntity(
    postId = cid,
    externalEmbedUri = embedEntity.uri,
)

internal fun PostView.post(): Post {
    val postEntity = postEntity()
    val quotedPostEntity = quotedPostEntity()
    val quotedPostProfileEntity = quotedPostProfileEntity()
    return post(
        postEntity = postEntity,
        profileEntity = profileEntity(),
        embeds = embedEntities(),
        viewerStatisticsEntity = viewer
            ?.postViewerStatisticsEntity(postEntity.cid),
        quote = if (quotedPostEntity != null && quotedPostProfileEntity != null) post(
            postEntity = quotedPostEntity,
            profileEntity = quotedPostProfileEntity,
            embeds = quotedPostEmbedEntities(),
            viewerStatisticsEntity = null,
            quote = null,
        ) else null
    )
}

private fun post(
    postEntity: PostEntity,
    profileEntity: ProfileEntity,
    embeds: List<PostEmbed>,
    viewerStatisticsEntity: PostViewerStatisticsEntity?,
    quote: Post?,
) = Post(
    cid = postEntity.cid,
    uri = postEntity.uri,
    author = profileEntity.asExternalModel(),
    replyCount = postEntity.replyCount.orZero(),
    repostCount = postEntity.repostCount.orZero(),
    likeCount = postEntity.likeCount.orZero(),
    quoteCount = postEntity.quoteCount.orZero(),
    indexedAt = postEntity.indexedAt,
    record = postEntity.record?.asExternalModel(),
    embed = when (val embedEntity = embeds.firstOrNull()) {
        is ExternalEmbedEntity -> embedEntity.asExternalModel()
        is ImageEntity -> ImageList(
            images = embeds.filterIsInstance<ImageEntity>()
                .map(ImageEntity::asExternalModel)
        )

        is VideoEntity -> embedEntity.asExternalModel()
        null -> null
    },
    quote = quote,
    viewerStats = viewerStatisticsEntity?.asExternalModel()
)

internal fun PostView.postEntity() =
    PostEntity(
        cid = Id(cid.cid),
        uri = Uri(uri.atUri),
        authorId = Id(author.did.did),
        replyCount = replyCount,
        repostCount = repostCount,
        likeCount = likeCount,
        quoteCount = quoteCount,
        indexedAt = indexedAt,
        record = record.asPostEntityRecordData(),
    )

internal fun PostView.profileEntity(): ProfileEntity =
    author.profileEntity()

internal fun PostView.embedEntities(): List<PostEmbed> =
    when (val embed = embed) {
        is PostViewEmbedUnion.ExternalView -> listOf(
            ExternalEmbedEntity(
                uri = Uri(embed.value.external.uri.uri),
                title = embed.value.external.title,
                description = embed.value.external.description,
                thumb = embed.value.external.thumb?.uri?.let(::Uri),
            )
        )

        is PostViewEmbedUnion.ImagesView -> embed.value.images.map {
            ImageEntity(
                fullSize = Uri(it.fullsize.uri),
                thumb = Uri(it.thumb.uri),
                alt = it.alt,
                width = it.aspectRatio?.width,
                height = it.aspectRatio?.height,
            )
        }

        is PostViewEmbedUnion.RecordView -> embed.embedEntities()
        is PostViewEmbedUnion.RecordWithMediaView -> embed.embedEntities()
        is PostViewEmbedUnion.Unknown -> emptyList()
        is PostViewEmbedUnion.VideoView -> listOf(
            VideoEntity(
                cid = Id(embed.value.cid.cid),
                playlist = Uri(embed.value.playlist.uri),
                thumbnail = embed.value.thumbnail?.uri?.let(::Uri),
                alt = embed.value.alt,
                width = embed.value.aspectRatio?.width,
                height = embed.value.aspectRatio?.height,
            )
        )

        null -> emptyList()
    }


internal fun ViewerState.postViewerStatisticsEntity(
    postId: Id,
) = PostViewerStatisticsEntity(
    postId = postId,
    likeUri = like?.atUri?.let(::Uri),
    repostUri = repost?.atUri?.let(::Uri),
    threadMuted = threadMuted == true,
    replyDisabled = replyDisabled == true,
    embeddingDisabled = embeddingDisabled == true,
    pinned = pinned == true,
)

internal fun ReplyRefRootUnion.postViewerStatisticsEntity() = when (this) {
    is ReplyRefRootUnion.PostView -> value.viewer?.postViewerStatisticsEntity(
        postId = Id(value.cid.cid),
    )

    is ReplyRefRootUnion.BlockedPost,
    is ReplyRefRootUnion.NotFoundPost,
    is ReplyRefRootUnion.Unknown,
        -> null
}

internal fun ReplyRefParentUnion.postViewerStatisticsEntity() = when (this) {
    is ReplyRefParentUnion.PostView -> value.viewer?.postViewerStatisticsEntity(
        postId = Id(value.cid.cid),
    )

    is ReplyRefParentUnion.BlockedPost,
    is ReplyRefParentUnion.NotFoundPost,
    is ReplyRefParentUnion.Unknown,
        -> null
}

internal fun JsonContent.asPostEntityRecordData(): PostEntity.RecordData? =
    // TODO can this be deterministic?
    try {
        val bskyPost = decodeAs<BskyPost>()
        PostEntity.RecordData(
            text = bskyPost.text,
            base64EncodedRecord = bskyPost.toPostRecord().toUrlEncodedBase64(),
            createdAt = bskyPost.createdAt,
        )
    } catch (e: Exception) {
        null
    }

private fun BskyPost.toPostRecord() =
    Post.Record(
        text = text,
        createdAt = createdAt,
        links = facets.mapNotNull(Facet::toLinkOrNull),
        replyRef = reply?.let {
            Post.ReplyRef(
                rootCid = it.root.cid.cid.let(::Id),
                rootUri = it.root.uri.atUri.let(::Uri),
                parentCid = it.parent.cid.cid.let(::Id),
                parentUri = it.parent.uri.atUri.let(::Uri),
            )
        },
    )

private fun Facet.toLinkOrNull(): Post.Link? {
    return if (features.isEmpty()) null else Post.Link(
        start = index.byteStart.toInt(),
        end = index.byteEnd.toInt(),
        target = when (val feature = features.first()) {
            is FacetFeatureUnion.Link -> Post.LinkTarget.ExternalLink(feature.value.uri.uri.let(::Uri))
            is FacetFeatureUnion.Mention -> Post.LinkTarget.UserDidMention(
                feature.value.did.did.let(
                    ::Id
                )
            )

            is FacetFeatureUnion.Tag -> Post.LinkTarget.Hashtag(feature.value.tag)
            is FacetFeatureUnion.Unknown -> return null
        },
    )
}

private fun Long?.orZero() = this ?: 0L