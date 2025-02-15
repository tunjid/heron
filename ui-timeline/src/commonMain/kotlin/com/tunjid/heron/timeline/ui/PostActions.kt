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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile

@Stable
interface PostActions {
    fun onProfileClicked(profile: Profile, post: Post, parentPost: Post?)
    fun onPostClicked(post: Post, parentPost: Post?)
    fun onPostMediaClicked(media: Embed.Media, index: Int, post: Post, parentPost: Post?)
    fun onReplyToPost(post: Post)
    fun onPostInteraction(interaction: Post.Interaction)
    fun onPostMetadataClicked(metadata: Post.Metadata)
}

@Composable
fun rememberPostActions(
    onProfileClicked: (profile: Profile, post: Post, parentPost: Post?) -> Unit,
    onPostClicked: (post: Post, parentPost: Post?) -> Unit,
    onPostMediaClicked: (media: Embed.Media, index: Int, post: Post, parentPost: Post?) -> Unit,
    onReplyToPost: (post: Post) -> Unit,
    onPostInteraction: (interaction: Post.Interaction) -> Unit,
    onPostMetadataClicked: (metadata: Post.Metadata) -> Unit = {},
) = remember {
    object : PostActions {
        override fun onProfileClicked(
            profile: Profile,
            post: Post,
            parentPost: Post?,
        ) = onProfileClicked(
            profile,
            post,
            parentPost
        )

        override fun onPostClicked(
            post: Post,
            parentPost: Post?,
        ) = onPostClicked(
            post,
            parentPost
        )

        override fun onPostMediaClicked(
            media: Embed.Media,
            index: Int,
            post: Post,
            parentPost: Post?,
        ) = onPostMediaClicked(
            media,
            index,
            post,
            parentPost
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
    }
}