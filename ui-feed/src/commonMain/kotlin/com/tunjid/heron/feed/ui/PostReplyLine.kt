package com.tunjid.heron.feed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import heron.ui_feed.generated.resources.Res
import heron.ui_feed.generated.resources.reply
import heron.ui_feed.generated.resources.reply_to
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PostReplyLine(
    replyingTo: Profile,
    onReplyTapped: (Profile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clickable { onReplyTapped(replyingTo) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = spacedBy(4.dp),
    ) {
        Icon(
            modifier = Modifier.size(12.dp),
            imageVector = Icons.AutoMirrored.Rounded.Reply,
            contentDescription = stringResource(Res.string.reply),
            tint = MaterialTheme.typography.bodySmall.color,
        )

        Text(
            text = stringResource(
                Res.string.reply_to,
                replyingTo.displayName ?: replyingTo.handle.id
            ),
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
