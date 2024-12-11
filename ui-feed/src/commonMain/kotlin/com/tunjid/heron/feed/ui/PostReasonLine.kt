package com.tunjid.heron.feed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.FeedItem
import com.tunjid.heron.data.core.models.Profile
import heron.ui_feed.generated.resources.Res
import heron.ui_feed.generated.resources.repost
import heron.ui_feed.generated.resources.repost_by
//import heron.feature_home.generated.resources.Res
//import heron.feature_home.generated.resources.repost
//import heron.feature_home.generated.resources.repost_by
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PostReasonLine(
    item: FeedItem,
    onOpenUser: (Profile) -> Unit,
) {
    when (item) {
        is FeedItem.Pinned -> PostPinnedReasonLine()
        is FeedItem.Repost -> PostRepostReasonLine(
            repostBy = item.by,
            onOpenUser = onOpenUser
        )
        is FeedItem.Reply,
        is FeedItem.Single -> Unit
    }
}

@Composable
private fun PostRepostReasonLine(
    repostBy: Profile,
    onOpenUser: (Profile) -> Unit,
) {
    PostReasonLine(
        modifier = Modifier.clickable { onOpenUser(repostBy) },
        imageVector = Icons.Rounded.Repeat,
        iconContentDescription = stringResource(Res.string.repost),
        text = stringResource(
            Res.string.repost_by,
            repostBy.displayName ?: repostBy.handle
        ),
    )
}

@Composable
private fun PostPinnedReasonLine() {
    PostReasonLine(
        imageVector = Icons.Rounded.Star,
        iconContentDescription = "Pinned",
        text = "Pinned",
    )
}

@Composable
private fun PostReasonLine(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    iconContentDescription: String,
    text: String,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = spacedBy(4.dp),
    ) {
        Icon(
            modifier = Modifier.size(12.dp),
            imageVector = imageVector,
            contentDescription = iconContentDescription,
            tint = MaterialTheme.typography.bodySmall.color,
        )

        Text(
            text = text,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
