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
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.see_more_posts
import heron.ui.timeline.generated.resources.show_more
import kotlin.jvm.JvmInline
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
 * hierarchy and invokes an inline lambda with five parameters per node:
 * - [TimelineItem.Threaded.Node] — the post data
 * - [NodePosition] — packed index and depth (two [Int]s in a [Long])
 * - [NodeFlags] — bitmask of boolean metadata (main post, anchored, timeline visibility,
 *   sibling/child status, and ancestor continuation lines)
 * - [RoundedPolygonShape] — avatar shape
 * - [NodeDecorations] — bitmask of decorations to render after the post
 *
 * Because all per-node values are lambda parameters (not mutable properties), they are
 * safe to capture in deferred lambdas like
 * [Modifier.drawBehind][androidx.compose.ui.draw.drawBehind].
 *
 * - **Tree mode**: Posts are indented by depth × [IndentPerDepth] and connected with
 *   curved reply connector lines drawn in [drawReplyConnectors]. Ancestor continuation
 *   lines are encoded in [NodeFlags] and read via [NodeFlags.hasAncestorContinuation].
 * - **Linear mode**: Posts are rendered flat (depth 0) with inter-post decorations
 *   between them — [BrokenTimeline], [Timeline] spacers, [Spacer] gaps, or [ShowMore]
 *   buttons — driven by the [NodeDecorations] bitmask.
 *
 * For linear mode, the number of visible nodes is limited by a saveable `maxNodes` state
 * and can be expanded with the "Show more" button.
 */
@Composable
internal fun ThreadedPost(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
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

    Column(
        modifier = modifier,
    ) {
        ThreadedItemIterator.onEachNode(
            item = item,
            maxItems = maxNodes,
        ) { node, position, flags, decorations, avatarShape ->
            val index = position[NodeDimension.Index]
            val depth = position[NodeDimension.Depth]
            key(node.post.uri.uri) {
                Post(
                    modifier = Modifier
                        .ifTrue(depth > 0) {
                            drawBehind {
                                drawReplyConnectors(
                                    depth = depth,
                                    flags = flags,
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
                        .animateBounds(lookaheadScope = presentationLookaheadScope, boundsTransform = paneTransitionScope.resizeAwareBoundsTransform),
                    paneTransitionScope = paneTransitionScope,
                    presentationLookaheadScope = presentationLookaheadScope,
                    hasMutedWords = node.isMuted && !node.post.authorMuted,
                    now = now,
                    post = node.post,
                    threadGate = node.threadGate,
                    isAnchoredInTimeline = flags.isAnchoredInTimeline,
                    isMainPost = flags.isMainPost,
                    showEngagementMetrics = showEngagementMetrics,
                    avatarShape = avatarShape,
                    sharedElementPrefix = sharedElementPrefix,
                    createdAt = node.post.createdAt,
                    presentation = presentation,
                    appliedLabels = node.appliedLabels,
                    postActions = postActions,
                    timeline = {
                        if (flags.showTimeline) {
                            Timeline(
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(top = 60.dp),
                            )
                        }
                    },
                )
            }
            if (decorations.has(NodeDecoration.BrokenTimeline)) {
                key(node.decorationKey(NodeDecoration.BrokenTimeline)) {
                    BrokenTimeline(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .animateBounds(lookaheadScope = presentationLookaheadScope, boundsTransform = paneTransitionScope.resizeAwareBoundsTransform)
                            .childThreadNode(videoId = null),
                        onClick = {
                            postActions.onPostAction(
                                PostAction.OfPost(
                                    post = node.post,
                                    isMainPost = flags.isMainPost,
                                    warnedAppliedLabels = node.appliedLabels.warned(),
                                ),
                            )
                        },
                    )
                }
            }
            if (decorations.has(NodeDecoration.ExtraTimeline)) {
                key(node.decorationKey(NodeDecoration.ExtraTimeline)) {
                    Timeline(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .height(
                                if (index == 0) 16.dp
                                else 12.dp,
                            )
                            .animateBounds(lookaheadScope = presentationLookaheadScope, boundsTransform = paneTransitionScope.resizeAwareBoundsTransform)
                            .childThreadNode(videoId = null),
                    )
                }
            }
            if (decorations.has(NodeDecoration.ExtraSpacer)) {
                key(node.decorationKey(NodeDecoration.ExtraSpacer)) {
                    Spacer(
                        Modifier
                            .height(2.dp)
                            .childThreadNode(videoId = null),
                    )
                }
            }
            if (decorations.has(NodeDecoration.ShowMore)) {
                key(node.decorationKey(NodeDecoration.ShowMore)) {
                    ShowMore(
                        modifier = Modifier
                            .animateBounds(lookaheadScope = presentationLookaheadScope, boundsTransform = paneTransitionScope.resizeAwareBoundsTransform),
                    ) {
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
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

private fun DrawScope.drawReplyConnectors(
    depth: Int,
    flags: NodeFlags,
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
        if (flags.hasAncestorContinuation(level)) {
            drawLine(
                color = color,
                start = Offset(lineX(level), -rowOverlapPx),
                end = Offset(lineX(level), size.height),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Butt,
            )
        }
    }

    if (!flags.isLastSibling) {
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

    if (flags.hasChildren) {
        drawLine(
            color = color,
            start = Offset(childLineX, avatarCenterFromTopPx),
            end = Offset(childLineX, size.height),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Butt,
        )
    }
}

private enum class NodeDimension(val shift: Int) {
    Index(shift = 0),
    Depth(shift = 32),
}

/**
 * Packed pair of [Int] values identified by [NodeDimension] entries.
 * Uses a [Long] backing field — lower 32 bits for [NodeDimension.Index],
 * upper 32 bits for [NodeDimension.Depth].
 */
@JvmInline
private value class NodePosition(val packed: Long = 0L) {

    operator fun get(dimension: NodeDimension): Int =
        (packed ushr dimension.shift).toInt()
}

@Suppress("NOTHING_TO_INLINE")
private inline fun NodePosition(
    index: Int,
    depth: Int,
): NodePosition = NodePosition(
    (index.toLong() and 0xFFFFFFFFL) or ((depth.toLong() and 0xFFFFFFFFL) shl 32),
)

/**
 * Bitmask of boolean flags describing a node's role in the thread.
 * Uses a [Long] backing field to avoid boxing.
 */
@JvmInline
private value class NodeFlags(val mask: Long = 0L) {

    val isMainPost: Boolean get() = mask and IS_MAIN_POST != 0L
    val isAnchoredInTimeline: Boolean get() = mask and IS_ANCHORED_IN_TIMELINE != 0L
    val showTimeline: Boolean get() = mask and SHOW_TIMELINE != 0L
    val isLastSibling: Boolean get() = mask and IS_LAST_SIBLING != 0L
    val hasChildren: Boolean get() = mask and HAS_CHILDREN != 0L

    fun hasAncestorContinuation(level: Int): Boolean =
        mask and (1L shl (ANCESTOR_CONTINUATION_BASE_SHIFT + level)) != 0L

    companion object {
        const val IS_MAIN_POST = 1L shl 0
        const val IS_ANCHORED_IN_TIMELINE = 1L shl 1
        const val SHOW_TIMELINE = 1L shl 2
        const val IS_LAST_SIBLING = 1L shl 3
        const val HAS_CHILDREN = 1L shl 4
        const val ANCESTOR_CONTINUATION_BASE_SHIFT = 5
        const val ANCESTOR_CONTINUATION_DEPTH_0 = 1L shl 5
        const val ANCESTOR_CONTINUATION_DEPTH_1 = 1L shl 6
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun NodeFlags(
    isMainPost: Boolean = false,
    isAnchoredInTimeline: Boolean = false,
    showTimeline: Boolean = false,
    isLastSibling: Boolean = false,
    hasChildren: Boolean = false,
    ancestorContinuationDepth0: Boolean = false,
    ancestorContinuationDepth1: Boolean = false,
): NodeFlags = NodeFlags(
    (if (isMainPost) NodeFlags.IS_MAIN_POST else 0L)
        or (if (isAnchoredInTimeline) NodeFlags.IS_ANCHORED_IN_TIMELINE else 0L)
        or (if (showTimeline) NodeFlags.SHOW_TIMELINE else 0L)
        or (if (isLastSibling) NodeFlags.IS_LAST_SIBLING else 0L)
        or (if (hasChildren) NodeFlags.HAS_CHILDREN else 0L)
        or (if (ancestorContinuationDepth0) NodeFlags.ANCESTOR_CONTINUATION_DEPTH_0 else 0L)
        or (if (ancestorContinuationDepth1) NodeFlags.ANCESTOR_CONTINUATION_DEPTH_1 else 0L),
)

private enum class NodeDecoration {
    BrokenTimeline,
    ExtraTimeline,
    ExtraSpacer,
    ShowMore,
}

/**
 * Bitmask of [NodeDecoration] values that should be rendered after a given node.
 * Uses a [Long] backing field to avoid list allocations during iteration.
 */
@JvmInline
private value class NodeDecorations(val mask: Long = 0L) {

    fun has(decoration: NodeDecoration): Boolean =
        mask and (1L shl decoration.ordinal) != 0L

    operator fun plus(decoration: NodeDecoration): NodeDecorations =
        NodeDecorations(mask or (1L shl decoration.ordinal))
}

private fun TimelineItem.Threaded.Node.decorationKey(
    decoration: NodeDecoration,
) = "${post.uri.uri}-$decoration"

@Stable
private object ThreadedItemIterator {

    inline fun onEachNode(
        item: TimelineItem.Threaded,
        maxItems: Int,
        block: (
            node: TimelineItem.Threaded.Node,
            position: NodePosition,
            flags: NodeFlags,
            decorations: NodeDecorations,
            avatarShape: RoundedPolygonShape,
        ) -> Unit,
    ) {
        when (item) {
            is TimelineItem.Threaded.Tree -> onEachTreeNode(item, block)
            is TimelineItem.Threaded.Linear -> onEachLinearNode(item, maxItems, block)
        }
    }

    inline fun onEachTreeNode(
        item: TimelineItem.Threaded.Tree,
        block: (
            node: TimelineItem.Threaded.Node,
            position: NodePosition,
            flags: NodeFlags,
            nodeDecorations: NodeDecorations,
            avatarShape: RoundedPolygonShape,
        ) -> Unit,
    ) {
        val noDecorations = NodeDecorations()
        val hasReplies = item.replies.isNotEmpty()

        // Anchor at depth 0
        block(
            item.anchor,
            NodePosition(
                index = 0,
                depth = 0,
            ),
            NodeFlags(
                isMainPost = true,
                isLastSibling = true,
                showTimeline = hasReplies,
                hasChildren = hasReplies,
            ),
            noDecorations,
            RoundedPolygonShape.Circle,
        )

        // Replies at depth 1
        item.replies.forEachIndexed { replyIndex, reply ->
            val replyIsLastSibling = replyIndex == item.replies.lastIndex
            val replyHasChildren = reply.children.isNotEmpty()

            block(
                reply,
                NodePosition(
                    index = replyIndex,
                    depth = 1,
                ),
                NodeFlags(
                    showTimeline = replyHasChildren,
                    hasChildren = replyHasChildren,
                    isLastSibling = replyIsLastSibling,
                ),
                noDecorations,
                RoundedPolygonShape.Circle,
            )

            // Children at depth 2
            reply.children.forEachIndexed { childIndex, child ->
                val childIsLastSibling = childIndex == reply.children.lastIndex
                val childHasChildren = child.children.isNotEmpty()

                block(
                    child,
                    NodePosition(
                        index = childIndex,
                        depth = 2,
                    ),
                    NodeFlags(
                        showTimeline = childHasChildren,
                        hasChildren = childHasChildren,
                        isLastSibling = childIsLastSibling,
                        ancestorContinuationDepth0 = !replyIsLastSibling,
                    ),
                    noDecorations,
                    RoundedPolygonShape.Circle,
                )

                // Grandchildren at depth 3
                child.children.forEachIndexed { grandchildIndex, grandchild ->
                    block(
                        grandchild,
                        NodePosition(
                            index = grandchildIndex,
                            depth = 3,
                        ),
                        NodeFlags(
                            isLastSibling = grandchildIndex == child.children.lastIndex,
                            ancestorContinuationDepth0 = !replyIsLastSibling,
                            ancestorContinuationDepth1 = !childIsLastSibling,
                        ),
                        noDecorations,
                        RoundedPolygonShape.Circle,
                    )
                }
            }
        }
    }

    inline fun onEachLinearNode(
        item: TimelineItem.Threaded.Linear,
        maxItems: Int,
        block: (
            node: TimelineItem.Threaded.Node,
            position: NodePosition,
            flags: NodeFlags,
            decorations: NodeDecorations,
            avatarShape: RoundedPolygonShape,
        ) -> Unit,
    ) {
        val isAncestor = item.isThreadedAncestor
        val isAnchor = item.isThreadedAnchor
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

            // Build inter-post decorations bitmask
            var decorations = NodeDecorations()
            if (nodeIndex != nodes.lastIndex) {
                decorations +=
                    if (nodeIndex == 0 && item.hasBreak) NodeDecoration.BrokenTimeline
                    else NodeDecoration.ExtraTimeline
            }
            if (nodeIndex == nodes.lastIndex - 1 && !isAncestorOrAnchor && maxItems >= nodes.size) {
                decorations += NodeDecoration.ExtraSpacer
            }
            if (nodeIndex == limit - 1 && nodes.size > maxItems) {
                decorations += NodeDecoration.ShowMore
            }

            block(
                currentNode,
                NodePosition(
                    index = nodeIndex,
                    depth = 0,
                ),
                NodeFlags(
                    isMainPost = currentNode.post == item.post,
                    isAnchoredInTimeline = isAnchor,
                    showTimeline = nodeIndex != nodes.lastIndex || isAncestor,
                ),
                decorations,
                avatarShape,
            )
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
