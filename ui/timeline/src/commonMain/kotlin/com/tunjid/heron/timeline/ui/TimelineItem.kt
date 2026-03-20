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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.timeline.ui.post.Post
import com.tunjid.heron.timeline.ui.post.PostReasonLine
import com.tunjid.heron.timeline.ui.post.feature.EmptyPost
import com.tunjid.heron.timeline.ui.post.feature.LoadingPost
import com.tunjid.heron.timeline.utilities.authorMuted
import com.tunjid.heron.timeline.utilities.createdAt
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import kotlin.time.Instant

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
                    is TimelineItem.Threaded if presentation == Timeline.Presentation.Text.WithEmbed -> ThreadedPost(
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
    item: TimelineItem.Threaded,
    sharedElementPrefix: String,
    now: Instant,
    showEngagementMetrics: Boolean,
    presentation: Timeline.Presentation,
    postActions: PostActions,
) {
    Column(
        modifier = modifier,
    ) {
        ThreadedTimelineItem(
            paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
            presentationLookaheadScope = presentationLookaheadScope,
            item = item,
            sharedElementPrefix = sharedElementPrefix,
            now = now,
            showEngagementMetrics = showEngagementMetrics,
            presentation = presentation,
            postActions = postActions,
        )
    }
}

@Composable
fun Timeline(
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

val ReplyThreadStartImageShape =
    RoundedPolygonShape.RoundedRectangle(
        topStartPercent = 1f,
        topEndPercent = 1f,
        bottomStartPercent = 0.3f,
        bottomEndPercent = 1f,
    )

val ReplyThreadImageShape =
    RoundedPolygonShape.Polygon(
        cornerSizePercentAtIndex = (0..4).map { index ->
            if (index == 2 || index == 3) 2f / 3
            else 1f
        },
    )

internal val ReplyThreadEndImageShape =
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

internal val TimelineItem.isThreadedAncestor
    get() = this is TimelineItem.Threaded.Linear && when (val gen = generation) {
        null -> false
        else -> gen <= -1
    }

internal val TimelineItem.isThreadedAnchor
    get() = this is TimelineItem.Threaded.Linear && generation == 0L

internal val TimelineItem.isThreadedAncestorOrAnchor
    get() = isThreadedAncestor || isThreadedAnchor
