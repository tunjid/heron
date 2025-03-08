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

package com.tunjid.heron.timeline.ui.post

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.UnknownEmbed
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.timeline.ui.post.feature.BlockedPostPost
import com.tunjid.heron.timeline.ui.post.feature.InvisiblePostPost
import com.tunjid.heron.timeline.ui.post.feature.QuotedPost
import com.tunjid.heron.timeline.ui.post.feature.UnknownPostPost
import com.tunjid.heron.timeline.ui.withQuotingPostIdPrefix
import com.tunjid.heron.ui.PanedSharedElementScope
import kotlinx.datetime.Instant

@Composable
internal fun PostEmbed(
    modifier: Modifier = Modifier,
    now: Instant,
    embed: Embed?,
    quote: Post?,
    postId: Id,
    sharedElementPrefix: String,
    panedSharedElementScope: PanedSharedElementScope,
    onPostMediaClicked: (media: Embed.Media, index: Int, quotingPostId: Id?) -> Unit,
    onQuotedPostClicked: (Post) -> Unit,
    onQuotedProfileClicked: (Post, Profile) -> Unit,
    presentation: Timeline.Presentation,
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier
    ) {
        when (embed) {
            is ExternalEmbed -> PostExternal(
                feature = embed,
                postId = postId,
                sharedElementPrefix = sharedElementPrefix,
                presentation = presentation,
                panedSharedElementScope = panedSharedElementScope,
                onClick = {
                    uriHandler.openUri(embed.uri.uri)
                },
            )

            is ImageList -> PostImages(
                feature = embed,
                sharedElementPrefix = sharedElementPrefix,
                panedSharedElementScope = panedSharedElementScope,
                presentation = presentation,
                onImageClicked = { index ->
                    onPostMediaClicked(embed, index, null)
                }
            )

            UnknownEmbed -> UnknownPostPost(onClick = {})
            is Video -> PostVideo(
                video = embed,
                panedSharedElementScope = panedSharedElementScope,
                sharedElementPrefix = sharedElementPrefix,
                presentation = presentation,
                onClicked = {
                    onPostMediaClicked(embed, 0, null)
                }
            )

            null -> Unit
        }
        if (presentation == Timeline.Presentation.TextAndEmbed) {
            if (quote != null) Spacer(Modifier.height(16.dp))
            when (quote?.cid) {
                null -> Unit
                Constants.notFoundPostId -> InvisiblePostPost(onClick = {})
                Constants.blockedPostId -> BlockedPostPost(onClick = {})
                Constants.unknownPostId -> UnknownPostPost(onClick = {})
                else -> QuotedPost(
                    now = now,
                    post = quote,
                    sharedElementPrefix = sharedElementPrefix.withQuotingPostIdPrefix(
                        quotingPostId = postId,
                    ),
                    panedSharedElementScope = panedSharedElementScope,
                    onProfileClicked = onQuotedProfileClicked,
                    onPostMediaClicked = { media, index ->
                        onPostMediaClicked(media, index, postId)
                    },
                    onClick = {
                        onQuotedPostClicked(quote)
                    }
                )
            }
        }
    }
}
