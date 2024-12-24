package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.feed.ThreadViewPost
import app.bsky.feed.ThreadViewPostParentUnion
import app.bsky.feed.ThreadViewPostReplieUnion
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.database.entities.PostThreadEntity

internal fun MultipleEntitySaver.add(
    viewingProfileId: Id,
    threadViewPost: ThreadViewPost
) {
    add(
        viewingProfileId = viewingProfileId,
        postView = threadViewPost.post,
    )
    generateSequence(threadViewPost) {
        when (val parent = it.parent) {
            is ThreadViewPostParentUnion.ThreadViewPost -> parent.value
            // TODO: Deal with deleted, blocked or removed posts
            is ThreadViewPostParentUnion.BlockedPost,
            is ThreadViewPostParentUnion.NotFoundPost,
            is ThreadViewPostParentUnion.Unknown,
            null -> null
        }
    }
        .windowed(
            size = 2,
            step = 1,
        )
        .forEach { window ->
            if (window.size == 1) addThreadParent(
                viewingProfileId = viewingProfileId,
                childPost = null,
                parentPost = window[0],
            )
            else addThreadParent(
                viewingProfileId = viewingProfileId,
                childPost = window[0],
                parentPost = window[1],
            )
        }

    threadViewPost.replies
        // TODO: Handle blocks and deletions
        .filterIsInstance<ThreadViewPostReplieUnion.ThreadViewPost>()
        .forEach {
            addThreadReply(
                viewingProfileId = viewingProfileId,
                parent = threadViewPost,
                reply = it.value,
            )
        }
}

private fun MultipleEntitySaver.addThreadParent(
    viewingProfileId: Id,
    parentPost: ThreadViewPost,
    childPost: ThreadViewPost?,
) {
    add(
        viewingProfileId = viewingProfileId,
        postView = parentPost.post,
    )
    if (childPost is ThreadViewPost) add(
        PostThreadEntity(
            postId = childPost.post.cid.cid.let(::Id),
            parentPostId = parentPost.post.cid.cid.let(::Id),
        )
    )
    parentPost.replies
        // TODO: Deal with deleted, blocked or removed posts
        .filterIsInstance<ThreadViewPostReplieUnion.ThreadViewPost>()
        .forEach {
            addThreadReply(
                viewingProfileId = viewingProfileId,
                parent = parentPost,
                reply = it.value,
            )
        }
}

private fun MultipleEntitySaver.addThreadReply(
    viewingProfileId: Id,
    reply: ThreadViewPost,
    parent: ThreadViewPost,
) {
    add(
        viewingProfileId = viewingProfileId,
        postView = reply.post,
    )
    add(
        PostThreadEntity(
            postId = reply.post.cid.cid.let(::Id),
            parentPostId = parent.post.cid.cid.let(::Id),
        )
    )
    reply.replies
        // TODO: Deal with deleted, blocked or removed posts
        .filterIsInstance<ThreadViewPostReplieUnion.ThreadViewPost>()
        .forEach {
            addThreadReply(
                viewingProfileId = viewingProfileId,
                parent = reply,
                reply = it.value,
            )
        }
}