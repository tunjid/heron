package com.tunjid.heron.feed.ui.feature

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.tunjid.heron.feed.ui.PostHeadline
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Composable
fun VisiblePostPost(
    now: Instant,
    post: Post,
    author: Profile,
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
                    shape = RoundedCornerShape(16.dp)
                )
            )
            PostHeadline(
                now = now,
                createdAt = post.record?.createdAt ?: remember { Clock.System.now() },
                author = author,
            )
        }
        Text(
            text = post.record?.text ?: "",
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
