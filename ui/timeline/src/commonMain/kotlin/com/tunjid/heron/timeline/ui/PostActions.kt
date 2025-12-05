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

package com.tunjid.heron.timeline.ui

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.timeline.ui.post.PostMetadata

sealed interface PostAction {
    data class LinkTargetClicked(
        val post: Post,
        val linkTarget: LinkTarget,
    ) : PostAction

    data class ProfileClicked(
        val profile: Profile,
        val post: Post,
        val quotingPostUri: PostUri?,
    ) : PostAction

    data class PostClicked(
        val post: Post,
    ) : PostAction

    data class PostRecordClicked(
        val record: Record,
        val owningPostUri: PostUri,
    ) : PostAction

    data class PostMediaClicked(
        val media: Embed.Media,
        val index: Int,
        val post: Post,
        val quotingPostUri: PostUri?,
    ) : PostAction

    data class ReplyToPost(
        val post: Post,
    ) : PostAction

    data class PostInteraction(
        val interaction: Post.Interaction,
        val viewerStats: Post.ViewerStats?,
    ) : PostAction

    data class PostMetadataClicked(
        val metadata: PostMetadata,
    ) : PostAction

    data class PostOptionsClicked(
        val post: Post,
    ) : PostAction
}

@Stable
fun interface PostActions {
    fun onPostAction(action: PostAction)

    companion object {
        val NoOp: PostActions = PostActions {}
    }
}

fun postActions(
    onLinkTargetClicked: (post: Post, linkTarget: LinkTarget) -> Unit,
    onProfileClicked: (profile: Profile, post: Post, quotingPostUri: PostUri?) -> Unit,
    onPostClicked: (post: Post) -> Unit,
    onPostRecordClicked: (record: Record, owningPostUri: PostUri) -> Unit,
    onPostMediaClicked: (media: Embed.Media, index: Int, post: Post, quotingPostUri: PostUri?) -> Unit,
    onReplyToPost: (post: Post) -> Unit,
    onPostInteraction: (interaction: Post.Interaction, viewerStats: Post.ViewerStats?) -> Unit,
    onPostMetadataClicked: (metadata: PostMetadata) -> Unit = {},
    onPostOptionsClicked: (post: Post) -> Unit,
): PostActions = PostActions { action ->
    when (action) {
        is PostAction.LinkTargetClicked -> onLinkTargetClicked(
            action.post,
            action.linkTarget
        )

        is PostAction.ProfileClicked -> onProfileClicked(
            action.profile,
            action.post,
            action.quotingPostUri
        )

        is PostAction.PostClicked -> onPostClicked(
            action.post
        )

        is PostAction.PostRecordClicked -> onPostRecordClicked(
            action.record,
            action.owningPostUri
        )

        is PostAction.PostMediaClicked -> onPostMediaClicked(
            action.media,
            action.index,
            action.post,
            action.quotingPostUri
        )

        is PostAction.ReplyToPost -> onReplyToPost(
            action.post
        )

        is PostAction.PostInteraction -> onPostInteraction(
            action.interaction,
            action.viewerStats
        )

        is PostAction.PostMetadataClicked -> onPostMetadataClicked(
            action.metadata
        )

        is PostAction.PostOptionsClicked -> onPostOptionsClicked(
            action.post
        )
    }
}
