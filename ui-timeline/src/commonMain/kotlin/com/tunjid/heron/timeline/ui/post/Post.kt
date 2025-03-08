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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.UnknownEmbed
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.PostActions
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.createdAt
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import kotlinx.datetime.Instant

@Composable
fun Post(
    panedSharedElementScope: PanedSharedElementScope,
    presentationLookaheadScope: LookaheadScope,
    modifier: Modifier = Modifier,
    now: Instant,
    post: Post,
    isAnchoredInTimeline: Boolean,
    avatarShape: RoundedPolygonShape,
    sharedElementPrefix: String,
    createdAt: Instant,
    presentation: Timeline.Presentation,
    postActions: PostActions,
    timeline: @Composable (BoxScope.() -> Unit) = {},
) {
    Box(modifier = modifier) {
        if (presentation == Timeline.Presentation.Text.WithEmbed) Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 8.dp),
            content = timeline,
        )
        val postData = rememberUpdatedPostData(
            postActions = postActions,
            panedSharedElementScope = panedSharedElementScope,
            presentationLookaheadScope = presentationLookaheadScope,
            post = post,
            presentation = presentation,
            sharedElementPrefix = sharedElementPrefix,
            avatarShape = avatarShape,
            now = now,
            createdAt = createdAt
        )
        val verticalPadding = when (presentation) {
            Timeline.Presentation.Text.WithEmbed -> 4.dp
            Timeline.Presentation.Media.Expanded -> 8.dp
            Timeline.Presentation.Media.Condensed -> 0.dp
        }
        Column(
            modifier = Modifier
                .padding(vertical = verticalPadding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(verticalPadding),
        ) {
            presentation.contentOrder.forEach { order ->
                key(order.key) {
                    when (order) {
                        PostContent.Actions -> ActionsContent(postData)
                        PostContent.Attribution -> AttributionContent(postData)
                        PostContent.Embed.Link -> EmbedContent(postData)
                        PostContent.Embed.Media -> EmbedContent(postData)
                        PostContent.Metadata -> if (isAnchoredInTimeline) MetadataContent(postData)
                        PostContent.Text -> TextContent(postData)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AttributionContent(
    data: PostData,
) = with(data.panedSharedElementScope) {
    when (data.presentation) {
        Timeline.Presentation.Text.WithEmbed,
        Timeline.Presentation.Media.Expanded,
            -> AttributionLayout(
            modifier = Modifier
                .contentPresentationPadding(
                    content = PostContent.Attribution,
                    presentation = data.presentation,
                ),
            avatar = {
                updatedMovableSharedElementOf(
                    modifier = Modifier
                        .size(UiTokens.avatarSize)
                        .clip(data.avatarShape)
                        .clickable {
                            data.postActions.onProfileClicked(
                                profile = data.post.author,
                                post = data.post,
                                quotingPostId = null,
                            )
                        },
                    key = data.post.avatarSharedElementKey(data.sharedElementPrefix),
                    state = remember(data.post.author.avatar) {
                        ImageArgs(
                            url = data.post.author.avatar?.uri,
                            contentScale = ContentScale.Crop,
                            contentDescription = data.post.author.displayName
                                ?: data.post.author.handle.id,
                            shape = data.avatarShape,
                        )
                    },
                    sharedElement = { state, modifier ->
                        AsyncImage(state, modifier)
                    }
                )
            },
            label = {
                PostHeadline(
                    now = data.now,
                    createdAt = data.createdAt,
                    author = data.post.author,
                    postId = data.post.cid,
                    sharedElementPrefix = data.sharedElementPrefix,
                    panedSharedElementScope = this,
                )
            }
        )

        Timeline.Presentation.Media.Condensed -> Unit
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun TextContent(
    data: PostData,
) = with(data.panedSharedElementScope) {
    when (data.presentation) {
        Timeline.Presentation.Text.WithEmbed,
        Timeline.Presentation.Media.Expanded,
            -> PostText(
            post = data.post,
            sharedElementPrefix = data.sharedElementPrefix,
            panedSharedElementScope = this,
            modifier = Modifier
                .zIndex(TextContentZIndex)
                .contentPresentationPadding(
                    content = PostContent.Text,
                    presentation = data.presentation,
                )
                .animateBounds(data.presentationLookaheadScope)
                .fillMaxWidth(),
            maxLines = when (data.presentation) {
                Timeline.Presentation.Text.WithEmbed -> Int.MAX_VALUE
                Timeline.Presentation.Media.Condensed -> throw IllegalArgumentException(
                    "Condensed media should not show text"
                )

                Timeline.Presentation.Media.Expanded -> 2
            },
            onClick = {
                data.postActions.onPostClicked(
                    post = data.post,
                    quotingPostId = null,
                )
            },
            onProfileClicked = { post, profile ->
                data.postActions.onProfileClicked(
                    profile = profile,
                    post = post,
                    quotingPostId = null
                )
            }
        )

        Timeline.Presentation.Media.Condensed -> Unit
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun EmbedContent(
    data: PostData,
) {
    PostEmbed(
        modifier = Modifier
            .zIndex(EmbedContentZIndex)
            .contentPresentationPadding(
                content = data.post.embed.asPostContent(),
                presentation = data.presentation,
            )
            .animateBounds(data.presentationLookaheadScope)
            .fillMaxWidth(),
        now = data.now,
        embed = data.post.embed,
        quote = data.post.quote,
        postId = data.post.cid,
        presentation = data.presentation,
        sharedElementPrefix = data.sharedElementPrefix,
        panedSharedElementScope = data.panedSharedElementScope,
        onPostMediaClicked = { media, index, quotingPostId ->
            data.postActions.onPostMediaClicked(
                media = media,
                index = index,
                post = data.post,
                quotingPostId = quotingPostId,
            )
        },
        onQuotedPostClicked = { quotedPost ->
            data.postActions.onPostClicked(
                post = quotedPost,
                quotingPostId = data.post.cid
            )
        },
        onQuotedProfileClicked = { quotedPost, quotedProfile ->
            data.postActions.onProfileClicked(
                profile = quotedProfile,
                post = quotedPost,
                quotingPostId = data.post.cid
            )
        },
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ActionsContent(
    data: PostData,
) {
    when (data.presentation) {
        Timeline.Presentation.Text.WithEmbed,
        Timeline.Presentation.Media.Expanded,
            -> PostActions(
            modifier = Modifier
                .contentPresentationPadding(
                    content = PostContent.Actions,
                    presentation = data.presentation,
                )
                .animateBounds(data.presentationLookaheadScope),
            replyCount = format(data.post.replyCount),
            repostCount = format(data.post.repostCount),
            likeCount = format(data.post.likeCount),
            repostUri = data.post.viewerStats?.repostUri,
            likeUri = data.post.viewerStats?.likeUri,
            postId = data.post.cid,
            postUri = data.post.uri,
            presentation = data.presentation,
            sharedElementPrefix = data.sharedElementPrefix,
            panedSharedElementScope = data.panedSharedElementScope,
            presentationLookaheadScope = data.presentationLookaheadScope,
            onReplyToPost = {
                data.postActions.onReplyToPost(data.post)
            },
            onPostInteraction = data.postActions::onPostInteraction,
        )

        Timeline.Presentation.Media.Condensed -> Unit
    }
}

@Composable
private fun MetadataContent(
    data: PostData,
) {
    PostMetadata(
        modifier = Modifier
            .padding(
                horizontal = 24.dp,
                vertical = 4.dp,
            ),
        time = data.post.createdAt,
        postId = data.post.cid,
        profileId = data.post.author.did,
        reposts = data.post.repostCount,
        quotes = data.post.quoteCount,
        likes = data.post.likeCount,
        onMetadataClicked = data.postActions::onPostMetadataClicked,
    )
}

private fun Modifier.contentPresentationPadding(
    content: PostContent,
    presentation: Timeline.Presentation,
) = padding(
    start = when (content) {
        PostContent.Actions -> when (presentation) {
            Timeline.Presentation.Text.WithEmbed -> 24.dp
            Timeline.Presentation.Media.Expanded -> 16.dp
            Timeline.Presentation.Media.Condensed -> 0.dp
        }

        PostContent.Attribution -> when (presentation) {
            Timeline.Presentation.Text.WithEmbed -> 8.dp
            Timeline.Presentation.Media.Expanded -> 8.dp
            Timeline.Presentation.Media.Condensed -> 0.dp
        }

        is PostContent.Embed -> when (presentation) {
            Timeline.Presentation.Text.WithEmbed -> 24.dp
            Timeline.Presentation.Media.Expanded -> when (content) {
                PostContent.Embed.Link -> 8.dp
                PostContent.Embed.Media -> 0.dp
            }

            Timeline.Presentation.Media.Condensed -> 0.dp
        }

        PostContent.Text -> when (presentation) {
            Timeline.Presentation.Text.WithEmbed -> 24.dp
            Timeline.Presentation.Media.Expanded -> 16.dp
            Timeline.Presentation.Media.Condensed -> 0.dp
        }

        PostContent.Metadata -> 0.dp
    },
    end = when (content) {
        PostContent.Actions -> when (presentation) {
            Timeline.Presentation.Text.WithEmbed -> 16.dp
            Timeline.Presentation.Media.Expanded -> 16.dp
            Timeline.Presentation.Media.Condensed -> 0.dp
        }

        PostContent.Attribution -> when (presentation) {
            Timeline.Presentation.Text.WithEmbed -> 8.dp
            Timeline.Presentation.Media.Expanded -> 8.dp
            Timeline.Presentation.Media.Condensed -> 0.dp
        }

        is PostContent.Embed -> when (presentation) {
            Timeline.Presentation.Text.WithEmbed -> 16.dp
            Timeline.Presentation.Media.Expanded -> when (content) {
                PostContent.Embed.Link -> 8.dp
                PostContent.Embed.Media -> 0.dp
            }

            Timeline.Presentation.Media.Condensed -> 0.dp
        }

        PostContent.Text -> when (presentation) {
            Timeline.Presentation.Text.WithEmbed -> 16.dp
            Timeline.Presentation.Media.Expanded -> 16.dp
            Timeline.Presentation.Media.Condensed -> 0.dp
        }

        PostContent.Metadata -> 0.dp
    }
)

private fun Embed?.asPostContent() = when (this) {
    is ImageList,
    is Video,
        -> PostContent.Embed.Media

    null,
    UnknownEmbed,
    is ExternalEmbed,
        -> PostContent.Embed.Link
}

@Composable
private fun rememberUpdatedPostData(
    postActions: PostActions,
    panedSharedElementScope: PanedSharedElementScope,
    presentationLookaheadScope: LookaheadScope,
    post: Post,
    presentation: Timeline.Presentation,
    sharedElementPrefix: String,
    avatarShape: RoundedPolygonShape,
    now: Instant,
    createdAt: Instant,
): PostData {
    return remember {
        PostData(
            postActions = postActions,
            panedSharedElementScope = panedSharedElementScope,
            presentationLookaheadScope = presentationLookaheadScope,
            post = post,
            presentation = presentation,
            sharedElementPrefix = sharedElementPrefix,
            avatarShape = avatarShape,
            now = now,
            created = createdAt
        )
    }.also {
        it.postActions = postActions
        it.panedSharedElementScope = panedSharedElementScope
        it.presentationLookaheadScope = presentationLookaheadScope
        it.post = post
        it.presentation = presentation
        it.sharedElementPrefix = sharedElementPrefix
        it.avatarShape = avatarShape
        it.now = now
        it.createdAt = createdAt
    }
}

@Stable
private class PostData(
    postActions: PostActions,
    panedSharedElementScope: PanedSharedElementScope,
    presentationLookaheadScope: LookaheadScope,
    post: Post,
    presentation: Timeline.Presentation,
    sharedElementPrefix: String,
    avatarShape: RoundedPolygonShape,
    now: Instant,
    created: Instant,
) {
    var postActions by mutableStateOf(postActions)
    var panedSharedElementScope by mutableStateOf(panedSharedElementScope)
    var presentationLookaheadScope by mutableStateOf(presentationLookaheadScope)
    var post by mutableStateOf(post)
    var presentation by mutableStateOf(presentation)
    var sharedElementPrefix by mutableStateOf(sharedElementPrefix)
    var avatarShape by mutableStateOf(avatarShape)
    var now by mutableStateOf(now)
    var createdAt by mutableStateOf(created)
}

private sealed class PostContent(val key: String) {
    data object Attribution : PostContent(key = "Attribution")
    data object Text : PostContent(key = "Text")
    sealed class Embed : PostContent(key = "Embed") {
        data object Link : Embed()
        data object Media : Embed()
    }

    data object Metadata : PostContent(key = "Metadata")
    data object Actions : PostContent(key = "Actions")
}

@Stable
private val Timeline.Presentation.contentOrder
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed -> TextAndEmbedOrder
        Timeline.Presentation.Media.Expanded -> ExpandedMediaOrder
        Timeline.Presentation.Media.Condensed -> CondensedMediaOrder
    }

private val TextAndEmbedOrder = listOf(
    PostContent.Attribution,
    PostContent.Text,
    PostContent.Embed.Media,
    PostContent.Metadata,
    PostContent.Actions,
)

private val ExpandedMediaOrder = listOf(
    PostContent.Attribution,
    PostContent.Embed.Media,
    PostContent.Text,
    PostContent.Metadata,
    PostContent.Actions,
)

private val CondensedMediaOrder = listOf(
    PostContent.Attribution,
    PostContent.Text,
    PostContent.Embed.Media,
    PostContent.Metadata,
    PostContent.Actions,
)

private const val EmbedContentZIndex = 2f
private const val TextContentZIndex = 1f