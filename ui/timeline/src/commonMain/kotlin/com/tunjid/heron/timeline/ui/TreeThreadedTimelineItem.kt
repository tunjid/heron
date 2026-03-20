package com.tunjid.heron.timeline.ui

import androidx.compose.animation.animateBounds
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.tunjid.heron.data.core.models.AppliedLabels.Companion.warned
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.timeline.ui.post.Post
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.childThreadNode
import com.tunjid.heron.timeline.utilities.authorMuted
import com.tunjid.heron.timeline.utilities.createdAt
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.see_more_posts
import heron.ui.timeline.generated.resources.show_more
import kotlin.math.min
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

/**
 * Unified composable for rendering both [TimelineItem.Threaded.Tree] and
 * [TimelineItem.Threaded.Linear] items.
 *
 * By using a single composable for both view modes, the same post URI always produces
 * the same composition key (via [key]) regardless of whether the user is viewing the
 * tree or linear layout. This compositional permanence enables animating bounds
 * when toggling between views.
 *
 * Iteration is delegated to [ThreadedItemIterator.onEachNode], which walks the node
 * hierarchy and invokes an inline lambda with per-node metadata (depth, avatar shape,
 * connector info, etc.) passed as parameters. Because the values are lambda parameters
 * rather than mutable properties, they are safe to capture in deferred lambdas like
 * [Modifier.drawBehind][androidx.compose.ui.draw.drawBehind].
 *
 * - **Tree mode**: Posts are indented by depth × [IndentPerDepth] and connected with
 *   curved reply connector lines drawn in [drawReplyConnectors].
 * - **Linear mode**: Posts are rendered flat (depth 0) with inter-post decorations
 *   between them — [BrokenTimeline], [Timeline] spacers, [Spacer] gaps, or [ShowMore]
 *   buttons — populated by the iterator in [ThreadedItemIterator.interPostDecorations].
 *
 * For linear mode, the number of visible nodes is limited by a saveable `maxNodes` state
 * and can be expanded with the "Show more" button.
 */
@Composable
internal fun ThreadedTimelineItem(
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    presentationLookaheadScope: LookaheadScope,
    item: TimelineItem.Threaded,
    sharedElementPrefix: String,
    now: Instant,
    showEngagementMetrics: Boolean,
    presentation: Timeline.Presentation,
    postActions: PostActions,
) {
    val connectorColor = MaterialTheme.colorScheme.surfaceContainerHighest

    val isLinear = item is TimelineItem.Threaded.Linear
    var maxNodes by rememberSaveable(isLinear) {
        mutableStateOf(if (isLinear) DefaultMaxPostsInLinearThread else Int.MAX_VALUE)
    }

    val iterator = remember(::ThreadedItemIterator)

    iterator.onEachNode(
        item = item,
        maxItems = maxNodes,
    ) {
            node,
            index,
            depth,
            isMainPost,
            isAnchoredInTimeline,
            avatarShape,
            showTimeline,
            ancestorContinuations,
            isLastSibling,
            hasChildren,
        ->
        key(node.post.uri.uri) {
            Post(
                modifier = Modifier
                    .ifTrue(depth > 0) {
                        drawBehind {
                            drawReplyConnectors(
                                depth = depth,
                                ancestorContinuations = ancestorContinuations,
                                isLastSibling = isLastSibling,
                                hasChildren = hasChildren,
                                indentPerDepthPx = IndentPerDepth.toPx(),
                                threadLineXOffsetPx = ThreadLineXOffset.toPx(),
                                curveRadiusPx = CurveRadius.toPx(),
                                strokeWidthPx = ConnectorStrokeWidth.toPx(),
                                avatarCenterFromTopPx = AvatarCenterFromTop.toPx(),
                                rowOverlapPx = RowOverlap.toPx(),
                                color = connectorColor,
                            )
                        }
                    }
                    .fillMaxWidth()
                    .padding(start = depth * IndentPerDepth)
                    .animateBounds(presentationLookaheadScope),
                paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                presentationLookaheadScope = presentationLookaheadScope,
                hasMutedWords = node.isMuted && !node.post.authorMuted,
                now = now,
                post = node.post,
                threadGate = node.threadGate,
                isAnchoredInTimeline = isAnchoredInTimeline,
                isMainPost = isMainPost,
                showEngagementMetrics = showEngagementMetrics,
                avatarShape = avatarShape,
                sharedElementPrefix = sharedElementPrefix,
                createdAt = node.post.createdAt,
                presentation = presentation,
                appliedLabels = node.appliedLabels,
                postActions = postActions,
                timeline = {
                    if (showTimeline) {
                        Timeline(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(top = 60.dp),
                        )
                    }
                },
            )
        }
        for (decoration in iterator.interPostDecorations) {
            when (decoration) {
                ThreadedItemIterator.InterPostDecoration.BrokenTimeline ->
                    key(node.decorationKey(decoration)) {
                        BrokenTimeline(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .childThreadNode(videoId = null),
                            onClick = {
                                postActions.onPostAction(
                                    PostAction.OfPost(
                                        post = node.post,
                                        isMainPost = isMainPost,
                                        warnedAppliedLabels = node.appliedLabels.warned(),
                                    ),
                                )
                            },
                        )
                    }

                ThreadedItemIterator.InterPostDecoration.ExtraTimeline ->
                    key(node.decorationKey(decoration)) {
                        Timeline(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .height(
                                    if (index == 0) 16.dp
                                    else 12.dp,
                                )
                                .childThreadNode(videoId = null),
                        )
                    }
                ThreadedItemIterator.InterPostDecoration.ExtraSpacer ->
                    key(node.decorationKey(decoration)) {
                        Spacer(
                            Modifier
                                .height(2.dp)
                                .childThreadNode(videoId = null),
                        )
                    }

                ThreadedItemIterator.InterPostDecoration.ShowMore ->
                    key(node.decorationKey(decoration)) {
                        ShowMore {
                            maxNodes += DefaultMaxPostsInLinearThread
                        }
                    }
            }
        }
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

private fun TimelineItem.Threaded.Node.decorationKey(
    decoration: ThreadedItemIterator.InterPostDecoration,
) = "${post.uri.uri}-$decoration"

private fun DrawScope.drawReplyConnectors(
    depth: Int,
    ancestorContinuations: List<Boolean>,
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

@Stable
internal class ThreadedItemIterator {

    enum class InterPostDecoration {
        BrokenTimeline,
        ExtraTimeline,
        ExtraSpacer,
        ShowMore,
    }

    val interPostDecorations: MutableList<InterPostDecoration> = mutableListOf()

    inline fun onEachNode(
        item: TimelineItem.Threaded,
        maxItems: Int,
        block: (
            node: TimelineItem.Threaded.Node,
            index: Int,
            depth: Int,
            isMainPost: Boolean,
            isAnchoredInTimeline: Boolean,
            avatarShape: RoundedPolygonShape,
            showTimeline: Boolean,
            ancestorContinuations: List<Boolean>,
            isLastSibling: Boolean,
            hasChildren: Boolean,
        ) -> Unit,
    ) {
        when (item) {
            is TimelineItem.Threaded.Tree -> onEachTreeNode(item, block)
            is TimelineItem.Threaded.Linear -> onEachLinearNode(item, maxItems, block)
        }
    }

    @PublishedApi
    internal inline fun onEachTreeNode(
        item: TimelineItem.Threaded.Tree,
        block: (
            node: TimelineItem.Threaded.Node,
            index: Int,
            depth: Int,
            isMainPost: Boolean,
            isAnchoredInTimeline: Boolean,
            avatarShape: RoundedPolygonShape,
            showTimeline: Boolean,
            ancestorContinuations: List<Boolean>,
            isLastSibling: Boolean,
            hasChildren: Boolean,
        ) -> Unit,
    ) {
        // Anchor at depth 0
        block(
            item.anchor,
            0,
            0,
            true,
            false,
            RoundedPolygonShape.Circle,
            item.replies.isNotEmpty(),
            emptyList(),
            true,
            item.replies.isNotEmpty(),
        )
        interPostDecorations.clear()

        // Replies at depth 1
        item.replies.forEachIndexed { replyIndex, reply ->
            val replyIsLastSibling = replyIndex == item.replies.lastIndex
            val replyHasChildren = reply.children.isNotEmpty()

            block(
                reply,
                replyIndex,
                1,
                false,
                false,
                RoundedPolygonShape.Circle,
                replyHasChildren,
                emptyList(),
                replyIsLastSibling,
                replyHasChildren,
            )
            interPostDecorations.clear()

            // Children at depth 2
            reply.children.forEachIndexed { childIndex, child ->
                val childIsLastSibling = childIndex == reply.children.lastIndex
                val childHasChildren = child.children.isNotEmpty()

                block(
                    child,
                    childIndex,
                    2,
                    false,
                    false,
                    RoundedPolygonShape.Circle,
                    childHasChildren,
                    listOf(!replyIsLastSibling),
                    childIsLastSibling,
                    childHasChildren,
                )
                interPostDecorations.clear()

                // Grandchildren at depth 3
                child.children.forEachIndexed { grandchildIndex, grandchild ->
                    block(
                        grandchild,
                        grandchildIndex,
                        3,
                        false,
                        false,
                        RoundedPolygonShape.Circle,
                        false,
                        listOf(!replyIsLastSibling, !childIsLastSibling),
                        grandchildIndex == child.children.lastIndex,
                        false,
                    )
                    interPostDecorations.clear()
                }
            }
        }
    }

    @PublishedApi
    internal inline fun onEachLinearNode(
        item: TimelineItem.Threaded.Linear,
        maxItems: Int,
        block: (
            node: TimelineItem.Threaded.Node,
            index: Int,
            depth: Int,
            isMainPost: Boolean,
            isAnchoredInTimeline: Boolean,
            avatarShape: RoundedPolygonShape,
            showTimeline: Boolean,
            ancestorContinuations: List<Boolean>,
            isLastSibling: Boolean,
            hasChildren: Boolean,
        ) -> Unit,
    ) {
        val isAncestor = item.generation != null && item.generation!! <= -1
        val isAnchor = item.generation == 0L
        val isAncestorOrAnchor = isAncestor || isAnchor
        val nodes = item.nodes
        val limit = min(maxItems, nodes.size)

        for (nodeIndex in 0 until limit) {
            val currentNode = nodes[nodeIndex]

            // Skip duplicate consecutive posts
            if (nodeIndex > 0 && nodes[nodeIndex].post.uri == nodes[nodeIndex - 1].post.uri) continue

            val avatarShape = when {
                isAnchor -> RoundedPolygonShape.Circle
                isAncestor ->
                    if (nodes.size == 1) ReplyThreadStartImageShape
                    else ReplyThreadImageShape

                else -> when (nodeIndex) {
                    0 ->
                        if (nodes.size == 1) RoundedPolygonShape.Circle
                        else ReplyThreadStartImageShape

                    nodes.lastIndex -> ReplyThreadEndImageShape
                    else -> ReplyThreadImageShape
                }
            }

            // Populate inter-post decorations
            if (nodeIndex != nodes.lastIndex) {
                if (nodeIndex == 0 && item.hasBreak) {
                    interPostDecorations.add(InterPostDecoration.BrokenTimeline)
                } else {
                    interPostDecorations.add(InterPostDecoration.ExtraTimeline)
                }
            }
            if (nodeIndex == nodes.lastIndex - 1 && !isAncestorOrAnchor && maxItems >= nodes.size) {
                interPostDecorations.add(InterPostDecoration.ExtraSpacer)
            }
            if (nodeIndex == limit - 1 && nodes.size > maxItems) {
                interPostDecorations.add(InterPostDecoration.ShowMore)
            }

            block(
                currentNode,
                nodeIndex,
                0,
                currentNode.post == item.post,
                isAnchor,
                avatarShape,
                nodeIndex != nodes.lastIndex || isAncestor,
                emptyList(),
                false,
                false,
            )
            interPostDecorations.clear()
        }
    }
}

private val IndentPerDepth = 24.dp
private val ThreadLineXOffset = 13.dp
private val ConnectorStrokeWidth = 2.dp
private val CurveRadius = 10.dp
private val AvatarCenterFromTop = 28.dp
private val RowOverlap = 6.dp
private const val DefaultMaxPostsInLinearThread = 3
