package com.tunjid.heron.data.network.models

import app.bsky.embed.ExternalViewExternal
import app.bsky.embed.RecordWithMediaViewMediaUnion
import app.bsky.feed.PostView
import app.bsky.feed.PostViewEmbedUnion
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.database.entities.postembeds.ExternalEmbedEntity

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
