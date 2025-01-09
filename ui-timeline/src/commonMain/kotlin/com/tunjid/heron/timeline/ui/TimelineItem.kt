package com.tunjid.heron.timeline.ui

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
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.timeline.ui.post.Post
import com.tunjid.heron.timeline.ui.post.PostReasonLine
import com.tunjid.heron.timeline.utilities.createdAt
import com.tunjid.heron.ui.SharedElementScope
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.shapes.toRoundedPolygonShape
import heron.ui_timeline.generated.resources.Res
import heron.ui_timeline.generated.resources.see_more_posts
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource

@Composable
fun TimelineItem(
    modifier: Modifier = Modifier,
    sharedElementScope: SharedElementScope,
    now: Instant,
    item: TimelineItem,
    sharedElementPrefix: String,
    onPostClicked: (Post) -> Unit,
    onProfileClicked: (Post, Profile) -> Unit,
    onPostMediaClicked: (Post, Embed.Media, Int) -> Unit,
    onReplyToPost: () -> Unit,
    onPostInteraction: (Post.Interaction) -> Unit,
) {
    TimelineCard(
        item = item,
        modifier = modifier,
        onPostClicked = onPostClicked,
    ) {
        Column(
            modifier = Modifier
                .padding(
                    top = if (item.isThreadedAnchor) 0.dp
                    else 16.dp,
                    bottom = if (item.isThreadedAncestorOrAnchor) 0.dp
                    else 8.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
        ) {
            if (item is TimelineItem.Repost) {
                PostReasonLine(
                    modifier = Modifier.padding(
                        start = 32.dp,
                        bottom = 4.dp
                    ),
                    item = item,
                    onProfileClicked = onProfileClicked,
                )
            }
            if (item is TimelineItem.Thread) ThreadedPost(
                sharedElementScope = sharedElementScope,
                item = item,
                sharedElementPrefix = sharedElementPrefix,
                now = now,
                onProfileClicked = onProfileClicked,
                onPostClicked = onPostClicked,
                onPostMediaClicked = onPostMediaClicked,
                onReplyToPost = onReplyToPost,
                onPostInteraction = onPostInteraction,
            ) else
                Post(
                    sharedElementScope = sharedElementScope,
                    now = now,
                    post = item.post,
                    embed = item.post.embed,
                    isAnchoredInTimeline = false,
                    avatarShape =
                    if (item is TimelineItem.Thread) ReplyThreadEndImageShape
                    else RoundedPolygonShape.Circle,
                    sharedElementPrefix = sharedElementPrefix,
                    createdAt = item.post.createdAt,
                    onProfileClicked = onProfileClicked,
                    onPostClicked = onPostClicked,
                    onPostMediaClicked = onPostMediaClicked,
                    onReplyToPost = onReplyToPost,
                    onPostInteraction = onPostInteraction,
                )
        }
    }
}

@Composable
private fun ThreadedPost(
    sharedElementScope: SharedElementScope,
    item: TimelineItem.Thread,
    sharedElementPrefix: String,
    now: Instant,
    onProfileClicked: (Post, Profile) -> Unit,
    onPostClicked: (Post) -> Unit,
    onPostMediaClicked: (Post, Embed.Media, Int) -> Unit,
    onReplyToPost: () -> Unit,
    onPostInteraction: (Post.Interaction) -> Unit,
) {
    Column {
        item.posts.forEachIndexed { index, post ->
            if (index == 0 || item.posts[index].cid != item.posts[index - 1].cid) {

                Post(
                    sharedElementScope = sharedElementScope,
                    now = now,
                    post = post,
                    embed = post.embed,
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
                    onProfileClicked = onProfileClicked,
                    onPostClicked = onPostClicked,
                    onPostMediaClicked = onPostMediaClicked,
                    onReplyToPost = onReplyToPost,
                    onPostInteraction = onPostInteraction,
                    timeline = {
                        if (index != item.posts.lastIndex || item.isThreadedAncestor) Timeline(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(top = 52.dp)
                        )
                    }
                )
                if (index != item.posts.lastIndex)
                    if (index == 0 && item.hasBreak) BrokenTimeline {
                        onPostClicked(post)
                    }
                    else Timeline(
                        modifier = Modifier.height(
                            if (index == 0) 16.dp
                            else 12.dp
                        )
                    )
                if (index == item.posts.lastIndex - 1 && !item.isThreadedAncestorOrAnchor) Spacer(
                    Modifier.height(4.dp)
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
    onClick: () -> Unit,
) {
    Column(
        Modifier
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
    onPostClicked: (Post) -> Unit,
    content: @Composable () -> Unit,
) {
    if (item.isThreadedAncestorOrAnchor) Surface(
        modifier = modifier,
        onClick = { onPostClicked(item.post) },
        content = { content() },
    )
    else ElevatedCard(
        modifier = modifier,
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
    prefix: String,
): String = "$prefix-${cid.id}-${author.did.id}"

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