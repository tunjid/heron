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
import com.tunjid.heron.data.core.types.PostId

@Stable
interface PostActions {
    fun onLinkTargetClicked(post: Post, linkTarget: LinkTarget)

    fun onProfileClicked(profile: Profile, post: Post, quotingPostId: PostId?)
    fun onPostClicked(post: Post, quotingPostId: PostId?)
    fun onPostMediaClicked(media: Embed.Media, index: Int, post: Post, quotingPostId: PostId?)
    fun onReplyToPost(post: Post)
    fun onPostInteraction(interaction: Post.Interaction)
    fun onPostMetadataClicked(metadata: Post.Metadata)
}

fun postActions(
    onLinkTargetClicked: (post: Post, linkTarget: LinkTarget) -> Unit,
    onProfileClicked: (profile: Profile, post: Post, quotingPostId: PostId?) -> Unit,
    onPostClicked: (post: Post, quotingPostId: PostId?) -> Unit,
    onPostMediaClicked: (media: Embed.Media, index: Int, post: Post, quotingPostId: PostId?) -> Unit,
    onReplyToPost: (post: Post) -> Unit,
    onPostInteraction: (interaction: Post.Interaction) -> Unit,
    onPostMetadataClicked: (metadata: Post.Metadata) -> Unit = {},
) = object : PostActions {
    override fun onProfileClicked(
        profile: Profile,
        post: Post,
        quotingPostId: PostId?,
    ) = onProfileClicked(
        profile,
        post,
        quotingPostId
    )

    override fun onPostClicked(
        post: Post,
        quotingPostId: PostId?,
    ) = onPostClicked(
        post,
        quotingPostId
    )

    override fun onPostMediaClicked(
        media: Embed.Media,
        index: Int,
        post: Post,
        quotingPostId: PostId?,
    ) = onPostMediaClicked(
        media,
        index,
        post,
        quotingPostId
    )

    override fun onReplyToPost(
        post: Post,
    ) = onReplyToPost(post)

    override fun onPostInteraction(
        interaction: Post.Interaction,
    ) = onPostInteraction(
        interaction
    )

    override fun onPostMetadataClicked(
        metadata: Post.Metadata,
    ) = onPostMetadataClicked(
        metadata
    )

    override fun onLinkTargetClicked(
        post: Post,
        linkTarget: LinkTarget
    ) = onLinkTargetClicked(
        post,
        linkTarget,
    )
}
