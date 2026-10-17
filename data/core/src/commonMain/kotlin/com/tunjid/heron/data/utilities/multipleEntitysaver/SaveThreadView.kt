/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.feed.ThreadViewPost
import app.bsky.feed.ThreadViewPostParentUnion
import app.bsky.feed.ThreadViewPostReplieUnion
import app.bsky.unspecced.GetPostThreadV2ThreadItem
import app.bsky.unspecced.GetPostThreadV2ThreadItemValueUnion
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.PostThreadEntity
import com.tunjid.heron.data.database.entities.postThreadDetailsEntityOrNull
import com.tunjid.heron.data.network.models.replyParentPostUri

internal fun MultipleEntitySaver.add(
    viewingProfileId: ProfileId?,
    threadViewPost: ThreadViewPost,
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
            null,
            -> null
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
        ?.filterIsInstance<ThreadViewPostReplieUnion.ThreadViewPost>()
        ?.forEach {
            addThreadReply(
                viewingProfileId = viewingProfileId,
                parent = threadViewPost,
                reply = it.value,
            )
        }
}

internal fun MultipleEntitySaver.add(
    viewingProfileId: ProfileId?,
    threadItems: List<GetPostThreadV2ThreadItem>,
) {
    val threadItemPosts = threadItems.mapNotNull { item ->
        when (val value = item.value) {
            is GetPostThreadV2ThreadItemValueUnion.Post -> value.value
            // TODO: Deal with deleted, blocked or removed posts
            is GetPostThreadV2ThreadItemValueUnion.Blocked,
            is GetPostThreadV2ThreadItemValueUnion.NoUnauthenticated,
            is GetPostThreadV2ThreadItemValueUnion.NotFound,
            is GetPostThreadV2ThreadItemValueUnion.Unknown,
            -> null
        }
    }
    val threadPostUris = threadItemPosts.mapTo(mutableSetOf()) { threadItemPost ->
        PostUri(threadItemPost.post.uri.atUri)
    }

    threadItemPosts.forEach { threadItemPost ->
        add(
            viewingProfileId = viewingProfileId,
            postView = threadItemPost.post,
        )
        postThreadDetailsEntityOrNull(
            postUri = PostUri(threadItemPost.post.uri.atUri),
            postIndex = threadItemPost.opThreadPostIndex,
            postCount = threadItemPost.opThreadPostCount,
        )?.let(::add)

        val parentPostUri = threadItemPost.post.replyParentPostUri()
            // Only link posts within the response; both ends of an edge must be saved.
            ?.takeIf(threadPostUris::contains)
            ?: return@forEach

        add(
            PostThreadEntity(
                postUri = PostUri(threadItemPost.post.uri.atUri),
                parentPostUri = parentPostUri,
            ),
        )
    }
}

private fun MultipleEntitySaver.addThreadParent(
    viewingProfileId: ProfileId?,
    parentPost: ThreadViewPost,
    childPost: ThreadViewPost?,
) {
    add(
        viewingProfileId = viewingProfileId,
        postView = parentPost.post,
    )
    if (childPost is ThreadViewPost) add(
        PostThreadEntity(
            postUri = childPost.post.uri.atUri.let(::PostUri),
            parentPostUri = parentPost.post.uri.atUri.let(::PostUri),
        ),
    )
    parentPost.replies
        // TODO: Deal with deleted, blocked or removed posts
        ?.filterIsInstance<ThreadViewPostReplieUnion.ThreadViewPost>()
        ?.forEach {
            addThreadReply(
                viewingProfileId = viewingProfileId,
                parent = parentPost,
                reply = it.value,
            )
        }
}

private fun MultipleEntitySaver.addThreadReply(
    viewingProfileId: ProfileId?,
    reply: ThreadViewPost,
    parent: ThreadViewPost,
) {
    add(
        viewingProfileId = viewingProfileId,
        postView = reply.post,
    )
    add(
        PostThreadEntity(
            postUri = reply.post.uri.atUri.let(::PostUri),
            parentPostUri = parent.post.uri.atUri.let(::PostUri),
        ),
    )
    reply.replies
        // TODO: Deal with deleted, blocked or removed posts
        ?.filterIsInstance<ThreadViewPostReplieUnion.ThreadViewPost>()
        ?.forEach {
            addThreadReply(
                viewingProfileId = viewingProfileId,
                parent = reply,
                reply = it.value,
            )
        }
}
