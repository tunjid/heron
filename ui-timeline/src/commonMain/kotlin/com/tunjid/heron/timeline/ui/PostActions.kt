package com.tunjid.heron.timeline.ui


import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
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
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Repeat
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
import com.tunjid.heron.data.core.types.Id
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import heron.ui_timeline.generated.resources.Res
import heron.ui_timeline.generated.resources.liked
import heron.ui_timeline.generated.resources.reply
import heron.ui_timeline.generated.resources.repost
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PostActions(
    replyCount: String?,
    repostCount: String?,
    likeCount: String?,
    reposted: Boolean,
    liked: Boolean,
    iconSize: Dp,
    postId: Id,
    sharedElementPrefix: String,
    animatedVisibilityScope: AnimatedVisibilityScope,
    movableSharedElementScope: MovableSharedElementScope,
    modifier: Modifier = Modifier,
    onReplyToPost: () -> Unit,
) = with(movableSharedElementScope) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = SpaceBetween,
    ) {
        PostAction(
            modifier = Modifier
                .sharedElement(
                    state = rememberSharedContentState(
                        postActionSharedElementKey(
                            prefix = sharedElementPrefix,
                            postId = postId,
                            icon = Icons.Rounded.ChatBubbleOutline,
                        )
                    ),
                    animatedVisibilityScope = animatedVisibilityScope,
                ),
            icon = Icons.Rounded.ChatBubbleOutline,
            iconSize = iconSize,
            contentDescription = stringResource(Res.string.reply),
            text = replyCount,
            onClick = onReplyToPost,
        )
        PostAction(
            modifier = Modifier
                .sharedElement(
                    state = rememberSharedContentState(
                        postActionSharedElementKey(
                            prefix = sharedElementPrefix,
                            postId = postId,
                            icon = Icons.Rounded.Repeat,
                        )
                    ),
                    animatedVisibilityScope = animatedVisibilityScope,
                ),
            icon = Icons.Rounded.Repeat,
            iconSize = iconSize,
            contentDescription = stringResource(Res.string.repost),
            text = repostCount,
            onClick = {},
            tint = if (reposted) {
                Color.Green
            } else {
                MaterialTheme.colorScheme.outline
            },
        )
        PostAction(
            modifier = Modifier
                .sharedElement(
                    state = rememberSharedContentState(
                        postActionSharedElementKey(
                            prefix = sharedElementPrefix,
                            postId = postId,
                            icon = Icons.Rounded.Favorite,
                        )
                    ),
                    animatedVisibilityScope = animatedVisibilityScope,
                ),
            icon = if (liked) {
                Icons.Rounded.Favorite
            } else {
                Icons.Rounded.FavoriteBorder
            },
            iconSize = iconSize,
            contentDescription = stringResource(Res.string.liked),
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
    modifier: Modifier = Modifier,
    text: String?,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.outline,
) {
    Row(
        modifier = modifier
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

private fun postActionSharedElementKey(
    prefix: String,
    postId: Id,
    icon: ImageVector,
): String = "$prefix-${postId.id}-${icon.hashCode()}"