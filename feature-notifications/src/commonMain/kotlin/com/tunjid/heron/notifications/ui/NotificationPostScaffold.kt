package com.tunjid.heron.notifications.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.post.PostActions
import com.tunjid.heron.timeline.ui.post.PostHeadline
import com.tunjid.heron.timeline.ui.post.PostText
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import kotlinx.datetime.Instant

@Composable
internal fun NotificationPostScaffold(
    modifier: Modifier = Modifier,
    panedSharedElementScope: PanedSharedElementScope,
    now: Instant,
    notification: Notification.PostAssociated,
    onProfileClicked: (Notification.PostAssociated, Profile) -> Unit,
    onPostClicked: (Notification.PostAssociated) -> Unit,
    onPostMediaClicked: (Post, Embed.Media, Int) -> Unit,
    onReplyToPost: () -> Unit,
    onPostInteraction: (Post.Interaction) -> Unit,
) {
    Box {
        Column(
            modifier = modifier,
        ) {
            PostAttribution(
                panedSharedElementScope = panedSharedElementScope,
                avatarShape = RoundedPolygonShape.Circle,
                onProfileClicked = onProfileClicked,
                notification = notification,
                sharedElementPrefix = notification.sharedElementPrefix(),
                now = now,
                createdAt = notification.indexedAt
            )
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier.padding(
                    start = 64.dp,
                    bottom = 8.dp
                ),
                verticalArrangement = spacedBy(8.dp),
            ) {
                PostText(
                    post = notification.associatedPost,
                    sharedElementPrefix = notification.sharedElementPrefix(),
                    panedSharedElementScope = panedSharedElementScope,
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = { onPostClicked(notification) },
                    onProfileClicked = { _, profile ->
                        onProfileClicked(notification, profile)
                    }
                )
//                PostEmbed(
//                    now = now,
//                    embed = embed,
//                    quote = post.quote,
//                    postId = post.cid,
//                    sharedElementPrefix = sharedElementPrefix,
//                    sharedElementScope = sharedElementScope,
//                    onPostMediaClicked = onPostMediaClicked,
//                    onPostClicked = onPostClicked,
//                )

                PostActions(
                    replyCount = format(notification.associatedPost.replyCount),
                    repostCount = format(notification.associatedPost.repostCount),
                    likeCount = format(notification.associatedPost.likeCount),
                    repostUri = notification.associatedPost.viewerStats?.repostUri,
                    likeUri = notification.associatedPost.viewerStats?.likeUri,
                    iconSize = 16.dp,
                    postId = notification.associatedPost.cid,
                    postUri = notification.associatedPost.uri,
                    sharedElementPrefix = notification.sharedElementPrefix(),
                    panedSharedElementScope = panedSharedElementScope,
                    onReplyToPost = onReplyToPost,
                    onPostInteraction = onPostInteraction,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PostAttribution(
    panedSharedElementScope: PanedSharedElementScope,
    avatarShape: RoundedPolygonShape,
    onProfileClicked: (Notification.PostAssociated, Profile) -> Unit,
    notification: Notification.PostAssociated,
    sharedElementPrefix: String,
    now: Instant,
    createdAt: Instant,
) = with(panedSharedElementScope) {
    val post = notification.associatedPost
    AttributionLayout(
        avatar = {
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
        },
        label = {
            PostHeadline(
                now = now,
                createdAt = createdAt,
                author = post.author,
                postId = post.cid,
                sharedElementPrefix = sharedElementPrefix,
                panedSharedElementScope = panedSharedElementScope,
            )
        }
    )
//    if (item is TimelineItem.Reply) {
//                    PostReplyLine(item.parentPost.author, onProfileClicked)
//                }
}

fun Notification.PostAssociated.sharedElementPrefix(
): String = "notification-${cid.id}"