package com.tunjid.heron.timeline.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.timeline.ui.post.Post
import com.tunjid.heron.timeline.utilities.authorMuted
import com.tunjid.heron.timeline.utilities.createdAt
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import kotlin.time.Instant

@Composable
fun TreePost(
    modifier: Modifier = Modifier,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    presentationLookaheadScope: LookaheadScope,
    item: TimelineItem.Threaded.Tree,
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
                    modifier = Modifier
                        .fillMaxWidth(),
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
                NodeCard(
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
private fun NodeCard(
    modifier: Modifier = Modifier,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    presentationLookaheadScope: LookaheadScope,
    node: TimelineItem.Threaded.Tree.Node,
    sharedElementPrefix: String,
    now: Instant,
    showEngagementMetrics: Boolean,
    presentation: Timeline.Presentation,
    postActions: PostActions,
) {
    val flatRows = remember(node.post.uri, node.children) {
        flattenTreeNodes(node.children, depth = 1)
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
                    FlatNodeRow(
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
private fun FlatNodeRow(
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    presentationLookaheadScope: LookaheadScope,
    row: FlatNodeRow,
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

private data class FlatNodeRow(
    val node: TimelineItem.Threaded.Tree.Node,
    val depth: Int,
    val isLastSibling: Boolean,
    val ancestorContinuations: List<Boolean>,
    val hasChildren: Boolean,
)

private fun flattenTreeNodes(
    nodes: List<TimelineItem.Threaded.Tree.Node>,
    depth: Int = 0,
    ancestorContinuations: List<Boolean> = emptyList(),
): List<FlatNodeRow> {
    var remaining = MAX_FLAT_REPLY_ROWS
    return buildList {
        fun doAddAll(
            innerNodes: List<TimelineItem.Threaded.Tree.Node>,
            innerDepth: Int,
            innerAncestorContinuations: List<Boolean>
        ) {
            for ((index, node) in innerNodes.withIndex()) {
                if (remaining <= 0) return
                remaining--

                val isLastSibling = index == innerNodes.lastIndex
                add(
                    FlatNodeRow(
                        node = node,
                        depth = innerDepth,
                        isLastSibling = isLastSibling,
                        ancestorContinuations = innerAncestorContinuations,
                        hasChildren = node.children.isNotEmpty(),
                    )
                )
                if (node.children.isNotEmpty()) {
                    doAddAll(
                        innerNodes = node.children,
                        innerDepth = innerDepth + 1,
                        innerAncestorContinuations = innerAncestorContinuations + !isLastSibling,
                    )
                }
            }
        }
        doAddAll(nodes, depth, ancestorContinuations)
    }
}

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

private val IndentPerDepth = 24.dp
private val ThreadLineXOffset = 13.dp
private val ConnectorStrokeWidth = 2.dp
private val CurveRadius = 10.dp
private val AvatarCenterFromTop = 28.dp
private val RowOverlap = 6.dp
private const val MAX_FLAT_REPLY_ROWS = 24
