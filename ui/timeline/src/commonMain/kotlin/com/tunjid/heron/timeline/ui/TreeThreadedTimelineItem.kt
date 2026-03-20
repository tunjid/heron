package com.tunjid.heron.timeline.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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

// We need the composition key to be the same, so it needs to be inline
@Suppress("NOTHING_TO_INLINE")
@Composable
internal inline fun ThreadedTreeItem(
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    presentationLookaheadScope: LookaheadScope,
    item: TimelineItem.Threaded.Tree,
    sharedElementPrefix: String,
    now: Instant,
    showEngagementMetrics: Boolean,
    presentation: Timeline.Presentation,
    postActions: PostActions,
) {
    key(item.post.uri.uri) {
        Post(
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
            timeline = {
                if (item.replies.isNotEmpty()) {
                    Timeline(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(top = 60.dp),
                    )
                }
            },
        )
    }

    val connectorColor = MaterialTheme.colorScheme.surfaceContainerHighest

    item.replies.forEachIndexed { replyIndex, reply ->
        val replyIsLastSibling = replyIndex == item.replies.lastIndex
        val replyHasChildren = reply.children.isNotEmpty()

        key(reply.post.uri.uri) {
            Post(
                modifier = Modifier
                    .drawBehind {
                        drawReplyConnectors(
                            depth = 1,
                            ancestorContinuations = emptyList(),
                            isLastSibling = replyIsLastSibling,
                            hasChildren = replyHasChildren,
                            indentPerDepthPx = IndentPerDepth.toPx(),
                            threadLineXOffsetPx = ThreadLineXOffset.toPx(),
                            curveRadiusPx = CurveRadius.toPx(),
                            strokeWidthPx = ConnectorStrokeWidth.toPx(),
                            avatarCenterFromTopPx = AvatarCenterFromTop.toPx(),
                            rowOverlapPx = RowOverlap.toPx(),
                            color = connectorColor,
                        )
                    }
                    .fillMaxWidth()
                    .padding(start = 1 * IndentPerDepth),
                paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                presentationLookaheadScope = presentationLookaheadScope,
                hasMutedWords = reply.isMuted && !reply.post.authorMuted,
                now = now,
                post = reply.post,
                threadGate = reply.threadGate,
                isAnchoredInTimeline = false,
                isMainPost = false,
                showEngagementMetrics = showEngagementMetrics,
                avatarShape = RoundedPolygonShape.Circle,
                sharedElementPrefix = sharedElementPrefix,
                createdAt = reply.post.createdAt,
                presentation = presentation,
                appliedLabels = reply.appliedLabels,
                postActions = postActions,
                timeline = {
                    if (replyHasChildren) {
                        Timeline(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(top = 60.dp),
                        )
                    }
                },
            )
        }

        reply.children.forEachIndexed { childIndex, child ->
            val childIsLastSibling = childIndex == reply.children.lastIndex
            val childHasChildren = child.children.isNotEmpty()

            key(child.post.uri.uri) {
                Post(
                    modifier = Modifier
                        .drawBehind {
                            drawReplyConnectors(
                                depth = 2,
                                ancestorContinuations = listOf(!replyIsLastSibling),
                                isLastSibling = childIsLastSibling,
                                hasChildren = childHasChildren,
                                indentPerDepthPx = IndentPerDepth.toPx(),
                                threadLineXOffsetPx = ThreadLineXOffset.toPx(),
                                curveRadiusPx = CurveRadius.toPx(),
                                strokeWidthPx = ConnectorStrokeWidth.toPx(),
                                avatarCenterFromTopPx = AvatarCenterFromTop.toPx(),
                                rowOverlapPx = RowOverlap.toPx(),
                                color = connectorColor,
                            )
                        }
                        .fillMaxWidth()
                        .padding(start = 2 * IndentPerDepth),
                    paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                    presentationLookaheadScope = presentationLookaheadScope,
                    hasMutedWords = child.isMuted && !child.post.authorMuted,
                    now = now,
                    post = child.post,
                    threadGate = child.threadGate,
                    isAnchoredInTimeline = false,
                    isMainPost = false,
                    showEngagementMetrics = showEngagementMetrics,
                    avatarShape = RoundedPolygonShape.Circle,
                    sharedElementPrefix = sharedElementPrefix,
                    createdAt = child.post.createdAt,
                    presentation = presentation,
                    appliedLabels = child.appliedLabels,
                    postActions = postActions,
                    timeline = {
                        if (childHasChildren) {
                            Timeline(
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(top = 60.dp),
                            )
                        }
                    },
                )
            }

            child.children.forEachIndexed { grandchildIndex, grandchild ->
                val grandchildIsLastSibling = grandchildIndex == child.children.lastIndex
                key(grandchild.post.uri.uri) {
                    Post(
                        modifier = Modifier
                            .drawBehind {
                                drawReplyConnectors(
                                    depth = 3,
                                    ancestorContinuations = listOf(!replyIsLastSibling, !childIsLastSibling),
                                    isLastSibling = grandchildIsLastSibling,
                                    hasChildren = false,
                                    indentPerDepthPx = IndentPerDepth.toPx(),
                                    threadLineXOffsetPx = ThreadLineXOffset.toPx(),
                                    curveRadiusPx = CurveRadius.toPx(),
                                    strokeWidthPx = ConnectorStrokeWidth.toPx(),
                                    avatarCenterFromTopPx = AvatarCenterFromTop.toPx(),
                                    rowOverlapPx = RowOverlap.toPx(),
                                    color = connectorColor,
                                )
                            }
                            .fillMaxWidth()
                            .padding(start = 3 * IndentPerDepth),
                        paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                        presentationLookaheadScope = presentationLookaheadScope,
                        hasMutedWords = grandchild.isMuted && !grandchild.post.authorMuted,
                        now = now,
                        post = grandchild.post,
                        threadGate = grandchild.threadGate,
                        isAnchoredInTimeline = false,
                        isMainPost = false,
                        showEngagementMetrics = showEngagementMetrics,
                        avatarShape = RoundedPolygonShape.Circle,
                        sharedElementPrefix = sharedElementPrefix,
                        createdAt = grandchild.post.createdAt,
                        presentation = presentation,
                        appliedLabels = grandchild.appliedLabels,
                        postActions = postActions,
                        timeline = {},
                    )
                }
            }
        }
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
