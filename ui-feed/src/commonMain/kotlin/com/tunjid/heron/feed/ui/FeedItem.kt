package com.tunjid.heron.feed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
        ) {
            Row(

                horizontalArrangement = spacedBy(16.dp),
            ) {
                val author: Profile = item.post.author
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
                        createdAt = item.indexedAt,
                        author = author,
                    )
                    PostReasonLine(
                        item = item,
                        onOpenUser = onProfileClicked,
                    )
                    if (item is FeedItem.Reply) {
                        PostReplyLine(item.parentPost.author, onProfileClicked)
                    }
                }
            }
            Column(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalArrangement = spacedBy(8.dp),
            ) {
                PostText(
                    post = item.post,
                    onClick = { onPostClicked(item.post) },
                    onOpenUser = onProfileClicked
                )
                PostFeature(
                    now = now,
                    post = item.post,
                    onOpenImage = onImageClicked,
                    onOpenPost = onPostClicked
                )
            }
            PostActions(
                replyCount = format(item.post.replyCount),
                repostCount = format(item.post.repostCount),
                likeCount = format(item.post.likeCount),
                reposted = false,
                liked = false,
                iconSize = 16.dp,
                onReplyToPost = onReplyToPost,
            )
        }
    }
}
