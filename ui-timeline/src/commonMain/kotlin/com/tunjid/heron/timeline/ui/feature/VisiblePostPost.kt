package com.tunjid.heron.timeline.ui.feature

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.timeline.ui.PostHeadline
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.images.shapes.ImageShape
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun VisiblePostPost(
    now: Instant,
    post: Post,
    author: Profile,
    sharedElementPrefix: String,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    onClick: () -> Unit,
) {
    FeatureContainer(onClick = onClick) {
        Row(horizontalArrangement = spacedBy(8.dp)) {
            AsyncImage(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.CenterVertically),
                args = ImageArgs(
                    url = author.avatar?.uri,
                    contentDescription = author.displayName ?: author.handle.id,
                    contentScale = ContentScale.Crop,
                    shape = ImageShape.Circle,
                )
            )
            PostHeadline(
                now = now,
                createdAt = post.record?.createdAt ?: remember { Clock.System.now() },
                author = author,
                postId = post.cid,
                sharedElementPrefix = sharedElementPrefix,
                animatedVisibilityScope = animatedVisibilityScope,
                sharedTransitionScope = sharedTransitionScope,
            )
        }
        Text(
            text = post.record?.text ?: "",
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
