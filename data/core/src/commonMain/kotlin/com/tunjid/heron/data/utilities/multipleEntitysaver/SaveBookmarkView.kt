package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.bookmark.BookmarkView
import app.bsky.bookmark.BookmarkViewItemUnion
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.PostBookmarkEntity
import com.tunjid.heron.data.network.models.postEntity
import kotlinx.datetime.Clock

internal fun MultipleEntitySaver.add(
    viewingProfileId: ProfileId?,
    bookmarkView: BookmarkView,
) {
    viewingProfileId?.let { profileId ->
        addBookmark(
            viewingProfileId = profileId,
            bookmarkView = bookmarkView,
        )
    }
}

internal fun MultipleEntitySaver.addBookmark(
    viewingProfileId: ProfileId,
    bookmarkView: BookmarkView,
) {
    val postView = when (val item = bookmarkView.item) {
        is BookmarkViewItemUnion.PostView -> item.value
        is BookmarkViewItemUnion.BlockedPost,
        is BookmarkViewItemUnion.NotFoundPost,
        is BookmarkViewItemUnion.Unknown,
        -> return
    }

    add(
        viewingProfileId = viewingProfileId,
        postView = postView,
    )

    val postEntity = postView.postEntity()
    add(
        PostBookmarkEntity(
            postId = postEntity.cid,
            postUri = postEntity.uri,
            createdAt = bookmarkView.createdAt ?: Clock.System.now(),
            viewingProfileId = viewingProfileId,
        ),
    )
}
