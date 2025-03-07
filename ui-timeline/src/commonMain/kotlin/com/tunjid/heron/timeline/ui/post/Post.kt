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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentWithReceiverOf
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
import com.tunjid.heron.data.core.models.Profile
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
) = with(panedSharedElementScope) {
    Box(modifier = modifier) {
        if (presentation == Timeline.Presentation.TextAndEmbed) Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 8.dp)
        ) {
            timeline()
        }
        val postData = rememberUpdatedPostData(
            postActions = postActions,
            presentationLookaheadScope = presentationLookaheadScope,
            post = post,
            presentation = presentation,
            sharedElementPrefix = sharedElementPrefix,
            avatarShape = avatarShape,
            now = now,
            createdAt = createdAt
        )
        val attributionContent = remember {
            movableContentWithReceiverOf<PanedSharedElementScope, PostData> { data ->
                AttributionContent(data)
            }
        }
        val textContent = remember {
            movableContentWithReceiverOf<PanedSharedElementScope, PostData> { data ->
                TextContent(data)
            }
        }
        val embedContent = remember {
            movableContentWithReceiverOf<PanedSharedElementScope, PostData> { data ->
                EmbedContent(data)
            }
        }
        val actionsContent = remember {
            movableContentWithReceiverOf<PanedSharedElementScope, PostData> { data ->
                ActionsContent(data)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            when (presentation) {
                Timeline.Presentation.TextAndEmbed -> {
                    attributionContent(
                        postData,
                    )
                    Spacer(Modifier.height(4.dp))
                    textContent(
                        postData,
                    )
                    Spacer(Modifier.height(4.dp))
                    embedContent(
                        postData,
                    )
                    Spacer(Modifier.height(4.dp))
                    if (isAnchoredInTimeline) PostMetadata(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(
                                vertical = 4.dp,
                            ),
                        time = post.createdAt,
                        postId = post.cid,
                        profileId = post.author.did,
                        reposts = post.repostCount,
                        quotes = post.quoteCount,
                        likes = post.likeCount,
                        onMetadataClicked = postActions::onPostMetadataClicked,
                    )
                    Spacer(Modifier.height(4.dp))
                    actionsContent(
                        postData,
                    )
                }

                Timeline.Presentation.CondensedMedia -> {
                    embedContent(
                        postData,
                    )
                }

                Timeline.Presentation.ExpandedMedia -> {
                    Spacer(Modifier.height(8.dp))
                    attributionContent(
                        postData,
                    )
                    Spacer(Modifier.height(8.dp))
                    embedContent(
                        postData,
                    )
                    Spacer(Modifier.height(8.dp))
                    actionsContent(
                        postData,
                    )
                    Spacer(Modifier.height(8.dp))
                    textContent(
                        postData,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PanedSharedElementScope.AttributionContent(
    data: PostData,
) {
    Box(
        modifier = Modifier
            .contentPresentationPadding(
                content = PostContent.Attribution,
                presentation = data.presentation,
            )
    ) {
        AttributionLayout(
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
                    panedSharedElementScope = this@AttributionContent,
                )
            }
        )
        //                if (item is TimelineItem.Reply) {
//                    PostReplyLine(item.parentPost.author, onProfileClicked)
//                }
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun PanedSharedElementScope.TextContent(
    data: PostData,
) {
    PostText(
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
            Timeline.Presentation.TextAndEmbed -> Int.MAX_VALUE
            Timeline.Presentation.CondensedMedia -> throw IllegalArgumentException(
                "Condensed media should not show text"
            )

            Timeline.Presentation.ExpandedMedia -> 2
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
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun PanedSharedElementScope.EmbedContent(
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
        panedSharedElementScope = this,
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
private fun PanedSharedElementScope.ActionsContent(
    data: PostData,
) {
    PostActions(
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
        panedSharedElementScope = this,
        presentationLookaheadScope = data.presentationLookaheadScope,
        onReplyToPost = {
            data.postActions.onReplyToPost(data.post)
        },
        onPostInteraction = data.postActions::onPostInteraction,
    )
}

@Composable
private fun rememberUpdatedPostData(
    postActions: PostActions,
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
        it.presentationLookaheadScope = presentationLookaheadScope
        it.presentation = presentation
        it.sharedElementPrefix = sharedElementPrefix
        it.avatarShape = avatarShape
        it.now = now
        it.createdAt = createdAt
    }
}

private fun Modifier.contentPresentationPadding(
    content: PostContent,
    presentation: Timeline.Presentation,
) = padding(
    start = when (content) {
        PostContent.Actions -> when (presentation) {
            Timeline.Presentation.TextAndEmbed -> 24.dp
            Timeline.Presentation.ExpandedMedia -> 8.dp
            Timeline.Presentation.CondensedMedia -> 0.dp
        }

        PostContent.Attribution -> when (presentation) {
            Timeline.Presentation.TextAndEmbed -> 8.dp
            Timeline.Presentation.ExpandedMedia -> 8.dp
            Timeline.Presentation.CondensedMedia -> 0.dp
        }

        is PostContent.Embed -> when (presentation) {
            Timeline.Presentation.TextAndEmbed -> 24.dp
            Timeline.Presentation.ExpandedMedia -> when (content) {
                PostContent.Embed.Link -> 8.dp
                PostContent.Embed.Media -> 0.dp
            }

            Timeline.Presentation.CondensedMedia -> 0.dp
        }

        PostContent.Text -> when (presentation) {
            Timeline.Presentation.TextAndEmbed -> 24.dp
            Timeline.Presentation.ExpandedMedia -> 8.dp
            Timeline.Presentation.CondensedMedia -> 0.dp
        }
    },
    end = when (content) {
        PostContent.Actions -> when (presentation) {
            Timeline.Presentation.TextAndEmbed -> 16.dp
            Timeline.Presentation.ExpandedMedia -> 8.dp
            Timeline.Presentation.CondensedMedia -> 0.dp
        }

        PostContent.Attribution -> when (presentation) {
            Timeline.Presentation.TextAndEmbed -> 8.dp
            Timeline.Presentation.ExpandedMedia -> 8.dp
            Timeline.Presentation.CondensedMedia -> 0.dp
        }

        is PostContent.Embed -> when (presentation) {
            Timeline.Presentation.TextAndEmbed -> 16.dp
            Timeline.Presentation.ExpandedMedia -> when (content) {
                PostContent.Embed.Link -> 8.dp
                PostContent.Embed.Media -> 0.dp
            }

            Timeline.Presentation.CondensedMedia -> 0.dp
        }

        PostContent.Text -> when (presentation) {
            Timeline.Presentation.TextAndEmbed -> 16.dp
            Timeline.Presentation.ExpandedMedia -> 8.dp
            Timeline.Presentation.CondensedMedia -> 0.dp
        }
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

@Stable
private class PostData(
    postActions: PostActions,
    presentationLookaheadScope: LookaheadScope,
    post: Post,
    presentation: Timeline.Presentation,
    sharedElementPrefix: String,
    avatarShape: RoundedPolygonShape,
    now: Instant,
    created: Instant,
) {
    var postActions by mutableStateOf(postActions)
    var presentationLookaheadScope by mutableStateOf(presentationLookaheadScope)
    var post by mutableStateOf(post)
    var presentation by mutableStateOf(presentation)
    var sharedElementPrefix by mutableStateOf(sharedElementPrefix)
    var avatarShape by mutableStateOf(avatarShape)
    var now by mutableStateOf(now)
    var createdAt by mutableStateOf(created)
}

private sealed class PostContent {
    data object Attribution : PostContent()
    data object Text : PostContent()
    sealed class Embed : PostContent() {
        data object Link : Embed()
        data object Media : Embed()
    }

    data object Actions : PostContent()
}

private const val EmbedContentZIndex = 2f
private const val TextContentZIndex = 1f