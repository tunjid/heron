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

internal fun imageEntity(
    index: Int,
    imagesViewImage: ImagesViewImage,
): ImageEntity = ImageEntity(
    fullSize = ImageUri(imagesViewImage.fullsize.uri),
    thumb = ImageUri(imagesViewImage.thumb.uri),
    alt = imagesViewImage.alt,
    width = imagesViewImage.aspectRatio?.width,
    height = imagesViewImage.aspectRatio?.height,
    index = index,
)

internal fun videoEntity(
    index: Int,
    videoView: VideoView,
): VideoEntity =
    VideoEntity(
        cid = GenericId(videoView.cid.cid),
        playlist = GenericUri(videoView.playlist.uri),
        thumbnail = videoView.thumbnail?.uri?.let(::ImageUri),
        alt = videoView.alt,
        width = videoView.aspectRatio?.width,
        height = videoView.aspectRatio?.height,
        index = index,
    )

internal fun postEmbed(
    index: Int,
    galleryViewItemUnion: GalleryViewItemUnion,
): PostEmbed? =
    when (galleryViewItemUnion) {
        // At some point this will contain video.
        // USe the parent type to accommodate this
        is GalleryViewItemUnion.Unknown -> null
        is GalleryViewItemUnion.ViewImage -> ImageEntity(
            fullSize = ImageUri(galleryViewItemUnion.value.fullsize.uri),
            thumb = ImageUri(galleryViewItemUnion.value.thumbnail.uri),
            alt = galleryViewItemUnion.value.alt,
            width = galleryViewItemUnion.value.aspectRatio.width,
            height = galleryViewItemUnion.value.aspectRatio.height,
            index = index,
        )
    }
