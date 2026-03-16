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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.tunjid.heron.data.core.models.AppliedLabels.Companion.warned
import com.tunjid.heron.data.core.models.ReplyNode
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.timeline.ui.post.Post
import com.tunjid.heron.timeline.ui.post.PostReasonLine
import com.tunjid.heron.timeline.ui.post.feature.EmptyPost
import com.tunjid.heron.timeline.ui.post.feature.LoadingPost
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.childThreadNode
import com.tunjid.heron.timeline.utilities.authorMuted
import com.tunjid.heron.timeline.utilities.createdAt
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.see_more_posts
import heron.ui.timeline.generated.resources.show_more
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

@Composable
fun TimelineItem(
    modifier: Modifier = Modifier,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    presentationLookaheadScope: LookaheadScope,
    now: Instant,
    item: TimelineItem,
    sharedElementPrefix: String,
    showEngagementMetrics: Boolean,
    presentation: Timeline.Presentation,
    postActions: PostActions,
) {
    TimelineCard(
        item = item,
        modifier = modifier,
        presentation = presentation,
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
                            postActions.onPostAction(
                                PostAction.OfProfile(
                                    profile = profile,
                                    post = post,
                                    quotingPostUri = null,
                                ),
                            )
                        },
                    )
                }
                when (item) {
                    is TimelineItem.Loading -> LoadingPost(
                        modifier = Modifier
                            .fillMaxWidth(),
                        presentation = presentation,
                    )
                    is TimelineItem.Empty -> EmptyPost(
                        modifier = Modifier
                            .fillMaxWidth(),
                        item = item,
                    )
                    is TimelineItem.Thread if presentation == Timeline.Presentation.Text.WithEmbed -> ThreadedPost(
                        modifier = Modifier
                            .fillMaxWidth(),
                        paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                        presentationLookaheadScope = presentationLookaheadScope,
                        item = item,
                        sharedElementPrefix = sharedElementPrefix,
                        now = now,
                        showEngagementMetrics = showEngagementMetrics,
                        presentation = presentation,
                        postActions = postActions,
                    )
                    is TimelineItem.ReplyTree if presentation == Timeline.Presentation.Text.WithEmbed -> ReplyTreePost(
                        modifier = Modifier.fillMaxWidth(),
                        paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                        presentationLookaheadScope = presentationLookaheadScope,
                        item = item,
                        sharedElementPrefix = sharedElementPrefix,
                        now = now,
                        showEngagementMetrics = showEngagementMetrics,
                        presentation = presentation,
                        postActions = postActions,
                    )
                    else -> Post(
                        modifier = Modifier
                            .fillMaxWidth(),
                        paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                        presentationLookaheadScope = presentationLookaheadScope,
                        hasMutedWords = item.isMuted && !item.post.authorMuted,
                        now = now,
                        post = item.post,
                        threadGate = item.threadGate,
                        isAnchoredInTimeline = false,
                        isMainPost = true,
                        showEngagementMetrics = showEngagementMetrics,
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
    showEngagementMetrics: Boolean,
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
                        hasMutedWords = item.isMuted && !item.post.authorMuted,
                        now = now,
                        post = post,
                        threadGate = item.postUrisToThreadGates[post.uri],
                        isAnchoredInTimeline = item.generation == 0L,
                        isMainPost = post == item.post,
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
                        showEngagementMetrics = showEngagementMetrics,
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
                                postActions.onPostAction(
                                    PostAction.OfPost(
                                        post = post,
                                        isMainPost = post == item.post,
                                        warnedAppliedLabels = item.appliedLabels.warned(),
                                    ),
                                )
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
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
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
    content: @Composable () -> Unit,
) {
    val cornerRadius =
        if (item.isThreadedAncestorOrAnchor) 0.dp
        else presentation.timelineCardPadding

    val isEmpty = item is TimelineItem.Empty
    val isFlat = cornerRadius == 0.dp

    val itemModifier = modifier
        .ifTrue(
            predicate = isEmpty,
            block = Modifier::fillMaxHeight,
        )

    if (isEmpty) Box(
        modifier = itemModifier,
        content = { content() },
    )
    else ElevatedCard(
        modifier = itemModifier,
        shape = animateDpAsState(cornerRadius).value.let(::RoundedCornerShape),
        colors =
        if (isFlat) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        else CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.cardElevation(),
        content = { content() },
    )
}

@Composable
private fun ReplyTreePost(
    modifier: Modifier = Modifier,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    presentationLookaheadScope: LookaheadScope,
    item: TimelineItem.ReplyTree,
    sharedElementPrefix: String,
    now: Instant,
    showEngagementMetrics: Boolean,
    presentation: Timeline.Presentation,
    postActions: PostActions,
) {
    Column(modifier) {
        item.ancestors.forEachIndexed { index, ancestor ->
            key(ancestor.uri.uri) {
                Post(
                    modifier = Modifier.fillMaxWidth(),
                    paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                    presentationLookaheadScope = presentationLookaheadScope,
                    hasMutedWords = item.isMuted && !ancestor.authorMuted,
                    now = now,
                    post = ancestor,
                    threadGate = null,
                    isAnchoredInTimeline = false,
                    isMainPost = false,
                    showEngagementMetrics = false,
                    avatarShape = when (index) {
                        0 -> ReplyThreadStartImageShape
                        else -> ReplyThreadImageShape
                    },
                    sharedElementPrefix = sharedElementPrefix,
                    createdAt = ancestor.createdAt,
                    presentation = presentation,
                    appliedLabels = item.appliedLabels,
                    postActions = postActions,
                    timeline = {
                        Timeline(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(top = 60.dp),
                        )
                    },
                )
            }
        }

        key(item.post.uri.uri) {
            Post(
                modifier = Modifier.fillMaxWidth(),
                paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                presentationLookaheadScope = presentationLookaheadScope,
                hasMutedWords = item.isMuted && !item.post.authorMuted,
                now = now,
                post = item.post,
                threadGate = item.threadGate,
                isAnchoredInTimeline = true,
                isMainPost = true,
                showEngagementMetrics = showEngagementMetrics,
                avatarShape = RoundedPolygonShape.Circle,
                sharedElementPrefix = sharedElementPrefix,
                createdAt = item.post.createdAt,
                presentation = presentation,
                appliedLabels = item.appliedLabels,
                postActions = postActions,
            )
        }

        item.replies.forEach { node ->
            key(node.post.uri.uri) {
                ReplyNodeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                    presentationLookaheadScope = presentationLookaheadScope,
                    node = node,
                    sharedElementPrefix = sharedElementPrefix,
                    now = now,
                    showEngagementMetrics = showEngagementMetrics,
                    presentation = presentation,
                    postActions = postActions,
                )
            }
        }
    }
}

@Composable
private fun ReplyNodeCard(
    modifier: Modifier = Modifier,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    presentationLookaheadScope: LookaheadScope,
    node: ReplyNode,
    sharedElementPrefix: String,
    now: Instant,
    showEngagementMetrics: Boolean,
    presentation: Timeline.Presentation,
    postActions: PostActions,
) {
    val flatRows = remember(node.post.uri, node.children) {
        flattenReplyNodes(node.children, depth = 1)
    }

    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.cardElevation(),
    ) {
        Column {
            Post(
                modifier = Modifier.fillMaxWidth(),
                paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                presentationLookaheadScope = presentationLookaheadScope,
                hasMutedWords = node.isMuted && !node.post.authorMuted,
                now = now,
                post = node.post,
                threadGate = node.threadGate,
                isAnchoredInTimeline = false,
                isMainPost = false,
                showEngagementMetrics = showEngagementMetrics,
                avatarShape = RoundedPolygonShape.Circle,
                sharedElementPrefix = sharedElementPrefix,
                createdAt = node.post.createdAt,
                presentation = presentation,
                appliedLabels = node.appliedLabels,
                postActions = postActions,
                timeline = {
                    if (node.children.isNotEmpty()) {
                        Timeline(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(top = 60.dp),
                        )
                    }
                },
            )

            flatRows.forEach { row ->
                key(row.node.post.uri.uri) {
                    FlatReplyRow(
                        paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                        presentationLookaheadScope = presentationLookaheadScope,
                        row = row,
                        sharedElementPrefix = sharedElementPrefix,
                        now = now,
                        showEngagementMetrics = showEngagementMetrics,
                        presentation = presentation,
                        postActions = postActions,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlatReplyRow(
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    presentationLookaheadScope: LookaheadScope,
    row: FlatReplyRow,
    sharedElementPrefix: String,
    now: Instant,
    showEngagementMetrics: Boolean,
    presentation: Timeline.Presentation,
    postActions: PostActions,
) {
    val connectorColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawReplyConnectors(
                    depth = row.depth,
                    ancestorContinuations = row.ancestorContinuations,
                    isLastSibling = row.isLastSibling,
                    hasChildren = row.hasChildren,
                    indentPerDepthPx = IndentPerDepth.toPx(),
                    threadLineXOffsetPx = ThreadLineXOffset.toPx(),
                    curveRadiusPx = CurveRadius.toPx(),
                    strokeWidthPx = ConnectorStrokeWidth.toPx(),
                    avatarCenterFromTopPx = AvatarCenterFromTop.toPx(),
                    rowOverlapPx = RowOverlap.toPx(),
                    color = connectorColor,
                )
            },
    ) {
        Post(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = row.depth * IndentPerDepth),
            paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
            presentationLookaheadScope = presentationLookaheadScope,
            hasMutedWords = row.node.isMuted && !row.node.post.authorMuted,
            now = now,
            post = row.node.post,
            threadGate = row.node.threadGate,
            isAnchoredInTimeline = false,
            isMainPost = false,
            showEngagementMetrics = showEngagementMetrics,
            avatarShape = RoundedPolygonShape.Circle,
            sharedElementPrefix = sharedElementPrefix,
            createdAt = row.node.post.createdAt,
            presentation = presentation,
            appliedLabels = row.node.appliedLabels,
            postActions = postActions,
            timeline = {
                if (row.hasChildren) {
                    Timeline(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(top = 60.dp),
                    )
                }
            },
        )
    }
}

private data class FlatReplyRow(
    val node: ReplyNode,
    val depth: Int,
    val isLastSibling: Boolean,
    val ancestorContinuations: BooleanArray,
    val hasChildren: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FlatReplyRow) return false
        return node == other.node &&
            depth == other.depth &&
            isLastSibling == other.isLastSibling &&
            ancestorContinuations.contentEquals(other.ancestorContinuations) &&
            hasChildren == other.hasChildren
    }

    override fun hashCode(): Int {
        var r = node.hashCode()
        r = 31 * r + depth
        r = 31 * r + isLastSibling.hashCode()
        r = 31 * r + ancestorContinuations.contentHashCode()
        r = 31 * r + hasChildren.hashCode()
        return r
    }
}

private fun flattenReplyNodes(
    nodes: List<ReplyNode>,
    depth: Int = 0,
    ancestorContinuations: BooleanArray = booleanArrayOf(),
    // Thread a mutable counter through every recursive call so the
    // ceiling is global, not per-branch.
    remaining: IntArray = intArrayOf(MAX_FLAT_REPLY_ROWS),
): List<FlatReplyRow> = buildList {
    for ((index, node) in nodes.withIndex()) {
        if (remaining[0] <= 0) break // when global ceiling is hit, stop
        remaining[0]--

        val isLastSibling = index == nodes.lastIndex
        add(
            FlatReplyRow(
                node = node,
                depth = depth,
                isLastSibling = isLastSibling,
                ancestorContinuations = ancestorContinuations,
                hasChildren = node.children.isNotEmpty(),
            ),
        )
        if (node.children.isNotEmpty()) {
            addAll(
                flattenReplyNodes(
                    nodes = node.children,
                    depth = depth + 1,
                    ancestorContinuations = ancestorContinuations + !isLastSibling,
                    remaining = remaining, // same array, shared across recursion
                ),
            )
        }
    }
}

private fun DrawScope.drawReplyConnectors(
    depth: Int,
    ancestorContinuations: BooleanArray,
    isLastSibling: Boolean,
    hasChildren: Boolean,
    indentPerDepthPx: Float,
    threadLineXOffsetPx: Float,
    curveRadiusPx: Float,
    strokeWidthPx: Float,
    avatarCenterFromTopPx: Float,
    rowOverlapPx: Float,
    color: Color,
) {
    val stroke = Stroke(
        width = strokeWidthPx,
        cap = StrokeCap.Butt,
        join = StrokeJoin.Round,
    )

    fun lineX(level: Int) = threadLineXOffsetPx + level * indentPerDepthPx

    val parentLineX = lineX(depth - 1)
    val childLineX = lineX(depth)
    val curveTopY = avatarCenterFromTopPx - curveRadiusPx

    for (level in 0 until depth - 1) {
        if (ancestorContinuations[level]) {
            drawLine(
                color = color,
                start = Offset(lineX(level), -rowOverlapPx),
                end = Offset(lineX(level), size.height),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Butt,
            )
        }
    }

    if (!isLastSibling) {
        drawLine(
            color = color,
            start = Offset(parentLineX, -rowOverlapPx),
            end = Offset(parentLineX, size.height),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Butt,
        )
    }

    drawPath(
        path = Path().apply {
            moveTo(parentLineX, -rowOverlapPx)
            lineTo(parentLineX, curveTopY)
            quadraticTo(
                x1 = parentLineX,
                y1 = avatarCenterFromTopPx,
                x2 = parentLineX + curveRadiusPx,
                y2 = avatarCenterFromTopPx, // end
            )
            lineTo(childLineX, avatarCenterFromTopPx)
        },
        color = color,
        style = stroke,
    )

    if (hasChildren) {
        drawLine(
            color = color,
            start = Offset(childLineX, avatarCenterFromTopPx),
            end = Offset(childLineX, size.height),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Butt,
        )
    }
}

private operator fun BooleanArray.plus(element: Boolean): BooleanArray =
    copyOf(size + 1).also { it[size] = element }

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
    get() = isThreadedAncestor || isThreadedAnchor || this is TimelineItem.ReplyTree

private const val DefaultMaxPostsInThread = 3
private val IndentPerDepth = 24.dp
private val ThreadLineXOffset = 13.dp
private val ConnectorStrokeWidth = 2.dp
private val CurveRadius = 10.dp
private val AvatarCenterFromTop = 28.dp
private val RowOverlap = 6.dp
private const val MAX_FLAT_REPLY_ROWS = 24
