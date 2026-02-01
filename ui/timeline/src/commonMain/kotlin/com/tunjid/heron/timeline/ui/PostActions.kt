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
import com.tunjid.heron.data.core.models.AppliedLabels
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.timeline.ui.post.PostMetadata

sealed interface PostAction {
    data class OfLinkTarget(
        val post: Post,
        val linkTarget: LinkTarget,
    ) : PostAction

    data class OfProfile(
        val profile: Profile,
        val post: Post,
        val quotingPostUri: PostUri?,
    ) : PostAction

    data class OfPost(
        val post: Post,
        val warnedAppliedLabels: AppliedLabels.Filtered?,
        val isMainPost: Boolean,
    ) : PostAction

    data class OfRecord(
        val record: Record,
        val owningPostUri: PostUri,
    ) : PostAction

    data class OfMedia(
        val media: Embed.Media,
        val index: Int,
        val post: Post,
        val isMainPost: Boolean,
        val quotingPostUri: PostUri?,
    ) : PostAction

    sealed interface Options : PostAction

    data class OfReply(
        val post: Post,
    ) : Options

    data class OfInteraction(
        val interaction: Post.Interaction,
        val viewerStats: Post.ViewerStats?,
    ) : Options

    data class OfMetadata(
        val metadata: PostMetadata,
    ) : Options

    data class OfMore(
        val post: Post,
    ) : Options
}

@Stable
fun interface PostActions {
    fun onPostAction(action: PostAction)

    companion object {
        val NoOp: PostActions = PostActions {}
    }
}
