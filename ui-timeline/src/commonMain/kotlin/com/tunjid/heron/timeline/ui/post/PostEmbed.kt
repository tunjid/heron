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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
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
import com.tunjid.heron.data.core.models.UnknownEmbed
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.timeline.ui.TimelineViewType
import com.tunjid.heron.timeline.ui.post.feature.BlockedPostPost
import com.tunjid.heron.timeline.ui.post.feature.InvisiblePostPost
import com.tunjid.heron.timeline.ui.post.feature.QuotedPost
import com.tunjid.heron.timeline.ui.post.feature.UnknownPostPost
import com.tunjid.heron.timeline.ui.withQuotedPostPrefix
import com.tunjid.heron.ui.PanedSharedElementScope
import kotlinx.datetime.Instant

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PostEmbed(
    now: Instant,
    embed: Embed?,
    quote: Post?,
    postId: Id,
    sharedElementPrefix: String,
    panedSharedElementScope: PanedSharedElementScope,
    onPostMediaClicked: (media: Embed.Media, index: Int, quotingPostId: Id?) -> Unit,
    onQuotedPostClicked: (Post) -> Unit,
    viewType: TimelineViewType,
) {
    val uriHandler = LocalUriHandler.current
    Column(
        // Needed to animate view type changes
        modifier = Modifier
            .animateBounds(
                lookaheadScope = panedSharedElementScope,
            )
    ) {
        when (embed) {
            is ExternalEmbed -> PostExternal(
                feature = embed,
                postId = postId,
                sharedElementPrefix = sharedElementPrefix,
                viewType = viewType,
                panedSharedElementScope = panedSharedElementScope,
                onClick = {
                    uriHandler.openUri(embed.uri.uri)
                },
            )

            is ImageList -> PostImages(
                feature = embed,
                sharedElementPrefix = sharedElementPrefix,
                panedSharedElementScope = panedSharedElementScope,
                onImageClicked = { index ->
                    onPostMediaClicked(embed, index, null)
                }
            )

            UnknownEmbed -> UnknownPostPost(onClick = {})
            is Video -> PostVideo(
                video = embed,
                panedSharedElementScope = panedSharedElementScope,
                sharedElementPrefix = sharedElementPrefix,
                onClicked = {
                    onPostMediaClicked(embed, 0, null)
                }
            )

            null -> Unit
        }
        if (viewType is TimelineViewType.Blog) {
            if (quote != null) Spacer(Modifier.height(16.dp))
            when (quote?.cid) {
                null -> Unit
                Constants.notFoundPostId -> InvisiblePostPost(onClick = {})
                Constants.blockedPostId -> BlockedPostPost(onClick = {})
                Constants.unknownPostId -> UnknownPostPost(onClick = {})
                else -> QuotedPost(
                    now = now,
                    post = quote,
                    author = quote.author,
                    sharedElementPrefix = sharedElementPrefix.withQuotedPostPrefix(
                        quotingPostId = postId,
                    ),
                    panedSharedElementScope = panedSharedElementScope,
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
