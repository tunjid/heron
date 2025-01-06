package com.tunjid.heron.timeline.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.shapes.toRoundedPolygonShape
import com.tunjid.heron.timeline.utilities.createdAt
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.ui.SharedElementScope
import com.tunjid.heron.ui.posts.PostActions
import com.tunjid.heron.ui.posts.PostHeadline
import com.tunjid.heron.ui.posts.PostText
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
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
    onImageClicked: (Uri) -> Unit,
    onReplyToPost: () -> Unit,
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
                onImageClicked = onImageClicked,
                onReplyToPost = onReplyToPost
            ) else SinglePost(
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
                onImageClicked = onImageClicked,
                onReplyToPost = onReplyToPost,
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
    onImageClicked: (Uri) -> Unit,
    onReplyToPost: () -> Unit,
) {
    Column {
        item.posts.forEachIndexed { index, post ->
            if (index == 0 || item.posts[index].cid != item.posts[index - 1].cid) {
                SinglePost(
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
                    onImageClicked = onImageClicked,
                    onReplyToPost = onReplyToPost,
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
private fun SinglePost(
    sharedElementScope: SharedElementScope,
    now: Instant,
    post: Post,
    embed: Embed?,
    isAnchoredInTimeline: Boolean,
    avatarShape: RoundedPolygonShape,
    sharedElementPrefix: String,
    createdAt: Instant,
    onProfileClicked: (Post, Profile) -> Unit,
    onPostClicked: (Post) -> Unit,
    onImageClicked: (Uri) -> Unit,
    onReplyToPost: () -> Unit,
    timeline: @Composable (BoxScope.() -> Unit) = {},
) {
    Box {
        timeline()
        Column(
            modifier = Modifier,
        ) {
            PostAttribution(
                sharedElementScope = sharedElementScope,
                avatarShape = avatarShape,
                onProfileClicked = onProfileClicked,
                post = post,
                sharedElementPrefix = sharedElementPrefix,
                now = now,
                createdAt = createdAt,
            )
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier.padding(
                    start = 24.dp,
                    bottom = 8.dp
                ),
                verticalArrangement = spacedBy(8.dp),
            ) {
                PostText(
                    post = post,
                    sharedElementPrefix = sharedElementPrefix,
                    sharedElementScope = sharedElementScope,
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = { onPostClicked(post) },
                    onProfileClicked = onProfileClicked
                )
                PostEmbed(
                    now = now,
                    embed = embed,
                    quote = post.quote,
                    postId = post.cid,
                    sharedElementPrefix = sharedElementPrefix,
                    sharedElementScope = sharedElementScope,
                    onOpenImage = onImageClicked,
                    onPostClicked = onPostClicked,
                )
                if (isAnchoredInTimeline) PostDate(
                    modifier = Modifier.padding(
                        vertical = 8.dp,
                    ),
                    time = post.createdAt,
                )
                PostActions(
                    replyCount = format(post.replyCount),
                    repostCount = format(post.repostCount),
                    likeCount = format(post.likeCount),
                    reposted = post.viewerStats?.reposted == true,
                    liked = post.viewerStats?.liked == true,
                    iconSize = 16.dp,
                    postId = post.cid,
                    sharedElementPrefix = sharedElementPrefix,
                    sharedElementScope = sharedElementScope,
                    onReplyToPost = onReplyToPost,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PostAttribution(
    sharedElementScope: SharedElementScope,
    avatarShape: RoundedPolygonShape,
    onProfileClicked: (Post, Profile) -> Unit,
    post: Post,
    sharedElementPrefix: String,
    now: Instant,
    createdAt: Instant,
) = with(sharedElementScope) {
    Row(
        horizontalArrangement = spacedBy(8.dp),
    ) {
        updatedMovableSharedElementOf(
            modifier = Modifier
                .size(48.dp)
                .clip(avatarShape)
                .clickable { onProfileClicked(post, post.author) },
            key = post.avatarSharedElementKey(sharedElementPrefix),
            state = remember(post.author.avatar) {
                ImageArgs(
                    url = post.author.avatar?.uri,
                    contentScale = ContentScale.Crop,
                    contentDescription = post.author.displayName ?: post.author.handle.id,
                    shape = avatarShape,
                )
            },
            sharedElement = { state, modifier ->
                AsyncImage(state, modifier)
            }
        )
        //      onClick = { onOpenUser(UserDid(author.did)) },
        //      fallbackColor = author.handle.color(),
        Column(Modifier.weight(1f)) {
            PostHeadline(
                now = now,
                createdAt = createdAt,
                author = post.author,
                postId = post.cid,
                sharedElementPrefix = sharedElementPrefix,
                sharedElementScope = sharedElementScope,
            )

//                if (item is TimelineItem.Reply) {
//                    PostReplyLine(item.parentPost.author, onProfileClicked)
//                }
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