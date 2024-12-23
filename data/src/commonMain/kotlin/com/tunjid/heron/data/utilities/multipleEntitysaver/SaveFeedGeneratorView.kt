package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.feed.GeneratorView
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.FeedGeneratorEntity
import com.tunjid.heron.data.network.models.profileEntity

internal fun MultipleEntitySaver.add(
    feedGeneratorView: GeneratorView,
) {
    feedGeneratorView.creator.profileEntity().let(::add)
    add(
        FeedGeneratorEntity(
            cid = feedGeneratorView.cid.cid.let(::Id),
            did = feedGeneratorView.did.did.let(::Id),
            uri = feedGeneratorView.uri.atUri.let(::Uri),
            creatorId = feedGeneratorView.creator.did.did.let(::Id),
            displayName = feedGeneratorView.displayName,
            description = feedGeneratorView.description,
            avatar = feedGeneratorView.avatar?.uri?.let(::Uri),
            likeCount = feedGeneratorView.likeCount,
            acceptsInteractions = feedGeneratorView.acceptsInteractions,
            indexedAt = feedGeneratorView.indexedAt,
        )
    )
}