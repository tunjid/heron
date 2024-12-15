package com.tunjid.heron.feed.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
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
import kotlinx.datetime.Instant

@Composable
fun TimelineItem(
    modifier: Modifier = Modifier,
    now: Instant,
    item: TimelineItem,
    onPostClicked: (Post) -> Unit,
    onProfileClicked: (Profile) -> Unit,
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
                    onOpenUser = onProfileClicked,
                )
            }
            if (item is TimelineItem.Reply) {
                PostReplies(
                    item = item,
                    now = now,
                    onProfileClicked = onProfileClicked,
                    onPostClicked = onPostClicked,
                    onImageClicked = onImageClicked,
                    onReplyToPost = onReplyToPost
                )
            }
            SinglePost(
                post = item.post,
                embed = item.post.embed,
                avatarShape =
                if (item is TimelineItem.Reply) ReplyThreadEndImageShape
                else ImageShape.Circle,
                now = now,
                createdAt = item.post.createdAt,
                onProfileClicked = onProfileClicked,
                onPostClicked = onPostClicked,
                onImageClicked = onImageClicked,
                onReplyToPost = onReplyToPost
            )
        }
    }
}

@Composable
private fun PostReplies(
    item: TimelineItem.Reply,
    now: Instant,
    onProfileClicked: (Profile) -> Unit,
    onPostClicked: (Post) -> Unit,
    onImageClicked: (Uri) -> Unit,
    onReplyToPost: () -> Unit
) {
    Box {
        Box(
            Modifier
                .matchParentSize()
        ) {
            TimelineThread()
        }
        Column {
            SinglePost(
                post = item.rootPost,
                embed = item.rootPost.embed,
                avatarShape = ReplyThreadStartImageShape,
                now = now,
                createdAt = item.rootPost.createdAt,
                onProfileClicked = onProfileClicked,
                onPostClicked = onPostClicked,
                onImageClicked = onImageClicked,
                onReplyToPost = onReplyToPost
            )
            Spacer(Modifier.height(16.dp))
            if (item.rootPost.cid != item.parentPost.cid) {
                SinglePost(
                    post = item.parentPost,
                    embed = item.parentPost.embed,
                    avatarShape = ReplyThreadImageShape,
                    now = now,
                    createdAt = item.parentPost.createdAt,
                    onProfileClicked = onProfileClicked,
                    onPostClicked = onPostClicked,
                    onImageClicked = onImageClicked,
                    onReplyToPost = onReplyToPost
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SinglePost(
    now: Instant,
    post: Post,
    embed: Embed?,
    avatarShape: ImageShape,
    createdAt: Instant,
    onProfileClicked: (Profile) -> Unit,
    onPostClicked: (Post) -> Unit,
    onImageClicked: (Uri) -> Unit,
    onReplyToPost: () -> Unit
) {
    Column(
        modifier = Modifier,
    ) {
        Row(
            horizontalArrangement = spacedBy(8.dp),
        ) {
            AsyncImage(
                modifier = Modifier
                    .size(48.dp)
                    .clip(avatarShape)
                    .clickable { onProfileClicked(post.author) },
                args = ImageArgs(
                    url = post.author.avatar?.uri,
                    contentScale = ContentScale.Crop,
                    contentDescription = post.author.displayName ?: post.author.handle.id,
                    shape = avatarShape,
                ),
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
                onOpenUser = onProfileClicked
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
                reposted = false,
                liked = false,
                iconSize = 16.dp,
                onReplyToPost = onReplyToPost,
            )
        }
    }
}

@Composable
private fun TimelineThread() {
    Spacer(
        Modifier
            .offset(x = 2.dp)
            .padding(top = 52.dp, bottom = 4.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .fillMaxHeight()
            .width(2.dp)
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

