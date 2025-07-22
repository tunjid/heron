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

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Commit
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.timeline.ui.post.Post
import com.tunjid.heron.timeline.ui.post.PostReasonLine
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.childThreadNode
import com.tunjid.heron.timeline.ui.post.threadtraversal.videoId
import com.tunjid.heron.timeline.utilities.createdAt
import com.tunjid.heron.timeline.utilities.presentationPadding
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.shapes.toRoundedPolygonShape
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.ui_timeline.generated.resources.Res
import heron.ui_timeline.generated.resources.see_more_posts
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource

@Composable
fun TimelineItem(
    modifier: Modifier = Modifier,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    presentationLookaheadScope: LookaheadScope,
    now: Instant,
    item: TimelineItem,
    sharedElementPrefix: String,
    presentation: Timeline.Presentation,
    postActions: PostActions,
) {
    TimelineCard(
        item = item,
        modifier = modifier,
        presentation = presentation,
        onPostClicked = { post ->
            postActions.onPostClicked(
                post = post,
                quotingPostId = null,
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .presentationPadding(
                        presentation = presentation,
                        top = if (item.isThreadedAnchor) 0.dp
                        else 16.dp,
                        bottom = if (item.isThreadedAncestorOrAnchor) 0.dp
                        else 8.dp,
                    ),
            ) {
                if (item is TimelineItem.Repost) {
                    PostReasonLine(
                        modifier = Modifier.padding(
                            start = 32.dp,
                            bottom = 4.dp
                        ),
                        item = item,
                        onProfileClicked = { post, profile ->
                            postActions.onProfileClicked(
                                profile = profile,
                                post = post,
                                quotingPostId = null
                            )
                        },
                    )
                }
                if (item is TimelineItem.Thread && presentation == Timeline.Presentation.Text.WithEmbed) ThreadedPost(
                    modifier = Modifier
                        .fillMaxWidth(),
                    paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                    presentationLookaheadScope = presentationLookaheadScope,
                    item = item,
                    sharedElementPrefix = sharedElementPrefix,
                    now = now,
                    presentation = presentation,
                    postActions = postActions,
                ) else Post(
                    modifier = Modifier
                        .fillMaxWidth()
                        .childThreadNode(videoId = item.post.videoId),
                    paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                    presentationLookaheadScope = presentationLookaheadScope,
                    now = now,
                    post = item.post,
                    isAnchoredInTimeline = false,
                    avatarShape = RoundedPolygonShape.Circle,
                    sharedElementPrefix = sharedElementPrefix,
                    createdAt = item.post.createdAt,
                    presentation = presentation,
                    postActions = postActions,
                )
            }
        },
    )
}

@Composable
private fun ThreadedPost(
    modifier: Modifier = Modifier,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    presentationLookaheadScope: LookaheadScope,
    item: TimelineItem.Thread,
    sharedElementPrefix: String,
    now: Instant,
    presentation: Timeline.Presentation,
    postActions: PostActions,
) {
    Column(
        modifier = modifier
    ) {
        item.posts.forEachIndexed { index, post ->
            if (index == 0 || item.posts[index].cid != item.posts[index - 1].cid) {
                Post(
                    modifier = Modifier
                        .childThreadNode(videoId = post.videoId),
                    paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                    presentationLookaheadScope = presentationLookaheadScope,
                    now = now,
                    post = post,
                    isAnchoredInTimeline = item.generation == 0L,
                    avatarShape =
                        when {
                            item.isThreadedAnchor -> RoundedPolygonShape.Circle
                            item.isThreadedAncestor ->
                                if (item.posts.size == 1) ReplyThreadStartImageShape
                                else ReplyThreadImageShape

                            else -> when (index) {
                                0 ->
                                    if (item.posts.size == 1) RoundedPolygonShape.Circle
                                    else ReplyThreadStartImageShape

                                item.posts.lastIndex -> ReplyThreadEndImageShape
                                else -> ReplyThreadImageShape
                            }
                        },
                    sharedElementPrefix = sharedElementPrefix,
                    createdAt = post.createdAt,
                    presentation = presentation,
                    postActions = postActions,
                    timeline = {
                        if (index != item.posts.lastIndex || item.isThreadedAncestor) Timeline(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(top = 52.dp)
                        )
                    }
                )
                if (index != item.posts.lastIndex)
                    if (index == 0 && item.hasBreak) BrokenTimeline(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .childThreadNode(videoId = null),
                        onClick = {
                            postActions.onPostClicked(
                                post = post,
                                quotingPostId = null,
                            )
                        }
                    )
                    else Timeline(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .height(
                                if (index == 0) 16.dp
                                else 12.dp
                            )
                            .childThreadNode(videoId = null)
                    )
                if (index == item.posts.lastIndex - 1 && !item.isThreadedAncestorOrAnchor) Spacer(
                    Modifier
                        .height(4.dp)
                        .childThreadNode(videoId = null)
                )
            }
        }
    }
}

@Composable
private fun Timeline(
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Spacer(
            Modifier
                .offset(x = 4.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .fillMaxHeight()
                .width(2.dp)
        )
    }
}

@Composable
private fun BrokenTimeline(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = NoOpInteractionSource,
                indication = null,
                onClick = onClick,
            )
    ) {
        Spacer(
            Modifier
                .offset(x = 4.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .height(8.dp)
                .width(2.dp)
        )
        Box {
            Row(
                modifier = Modifier.offset(y = -(3.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier
                        .offset(x = -(7).dp)
                        .rotate(90f),
                    imageVector = Icons.Rounded.Commit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Text(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onClick)
                        .padding(horizontal = 16.dp),
                    text = stringResource(Res.string.see_more_posts),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.outline
                    ),
                )
            }
            Spacer(
                Modifier
                    .padding(top = 12.dp)
                    .offset(x = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .height(20.dp)
                    .width(2.dp)
            )
        }
    }
}

@Composable
fun TimelineCard(
    item: TimelineItem,
    modifier: Modifier = Modifier,
    presentation: Timeline.Presentation,
    onPostClicked: (Post) -> Unit,
    content: @Composable () -> Unit,
) {
    if (item.isThreadedAncestorOrAnchor) Surface(
        modifier = modifier,
        shape = RectangleShape,
        onClick = { onPostClicked(item.post) },
        content = { content() },
    )
    else ElevatedCard(
        modifier = modifier,
        shape = animateDpAsState(
            when (presentation) {
                Timeline.Presentation.Text.WithEmbed -> 8.dp
                Timeline.Presentation.Media.Condensed -> 8.dp
                Timeline.Presentation.Media.Expanded -> 0.dp
            }
        ).value.let(::RoundedCornerShape),
        onClick = { onPostClicked(item.post) },
        content = { content() },
    )
}

private val ReplyThreadStartImageShape =
    RoundedCornerShape(
        topStartPercent = 100,
        topEndPercent = 100,
        bottomStartPercent = 30,
        bottomEndPercent = 100,
    ).toRoundedPolygonShape()

private val ReplyThreadImageShape =
    RoundedPolygonShape.Polygon(
        cornerSizeAtIndex = (0..4).map { index ->
            if (index == 2 || index == 3) 32.dp
            else 48.dp
        }
    )

private val ReplyThreadEndImageShape =
    RoundedCornerShape(
        topStartPercent = 30,
        topEndPercent = 100,
        bottomStartPercent = 100,
        bottomEndPercent = 100,
    ).toRoundedPolygonShape()

fun Post.avatarSharedElementKey(
    prefix: String?,
    quotingPostId: PostId? = null,
): String {
    val finalPrefix = quotingPostId
        ?.let { "$prefix-$it" }
        ?: prefix
    return "$finalPrefix-${cid.id}-${author.did.id}"
}

fun Message.avatarSharedElementKey(
    prefix: String?,
    conversationId: ConversationId,
): String {
    val finalPrefix = conversationId
        .let { "$prefix-$it" }
    return "$finalPrefix-${id.id}-${conversationId.id}"
}

fun FeedGenerator.avatarSharedElementKey(
    prefix: String?,
): String = "$prefix-${uri.uri}-avatar"

fun FeedList.avatarSharedElementKey(
    prefix: String?,
): String = "$prefix-${uri.uri}-avatar"

fun StarterPack.avatarSharedElementKey(
    prefix: String?,
): String = "$prefix-${uri.uri}-avatar"

fun String.withQuotingPostIdPrefix(
    quotingPostId: PostId? = null,
): String = quotingPostId
    ?.let { "$this-$it" }
    ?: this

private val TimelineItem.isThreadedAncestor
    get() = this is TimelineItem.Thread && when (val gen = generation) {
        null -> false
        else -> gen <= -1
    }

private val TimelineItem.isThreadedAnchor
    get() = this is TimelineItem.Thread && generation == 0L

private val TimelineItem.isThreadedAncestorOrAnchor
    get() = isThreadedAncestor || isThreadedAnchor

private val NoOpInteractionSource = MutableInteractionSource()