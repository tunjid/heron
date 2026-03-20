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
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.AppliedLabels.Companion.warned
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.timeline.ui.post.Post
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.childThreadNode
import com.tunjid.heron.timeline.utilities.authorMuted
import com.tunjid.heron.timeline.utilities.createdAt
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.see_more_posts
import heron.ui.timeline.generated.resources.show_more
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ThreadedLinearItem(
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    presentationLookaheadScope: LookaheadScope,
    item: TimelineItem.Threaded.Linear,
    sharedElementPrefix: String,
    now: Instant,
    showEngagementMetrics: Boolean,
    presentation: Timeline.Presentation,
    postActions: PostActions,
) {
    var maxNodes by rememberSaveable {
        mutableStateOf(DefaultMaxPostsInThread)
    }
    val limitedNodes = remember(item.nodes, maxNodes) {
        item.nodes.take(maxNodes)
    }
    limitedNodes.forEachIndexed { index, node ->
        if (index == 0 || item.nodes[index].post.uri != item.nodes[index - 1].post.uri) {
            key(node.post.uri.uri) {
                Post(
                    modifier = Modifier,
                    paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                    presentationLookaheadScope = presentationLookaheadScope,
                    hasMutedWords = item.isMuted && !item.post.authorMuted,
                    now = now,
                    post = node.post,
                    threadGate = node.threadGate,
                    isAnchoredInTimeline = item.generation == 0L,
                    isMainPost = node.post == item.post,
                    avatarShape = when {
                        item.isThreadedAnchor -> RoundedPolygonShape.Circle
                        item.isThreadedAncestor ->
                            if (item.nodes.size == 1) ReplyThreadStartImageShape
                            else ReplyThreadImageShape

                        else -> when (index) {
                            0 ->
                                if (item.nodes.size == 1) RoundedPolygonShape.Circle
                                else ReplyThreadStartImageShape

                            item.nodes.lastIndex -> ReplyThreadEndImageShape
                            else -> ReplyThreadImageShape
                        }
                    },
                    sharedElementPrefix = sharedElementPrefix,
                    createdAt = node.post.createdAt,
                    presentation = presentation,
                    showEngagementMetrics = showEngagementMetrics,
                    appliedLabels = node.appliedLabels,
                    postActions = postActions,
                    timeline = {
                        if (index != item.nodes.lastIndex || item.isThreadedAncestor) Timeline(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(top = 60.dp),
                        )
                    },
                )
            }
            if (index != item.nodes.lastIndex) {
                if (index == 0 && item.hasBreak) BrokenTimeline(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .childThreadNode(videoId = null),
                    onClick = {
                        postActions.onPostAction(
                            PostAction.OfPost(
                                post = node.post,
                                isMainPost = node.post == item.post,
                                warnedAppliedLabels = node.appliedLabels.warned(),
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
            if (index == item.nodes.lastIndex - 1 && !item.isThreadedAncestorOrAnchor && maxNodes >= item.nodes.size) Spacer(
                Modifier
                    .height(2.dp)
                    .childThreadNode(videoId = null),
            )
        }
    }

    if (item.nodes.size > maxNodes) ShowMore {
        maxNodes += DefaultMaxPostsInThread
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

private const val DefaultMaxPostsInThread = 3
