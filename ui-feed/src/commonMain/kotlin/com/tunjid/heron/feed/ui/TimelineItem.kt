package com.tunjid.heron.feed.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.feed.utilities.createdAt
import com.tunjid.heron.feed.utilities.format
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.images.shapes.ImageShape
import com.tunjid.heron.images.shapes.toImageShape
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import kotlinx.datetime.Instant

@Composable
fun TimelineItem(
    modifier: Modifier = Modifier,
    movableSharedElementScope: MovableSharedElementScope,
    now: Instant,
    item: TimelineItem,
    sharedElementPrefix: String,
    onPostClicked: (Post) -> Unit,
    onProfileClicked: Post?.(Profile) -> Unit,
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
                    top = 16.dp,
                    bottom = 8.dp,
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
                movableSharedElementScope = movableSharedElementScope,
                item = item,
                now = now,
                sharedElementPrefix = sharedElementPrefix,
                onProfileClicked = onProfileClicked,
                onPostClicked = onPostClicked,
                onImageClicked = onImageClicked,
                onReplyToPost = onReplyToPost
            ) else SinglePost(
                movableSharedElementScope = movableSharedElementScope,
                post = item.post,
                embed = item.post.embed,
                avatarShape =
                if (item is TimelineItem.Thread) ReplyThreadEndImageShape
                else ImageShape.Circle,
                avatarSharedElementKey = item.post.avatarSharedElementKey(sharedElementPrefix),
                now = now,
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
    movableSharedElementScope: MovableSharedElementScope,
    item: TimelineItem.Thread,
    sharedElementPrefix: String,
    now: Instant,
    onProfileClicked: Post?.(Profile) -> Unit,
    onPostClicked: (Post) -> Unit,
    onImageClicked: (Uri) -> Unit,
    onReplyToPost: () -> Unit
) {
    Column {
        item.posts.forEachIndexed { index, post ->
            if (index == 0 || item.posts[index].cid != item.posts[index - 1].cid) {
                SinglePost(
                    movableSharedElementScope = movableSharedElementScope,
                    post = post,
                    embed = post.embed,
                    avatarShape =
                    if (item.posts.size == 1) ReplyThreadImageShape
                    else when (index) {
                        0 -> ReplyThreadStartImageShape
                        item.posts.lastIndex -> ReplyThreadEndImageShape
                        else -> ReplyThreadImageShape
                    },
                    avatarSharedElementKey = post.avatarSharedElementKey(sharedElementPrefix),
                    now = now,
                    createdAt = post.createdAt,
                    onProfileClicked = onProfileClicked,
                    onPostClicked = onPostClicked,
                    onImageClicked = onImageClicked,
                    onReplyToPost = onReplyToPost,
                    timeline = {
                        if (index != item.posts.lastIndex) Timeline(
                            Modifier
                                .matchParentSize()
                                .padding(top = 52.dp)
                        )
                    }
                )
                if (index != item.posts.lastIndex) Timeline(
                    Modifier.height(if (index == 0) 16.dp else 12.dp)
                )
                if (index == item.posts.lastIndex - 1) Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SinglePost(
    movableSharedElementScope: MovableSharedElementScope,
    now: Instant,
    post: Post,
    embed: Embed?,
    avatarShape: ImageShape,
    avatarSharedElementKey: String,
    createdAt: Instant,
    onProfileClicked: Post?.(Profile) -> Unit,
    onPostClicked: (Post) -> Unit,
    onImageClicked: (Uri) -> Unit,
    onReplyToPost: () -> Unit,
    timeline: @Composable BoxScope.() -> Unit = {},
) {
    Box {
        timeline()
        Column(
            modifier = Modifier,
        ) {
            Row(
                horizontalArrangement = spacedBy(8.dp),
            ) {
                movableSharedElementScope.updatedMovableSharedElementOf(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(avatarShape)
                        .clickable { onProfileClicked(post, post.author) },
                    key = avatarSharedElementKey,
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
                    )

//                if (item is TimelineItem.Reply) {
//                    PostReplyLine(item.parentPost.author, onProfileClicked)
//                }
                }
            }
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
                    onClick = { onPostClicked(post) },
                    onProfileClicked = onProfileClicked
                )
                PostEmbed(
                    now = now,
                    embed = embed,
                    quote = post.quote,
                    onOpenImage = onImageClicked,
                    onOpenPost = onPostClicked
                )
                PostActions(
                    replyCount = format(post.replyCount),
                    repostCount = format(post.repostCount),
                    likeCount = format(post.likeCount),
                    reposted = post.viewerStats?.reposted == true,
                    liked = post.viewerStats?.liked == true,
                    iconSize = 16.dp,
                    onReplyToPost = onReplyToPost,
                )
            }
        }
    }
}

@Composable
private fun Timeline(
    modifier: Modifier = Modifier
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
fun TimelineCard(
    item: TimelineItem,
    modifier: Modifier = Modifier,
    onPostClicked: (Post) -> Unit,
    content: @Composable () -> Unit
) {
    if (item is TimelineItem.Thread && item.generation == 0L) Surface(
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
    ).toImageShape()

private val ReplyThreadImageShape =
    ImageShape.Polygon(
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
    ).toImageShape()


fun Post.avatarSharedElementKey(
    prefix: String,
): String = "$prefix-${cid.id}-${author.did.id}"
