package com.tunjid.heron.feed.ui


import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun PostActions(
    replyCount: String?,
    repostCount: String?,
    likeCount: String?,
    reposted: Boolean,
    liked: Boolean,
    iconSize: Dp,
    onReplyToPost: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = SpaceBetween,
    ) {
        PostAction(
            icon = Icons.Default.ChatBubbleOutline,
            iconSize = iconSize,
            contentDescription = "Reply",
            text = replyCount,
            onClick = onReplyToPost,
        )
        PostAction(
            icon = Icons.Default.Repeat,
            iconSize = iconSize,
            contentDescription = "Repost",
            text = repostCount,
            onClick = {},
            tint = if (reposted) {
                Color.Green
            } else {
                MaterialTheme.colorScheme.outline
            },
        )
        PostAction(
            icon = if (liked) {
                Icons.Default.Favorite
            } else {
                Icons.Default.FavoriteBorder
            },
            iconSize = iconSize,
            contentDescription = "Like",
            text = likeCount,
            onClick = {},
            tint = if (liked) {
                Color.Red
            } else {
                MaterialTheme.colorScheme.outline
            },
        )
        Spacer(Modifier.width(0.dp))
    }
}

@Composable
private fun PostAction(
    icon: ImageVector,
    iconSize: Dp,
    contentDescription: String,
    text: String?,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.outline,
) {
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = spacedBy(4.dp),
    ) {
        Icon(
            modifier = Modifier.size(iconSize),
            painter = rememberVectorPainter(icon),
            contentDescription = contentDescription,
            tint = tint,
        )

        if (text != null) {
            Text(
                text = text,
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall.copy(color = tint),
            )
        }
    }
}
