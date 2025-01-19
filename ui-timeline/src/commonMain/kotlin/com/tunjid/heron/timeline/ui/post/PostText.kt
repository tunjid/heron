package com.tunjid.heron.timeline.ui.post

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.ui.SharedElementScope
import com.tunjid.heron.ui.text.rememberFormattedTextPost

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PostText(
    post: Post,
    sharedElementPrefix: String,
    sharedElementScope: SharedElementScope,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onProfileClicked: (Post, Profile) -> Unit,
) = with(sharedElementScope) {
    val maybeExternalLink = (post.embed as? ExternalEmbed)?.uri?.uri
    val text = post.record?.text?.removeSuffix(maybeExternalLink.orEmpty())?.trim().orEmpty()

    if (text.isBlank()) Spacer(Modifier.height(0.dp))
    else Text(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .sharedElement(
                key = post.textSharedElementKey(
                    prefix = sharedElementPrefix,
                ),
            ),
        text = rememberFormattedTextPost(
            text = text,
            textLinks = post.record?.links ?: emptyList(),
            onProfileClicked = { onProfileClicked(post, it) }
        ),
        style = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
    )
}

private fun Post.textSharedElementKey(
    prefix: String,
): String = "$prefix-${cid.id}-text"
