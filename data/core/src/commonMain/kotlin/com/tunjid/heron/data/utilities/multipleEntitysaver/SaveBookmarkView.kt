package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.bookmark.BookmarkView
import app.bsky.bookmark.BookmarkViewItemUnion
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.BookmarkEntity
import kotlin.time.Clock

internal fun MultipleEntitySaver.add(viewingProfileId: ProfileId, bookmarkView: BookmarkView) {
    val postView =
        when (val item = bookmarkView.item) {
            is BookmarkViewItemUnion.PostView -> item.value
            is BookmarkViewItemUnion.BlockedPost,
            is BookmarkViewItemUnion.NotFoundPost,
            is BookmarkViewItemUnion.Unknown -> return
        }

    add(viewingProfileId = viewingProfileId, postView = postView)

    add(
        BookmarkEntity(
            bookmarkedUri = bookmarkView.subject.uri.atUri.let(::GenericUri),
            createdAt = bookmarkView.createdAt ?: Clock.System.now(),
            viewingProfileId = viewingProfileId,
        )
    )
}
