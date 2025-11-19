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
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.timeline.ui.post.Post
import com.tunjid.heron.timeline.ui.post.PostReasonLine
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.childThreadNode
import com.tunjid.heron.timeline.utilities.createdAt
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.see_more_posts
import heron.ui.timeline.generated.resources.show_more
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
        onPostClicked = postActions::onPostClicked,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .timelineCardPresentationPadding(
                        presentation = presentation,
                    ),
            ) {
                if (item is TimelineItem.Repost) {
                    PostReasonLine(
                        modifier = Modifier.padding(
                            start = 32.dp,
                            top = 4.dp,
                            bottom = 4.dp,
                        ),
                        item = item,
                        onProfileClicked = { post, profile ->
                            postActions.onProfileClicked(
                                profile = profile,
                                post = post,
                                quotingPostUri = null,
                            )
                        },
                    )
                }
                when {
                    item is TimelineItem.Thread && presentation == Timeline.Presentation.Text.WithEmbed -> ThreadedPost(
                        modifier = Modifier
                            .fillMaxWidth(),
                        paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                        presentationLookaheadScope = presentationLookaheadScope,
                        item = item,
                        sharedElementPrefix = sharedElementPrefix,
                        now = now,
                        presentation = presentation,
                        postActions = postActions,
                    )

                    else -> Post(
                        modifier = Modifier
                            .fillMaxWidth(),
                        paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                        presentationLookaheadScope = presentationLookaheadScope,
                        now = now,
                        post = item.post,
                        isAnchoredInTimeline = false,
                        avatarShape = RoundedPolygonShape.Circle,
                        sharedElementPrefix = sharedElementPrefix,
                        createdAt = item.post.createdAt,
                        presentation = presentation,
                        appliedLabels = item.appliedLabels,
                        postActions = postActions,
                    )
                }
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
    var maxPosts by rememberSaveable {
        mutableStateOf(DefaultMaxPostsInThread)
    }
    Column(
        modifier = modifier,
    ) {
        val limitedPosts = remember(item.posts, maxPosts) {
            item.posts.take(maxPosts)
        }
        limitedPosts.forEachIndexed { index, post ->
            key(post.uri.uri) {
                if (index == 0 || item.posts[index].uri != item.posts[index - 1].uri) {
                    Post(
                        modifier = Modifier,
                        paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                        presentationLookaheadScope = presentationLookaheadScope,
                        now = now,
                        post = post,
                        isAnchoredInTimeline = item.generation == 0L,
                        avatarShape = when {
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
                        appliedLabels = item.appliedLabels,
                        postActions = postActions,
                        timeline = {
                            if (index != item.posts.lastIndex || item.isThreadedAncestor) Timeline(
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(top = 60.dp),
                            )
                        },
                    )
                    if (index != item.posts.lastIndex) {
                        if (index == 0 && item.hasBreak) BrokenTimeline(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .childThreadNode(videoId = null),
                            onClick = {
                                postActions.onPostClicked(post = post)
                            },
                        )
                        else Timeline(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .height(
                                    if (index == 0) 16.dp
                                    else 12.dp,
                                )
                                .childThreadNode(videoId = null),
                        )
                    }
                    if (index == item.posts.lastIndex - 1 && !item.isThreadedAncestorOrAnchor && maxPosts >= item.posts.size) Spacer(
                        Modifier
                            .height(2.dp)
                            .childThreadNode(videoId = null),
                    )
                }
            }
        }

        if (item.posts.size > maxPosts) ShowMore {
            maxPosts += DefaultMaxPostsInThread
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
                .width(2.dp),
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
            ),
    ) {
        Spacer(
            Modifier
                .offset(x = 4.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .height(8.dp)
                .width(2.dp),
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
                        color = MaterialTheme.colorScheme.outline,
                    ),
                )
            }
            Spacer(
                Modifier
                    .padding(top = 12.dp)
                    .offset(x = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .height(20.dp)
                    .width(2.dp),
            )
        }
    }
}

@Composable
private fun ShowMore(
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(),
        ) {
            Timeline(
                Modifier
                    .offset(8.dp)
                    .height(4.dp),
            )
            Icon(
                modifier = Modifier
                    .offset(1.dp, y = (-3).dp)
                    .rotate(90f),
                imageVector = Icons.Rounded.LinearScale,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
        TextButton(
            modifier = Modifier
                .offset(y = (-4).dp)
                .weight(1f),
            onClick = onClick,
            content = {
                Text(stringResource(Res.string.show_more))
            },
        )
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
    val cornerRadius =
        if (item.isThreadedAncestorOrAnchor) 0.dp
        else presentation.timelineCardPadding

    val isFlat = cornerRadius == 0.dp

    ElevatedCard(
        modifier = modifier,
        shape = animateDpAsState(cornerRadius).value.let(::RoundedCornerShape),
        colors =
        if (isFlat) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        else CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.cardElevation(),
        onClick = { onPostClicked(item.post) },
        content = { content() },
    )
}

private val ReplyThreadStartImageShape =
    RoundedPolygonShape.RoundedRectangle(
        topStartPercent = 1f,
        topEndPercent = 1f,
        bottomStartPercent = 0.3f,
        bottomEndPercent = 1f,
    )

private val ReplyThreadImageShape =
    RoundedPolygonShape.Polygon(
        cornerSizePercentAtIndex = (0..4).map { index ->
            if (index == 2 || index == 3) 2f / 3
            else 1f
        },
    )

private val ReplyThreadEndImageShape =
    RoundedPolygonShape.RoundedRectangle(
        topStartPercent = 0.3f,
        topEndPercent = 1f,
        bottomStartPercent = 1f,
        bottomEndPercent = 1f,
    )

private val Timeline.Presentation.timelineCardPadding: Dp
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed -> 8.dp
        Timeline.Presentation.Media.Condensed -> 8.dp
        Timeline.Presentation.Media.Expanded -> 0.dp
        Timeline.Presentation.Media.Grid -> 0.dp
    }

private fun Modifier.timelineCardPresentationPadding(
    presentation: Timeline.Presentation,
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp,
) = when (presentation) {
    Timeline.Presentation.Text.WithEmbed -> padding(
        start = start,
        top = top,
        end = end,
        bottom = bottom,
    )

    Timeline.Presentation.Media.Condensed -> this
    Timeline.Presentation.Media.Expanded -> this
    Timeline.Presentation.Media.Grid -> this
}

fun Record.avatarSharedElementKey(
    prefix: String?,
    quotingPostUri: PostUri? = null,
): String {
    val finalPrefix = quotingPostUri
        ?.let { "$prefix-$it" }
        ?: prefix
    val creator = when (this) {
        is Labeler -> creator
        is Post -> author
        is FeedGenerator -> creator
        is FeedList -> creator
        is StarterPack -> creator
    }
    return "$finalPrefix-${reference.uri.uri}-${creator.did.id}-avatar"
}

fun String.withQuotingPostUriPrefix(
    quotingPostUri: PostUri? = null,
): String = quotingPostUri
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

private const val DefaultMaxPostsInThread = 3
