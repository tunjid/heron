/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.timeline.ui.post

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import com.tunjid.heron.ui.text.rememberFormattedTextPost

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PostText(
    post: Post,
    sharedElementPrefix: String,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onProfileClicked: (Post, Profile) -> Unit,
) = with(paneMovableElementSharedTransitionScope) {
    val maybeExternalLink = (post.embed as? ExternalEmbed)?.uri?.uri
    val text = post.record
        ?.text
        ?.removeSuffix(maybeExternalLink.orEmpty())
        ?.trim()
        .orEmpty()

    if (text.isBlank()) Spacer(Modifier.height(0.dp))
    else Text(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .paneSharedElement(
                key = post.textSharedElementKey(
                    prefix = sharedElementPrefix,
                ),
            ),
        text = rememberFormattedTextPost(
            text = text,
            textLinks = post.record?.links ?: emptyList(),
            onProfileClicked = { onProfileClicked(post, it) }
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
    )
}

private fun Post.textSharedElementKey(
    prefix: String,
): String = "$prefix-${cid.id}-text"
