package com.tunjid.heron.data.network.models

import app.bsky.embed.ExternalViewExternal
import app.bsky.embed.GalleryViewItemUnion
import app.bsky.embed.ImagesViewImage
import app.bsky.embed.RecordWithMediaViewMediaUnion
import app.bsky.embed.VideoView
import app.bsky.feed.PostView
import app.bsky.feed.PostViewEmbedUnion
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.database.entities.postembeds.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.ImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostEmbed
import com.tunjid.heron.data.database.entities.postembeds.VideoEntity

/**
 * The hydrated `app.bsky.embed.external#viewExternal` of this post's embed, if any — used to
 * surface `associatedRefs` / `viewExternalSource` for standard-site record stubbing.
 */
internal fun PostView.externalAssociatedView(): ExternalViewExternal? =
    when (val embed = embed) {
        is PostViewEmbedUnion.ExternalView -> embed.value.external
        is PostViewEmbedUnion.RecordWithMediaView -> when (val media = embed.value.media) {
            is RecordWithMediaViewMediaUnion.ExternalView -> media.value.external
            else -> null
        }
        else -> null
    }

internal fun ExternalViewExternal.asExternalEmbedEntity() = ExternalEmbedEntity(
    uri = GenericUri(uri.uri),
    title = title,
    description = description,
    thumb = thumb?.uri?.let(::ImageUri),
    readingTime = readingTime,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun ImagesViewImage.imageEntity(): ImageEntity = ImageEntity(
    fullSize = ImageUri(fullsize.uri),
    thumb = ImageUri(thumb.uri),
    alt = alt,
    width = aspectRatio?.width,
    height = aspectRatio?.height,
)

internal fun VideoView.videoEntity(): VideoEntity =
    VideoEntity(
        cid = GenericId(cid.cid),
        playlist = GenericUri(playlist.uri),
        thumbnail = thumbnail?.uri?.let(::ImageUri),
        alt = alt,
        width = aspectRatio?.width,
        height = aspectRatio?.height,
    )

internal fun GalleryViewItemUnion.postEmbed(): PostEmbed? =
    when (this) {
        // At some point this will contain video.
        // USe the parent type to accommodate this
        is GalleryViewItemUnion.Unknown -> null
        is GalleryViewItemUnion.ViewImage -> ImageEntity(
            fullSize = ImageUri(value.fullsize.uri),
            thumb = ImageUri(value.thumbnail.uri),
            alt = value.alt,
            width = value.aspectRatio.width,
            height = value.aspectRatio.height,
        )
    }
