package com.tunjid.heron.feed.ui

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.FeedItem
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.feed.utilities.format
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import kotlinx.datetime.Instant

@Composable
fun FeedItem(
    modifier: Modifier = Modifier,
    now: Instant,
    item: FeedItem,
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
            if (item is FeedItem.Repost) {
                PostReasonLine(
                    item = item,
                    onOpenUser = onProfileClicked,
                )
            }
            if (item is FeedItem.Reply) {
                SinglePost(
                    post = item.rootPost,
                    now = now,
                    indexedAt = item.indexedAt,
                    onProfileClicked = onProfileClicked,
                    onPostClicked = onPostClicked,
                    onImageClicked = onImageClicked,
                    onReplyToPost = onReplyToPost
                )
                SinglePost(
                    post = item.parentPost,
                    now = now,
                    indexedAt = item.indexedAt,
                    onProfileClicked = onProfileClicked,
                    onPostClicked = onPostClicked,
                    onImageClicked = onImageClicked,
                    onReplyToPost = onReplyToPost
                )
            }
            SinglePost(
                post = item.post,
                now = now,
                indexedAt = item.indexedAt,
                onProfileClicked = onProfileClicked,
                onPostClicked = onPostClicked,
                onImageClicked = onImageClicked,
                onReplyToPost = onReplyToPost
            )
        }
    }
}

@Composable
private fun SinglePost(
    now: Instant,
    post: Post,
    indexedAt: Instant,
    onProfileClicked: (Profile) -> Unit,
    onPostClicked: (Post) -> Unit,
    onImageClicked: (Uri) -> Unit,
    onReplyToPost: () -> Unit
) {
    Column(
        modifier = Modifier,
    ) {
        Row(
            horizontalArrangement = spacedBy(16.dp),
        ) {
            val author: Profile = post.author
            AsyncImage(
                modifier = Modifier
                    .size(48.dp),
                args = ImageArgs(
                    url = author.avatar?.uri,
                    contentScale = ContentScale.Crop,
                    contentDescription = author.displayName ?: author.handle.id,
                    shape = CircleShape,
                ),
            )
            //      onClick = { onOpenUser(UserDid(author.did)) },
            //      fallbackColor = author.handle.color(),
            Column(Modifier.weight(1f)) {
                PostHeadline(
                    now = now,
                    createdAt = indexedAt,
                    author = author,
                )

//                if (item is FeedItem.Reply) {
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
            PostFeature(
                now = now,
                post = post,
                onOpenImage = onImageClicked,
                onOpenPost = onPostClicked
            )
        }
        PostActions(
            modifier = Modifier.padding(horizontal = 24.dp),
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
