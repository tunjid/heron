package com.tunjid.heron.data.network.models

import app.bsky.feed.PostView
import app.bsky.feed.PostViewEmbedUnion
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.ImageEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.PostExternalEmbedEntity
import com.tunjid.heron.data.database.entities.PostImageEntity
import com.tunjid.heron.data.database.entities.PostVideoEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.VideoEntity


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
    )

internal fun PostView.profileEntity() =
    ProfileEntity(
        did = Id(author.did.did),
        handle = Id(author.handle.handle),
        displayName = author.displayName,
        description = null,
        avatar = author.avatar?.uri?.let(::Uri),
        banner = null,
        followersCount = 0,
        followsCount = 0,
        postsCount = 0,
        joinedViaStarterPack = null,
        indexedAt = null,
        createdAt = author.createdAt,
    )

internal fun PostView.embedEntities() =
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

        is PostViewEmbedUnion.RecordView -> emptyList()
        is PostViewEmbedUnion.RecordWithMediaView -> emptyList()
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