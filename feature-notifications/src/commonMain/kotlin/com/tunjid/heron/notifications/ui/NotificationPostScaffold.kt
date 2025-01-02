package com.tunjid.heron.notifications.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.images.shapes.ImageShape
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.ui.SharedElementScope
import com.tunjid.heron.ui.posts.PostActions
import com.tunjid.heron.ui.posts.PostHeadline
import com.tunjid.heron.ui.posts.PostText
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import kotlinx.datetime.Instant

@Composable
internal fun NotificationPostScaffold(
    sharedElementScope: SharedElementScope,
    now: Instant,
    notification: Notification.PostAssociated,
    sharedElementPrefix: String,
    onProfileClicked: (Notification, Profile) -> Unit,
    onPostClicked: (Post) -> Unit,
    onImageClicked: (Uri) -> Unit,
    onReplyToPost: () -> Unit,
) = with(sharedElementScope) {
    Box {
        Column(
            modifier = Modifier,
        ) {
            PostAttribution(
                sharedElementScope = sharedElementScope,
                avatarShape = ImageShape.Circle,
                onProfileClicked = onProfileClicked,
                notification = notification,
                sharedElementPrefix = sharedElementPrefix,
                now = now,
                createdAt = notification.indexedAt
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
                    post = notification.associatedPost,
                    sharedElementPrefix = sharedElementPrefix,
                    sharedElementScope = sharedElementScope,
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = { onPostClicked(notification.associatedPost) },
                    onProfileClicked = { onProfileClicked(notification, it) }
                )
//                PostEmbed(
//                    now = now,
//                    embed = embed,
//                    quote = post.quote,
//                    postId = post.cid,
//                    sharedElementPrefix = sharedElementPrefix,
//                    sharedElementScope = sharedElementScope,
//                    onOpenImage = onImageClicked,
//                    onPostClicked = onPostClicked,
//                )

                PostActions(
                    replyCount = format(notification.associatedPost.replyCount),
                    repostCount = format(notification.associatedPost.repostCount),
                    likeCount = format(notification.associatedPost.likeCount),
                    reposted = notification.associatedPost.viewerStats?.reposted == true,
                    liked = notification.associatedPost.viewerStats?.liked == true,
                    iconSize = 16.dp,
                    postId = notification.associatedPost.cid,
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
    avatarShape: ImageShape,
    onProfileClicked: (Notification, Profile) -> Unit,
    notification: Notification.PostAssociated,
    sharedElementPrefix: String,
    now: Instant,
    createdAt: Instant,
) = with(sharedElementScope) {
    val post = notification.associatedPost
    Row(
        horizontalArrangement = spacedBy(8.dp),
    ) {
        updatedMovableSharedElementOf(
            modifier = Modifier
                .size(48.dp)
                .clip(avatarShape)
                .clickable { onProfileClicked(notification, post.author) },
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