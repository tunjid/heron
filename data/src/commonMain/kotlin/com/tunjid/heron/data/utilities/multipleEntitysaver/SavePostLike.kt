package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.feed.GetLikesLike
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.database.entities.PostLikeEntity

internal fun MultipleEntitySaver.add(
    viewingProfileId: Id,
    postId: Id,
    like: GetLikesLike,
) {
    add(
        PostLikeEntity(
            postId = postId,
            authorId = like.actor.did.did.let(::Id),
            createdAt = like.createdAt,
            indexedAt = like.indexedAt,
        )
    )
    add(
        viewingProfileId = viewingProfileId,
        profileView = like.actor,
    )
}