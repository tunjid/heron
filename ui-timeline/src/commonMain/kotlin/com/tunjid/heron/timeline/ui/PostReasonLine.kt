package com.tunjid.heron.timeline.ui

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
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelineItem
import heron.ui_timeline.generated.resources.Res
import heron.ui_timeline.generated.resources.repost
import heron.ui_timeline.generated.resources.repost_by
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PostReasonLine(
    modifier: Modifier = Modifier,
    item: TimelineItem,
    onProfileClicked: (Post, Profile) -> Unit,
) {
    when (item) {
        is TimelineItem.Pinned -> PostPinnedReasonLine(
            modifier = modifier
        )

        is TimelineItem.Repost -> PostRepostReasonLine(
            modifier = modifier,
            repostBy = item.by,
            onProfileClicked = {
                onProfileClicked(item.post, it)
            }
        )

        is TimelineItem.Thread,
        is TimelineItem.Single -> Unit
    }
}

@Composable
private fun PostRepostReasonLine(
    modifier: Modifier = Modifier,
    repostBy: Profile,
    onProfileClicked: (Profile) -> Unit,
) {
    PostReasonLine(
        modifier = modifier.clickable { onProfileClicked(repostBy) },
        imageVector = Icons.Rounded.Repeat,
        iconContentDescription = stringResource(Res.string.repost),
        text = stringResource(
            Res.string.repost_by,
            repostBy.displayName ?: repostBy.handle
        ),
    )
}

@Composable
private fun PostPinnedReasonLine(
    modifier: Modifier = Modifier,
) {
    PostReasonLine(
        modifier = modifier,
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
        )

        Text(
            text = text,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
