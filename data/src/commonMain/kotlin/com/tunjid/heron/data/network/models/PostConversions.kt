package com.tunjid.heron.data.network.models

import app.bsky.feed.PostView
import app.bsky.feed.PostViewEmbedUnion
import app.bsky.feed.ReplyRefParentUnion
import app.bsky.feed.ReplyRefRootUnion
import app.bsky.feed.ViewerState
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.postembeds.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.ImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostEmbed
import com.tunjid.heron.data.database.entities.postembeds.PostExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.PostImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostVideoEntity
import com.tunjid.heron.data.database.entities.postembeds.VideoEntity
import com.tunjid.heron.data.database.entities.profile.PostViewerStatisticsEntity
import sh.christian.ozone.api.model.JsonContent
import app.bsky.feed.Post as BskyPost


internal fun PostEntity.postVideoEntity(
    embedEntity: VideoEntity
) = PostVideoEntity(
    postId = cid,
    videoId = embedEntity.cid,
)

internal fun PostEntity.postImageEntity(
    embedEntity: ImageEntity
) = PostImageEntity(
    postId = cid,
    imageUri = embedEntity.fullSize,
)

internal fun PostEntity.postExternalEmbedEntity(
    embedEntity: ExternalEmbedEntity
) = PostExternalEmbedEntity(
    postId = cid,
    externalEmbedUri = embedEntity.uri,
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
    liked = like != null,
    reposted = repost != null,
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
    is ReplyRefRootUnion.Unknown -> null
}

internal fun ReplyRefParentUnion.postViewerStatisticsEntity() = when (this) {
    is ReplyRefParentUnion.PostView -> value.viewer?.postViewerStatisticsEntity(
        postId = Id(value.cid.cid),
    )

    is ReplyRefParentUnion.BlockedPost,
    is ReplyRefParentUnion.NotFoundPost,
    is ReplyRefParentUnion.Unknown -> null
}

internal fun JsonContent.asPostEntityRecordData(): PostEntity.RecordData? =
    // TODO can this be deterministic?
    try {
        val bskyPost = decodeAs<BskyPost>()
        PostEntity.RecordData(
            text = bskyPost.text,
            createdAt = bskyPost.createdAt,
        )
    } catch (e: Exception) {
        null
    }
