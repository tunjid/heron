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

@Stable
interface PostActions {
    fun onLinkTargetClicked(post: Post, linkTarget: LinkTarget)
    fun onProfileClicked(profile: Profile, post: Post, quotingPostUri: PostUri?)
    fun onPostClicked(post: Post)
    fun onPostRecordClicked(record: Record, owningPostUri: PostUri)
    fun onPostMediaClicked(media: Embed.Media, index: Int, post: Post, quotingPostUri: PostUri?)
    fun onReplyToPost(post: Post)
    fun onPostInteraction(interaction: Post.Interaction)
    fun onPostMetadataClicked(metadata: Post.Metadata)
    fun onPostOptionsClicked(post: Post)

    companion object {
        val NoOp: PostActions = NoOpPostActions
    }
}

fun postActions(
    onLinkTargetClicked: (post: Post, linkTarget: LinkTarget) -> Unit,
    onProfileClicked: (profile: Profile, post: Post, quotingPostUri: PostUri?) -> Unit,
    onPostClicked: (post: Post) -> Unit,
    onPostRecordClicked: (record: Record, owningPostUri: PostUri) -> Unit,
    onPostMediaClicked: (media: Embed.Media, index: Int, post: Post, quotingPostUri: PostUri?) -> Unit,
    onReplyToPost: (post: Post) -> Unit,
    onPostInteraction: (interaction: Post.Interaction) -> Unit,
    onPostMetadataClicked: (metadata: Post.Metadata) -> Unit = {},
    onPostOptionsClicked: (post: Post) -> Unit,
) = object : PostActions {
    override fun onProfileClicked(
        profile: Profile,
        post: Post,
        quotingPostUri: PostUri?,
    ) = onProfileClicked(
        profile,
        post,
        quotingPostUri,
    )

    override fun onPostClicked(
        post: Post,
    ) = onPostClicked(
        post,
    )

    override fun onPostRecordClicked(
        record: Record,
        owningPostUri: PostUri,
    ) = onPostRecordClicked(
        record,
        owningPostUri,
    )

    override fun onPostMediaClicked(
        media: Embed.Media,
        index: Int,
        post: Post,
        quotingPostUri: PostUri?,
    ) = onPostMediaClicked(
        media,
        index,
        post,
        quotingPostUri,
    )

    override fun onReplyToPost(
        post: Post,
    ) = onReplyToPost(post)

    override fun onPostInteraction(
        interaction: Post.Interaction,
    ) = onPostInteraction(
        interaction,
    )

    override fun onPostMetadataClicked(
        metadata: Post.Metadata,
    ) = onPostMetadataClicked(
        metadata,
    )

    override fun onPostOptionsClicked(
        post: Post,
    ) = onPostOptionsClicked(post)

    override fun onLinkTargetClicked(
        post: Post,
        linkTarget: LinkTarget,
    ) = onLinkTargetClicked(
        post,
        linkTarget,
    )
}

private val NoOpPostActions = object : PostActions {
    override fun onProfileClicked(
        profile: Profile,
        post: Post,
        quotingPostUri: PostUri?,
    ) = Unit

    override fun onPostClicked(
        post: Post,
    ) = Unit

    override fun onPostRecordClicked(
        record: Record,
        owningPostUri: PostUri,
    ) = Unit

    override fun onPostMediaClicked(
        media: Embed.Media,
        index: Int,
        post: Post,
        quotingPostUri: PostUri?,
    ) = Unit

    override fun onReplyToPost(
        post: Post,
    ) = Unit

    override fun onPostInteraction(
        interaction: Post.Interaction,
    ) = Unit

    override fun onPostMetadataClicked(
        metadata: Post.Metadata,
    ) = Unit

    override fun onPostOptionsClicked(
        post: Post,
    ) = Unit

    override fun onLinkTargetClicked(
        post: Post,
        linkTarget: LinkTarget,
    ) = Unit
}
