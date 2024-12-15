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
    onPostClicked: (Post) -> Unit,
    onProfileClicked: Post?.(Profile) -> Unit,
    onImageClicked: (Uri) -> Unit,
    onReplyToPost: () -> Unit,
) {
    ElevatedCard(
        modifier = modifier,
        onClick = { onPostClicked(item.post) }
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
            if (item is TimelineItem.Reply) {
                PostReplies(
                    movableSharedElementScope = movableSharedElementScope,
                    item = item,
                    now = now,
                    onProfileClicked = onProfileClicked,
                    onPostClicked = onPostClicked,
                    onImageClicked = onImageClicked,
                    onReplyToPost = onReplyToPost
                )
            }
            SinglePost(
                movableSharedElementScope = movableSharedElementScope,
                post = item.post,
                embed = item.post.embed,
                avatarShape =
                if (item is TimelineItem.Reply) ReplyThreadEndImageShape
                else ImageShape.Circle,
                avatarSharedElementKey = item.post.avatarSharedElementKey(item.sourceId),
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
private fun PostReplies(
    movableSharedElementScope: MovableSharedElementScope,
    item: TimelineItem.Reply,
    now: Instant,
    onProfileClicked: Post?.(Profile) -> Unit,
    onPostClicked: (Post) -> Unit,
    onImageClicked: (Uri) -> Unit,
    onReplyToPost: () -> Unit
) {
    Column {
        SinglePost(
            movableSharedElementScope = movableSharedElementScope, post = item.rootPost,
            embed = item.rootPost.embed,
            avatarShape = ReplyThreadStartImageShape,
            avatarSharedElementKey = item.rootPost.avatarSharedElementKey(item.sourceId),
            now = now,
            createdAt = item.rootPost.createdAt,
            onProfileClicked = onProfileClicked,
            onPostClicked = onPostClicked,
            onImageClicked = onImageClicked,
            onReplyToPost = onReplyToPost,
            timeline = {
                Timeline(
                    Modifier
                        .matchParentSize()
                        .padding(top = 52.dp)
                )
            }
        )
        Timeline(Modifier.height(16.dp))
        if (item.rootPost.cid != item.parentPost.cid) {
            SinglePost(movableSharedElementScope = movableSharedElementScope,
                post = item.parentPost,
                embed = item.parentPost.embed,
                avatarShape = ReplyThreadImageShape,
                avatarSharedElementKey = item.parentPost.avatarSharedElementKey(item.sourceId),
                now = now,
                createdAt = item.parentPost.createdAt,
                onProfileClicked = onProfileClicked,
                onPostClicked = onPostClicked,
                onImageClicked = onImageClicked,
                onReplyToPost = onReplyToPost,
                timeline = {
                    Timeline(
                        Modifier
                            .matchParentSize()
                            .padding(top = 48.dp)
                    )
                }
            )
            Timeline(Modifier.height(12.dp))
            Spacer(Modifier.height(4.dp))
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
                    start = 32.dp,
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
            if (index == 2 || index == 3) 14.dp
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
    sourceId: String,
): String = "$sourceId-${cid.id}-${author.did.id}"
