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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.text.MultilineStyledText
import com.tunjid.heron.ui.text.rememberFormattedTextPost
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.post_thread_position
import heron.ui.timeline.generated.resources.post_thread_position_description
import org.jetbrains.compose.resources.stringResource

@Composable
fun PostText(
    post: Post,
    sharedElementPrefix: String,
    paneTransitionScope: PaneTransitionScope,
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLinkTargetClicked: (Post, LinkTarget) -> Unit,
) = with(paneTransitionScope) {
    val maybeExternalLink = (post.embed as? ExternalEmbed)?.uri?.uri
    val text = post.record
        ?.text
        ?.removeSuffix(maybeExternalLink.orEmpty())
        ?.trim()
        .orEmpty()
    val threadDetails = post.threadDetails

    if (text.isBlank() && threadDetails == null) Spacer(Modifier.height(0.dp))
    else PaneStickySharedElement(
        boundsTransform = sharedElementBoundsTransform,
        clipInOverlayDuringTransition = paneOverlayClip,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() },
        sharedContentState = rememberSharedContentState(
            key = remember(sharedElementPrefix, post.cid) {
                post.rememberTextSharedElementKey(
                    prefix = sharedElementPrefix,
                )
            },
        ),
    ) {
        val formattedText = rememberFormattedTextPost(
            text = text,
            textLinks = post.record?.links ?: emptyList(),
            onLinkTargetClicked = { onLinkTargetClicked(post, it) },
        )
        val threadPositionLabel = threadDetails?.let {
            stringResource(
                Res.string.post_thread_position,
                it.postIndex,
                it.postCount,
            )
        }
        MultilineStyledText(
            text = remember(formattedText, threadPositionLabel) {
                formattedText.withThreadPosition(
                    label = threadPositionLabel,
                )
            },
            maxLines = maxLines,
            inlineContent = threadDetails?.let {
                rememberThreadPositionInlineContent(
                    label = threadPositionLabel.orEmpty(),
                    contentDescription = stringResource(
                        Res.string.post_thread_position_description,
                        it.postIndex,
                        it.postCount,
                    ),
                )
            } ?: emptyMap(),
        )
    }
}

private fun AnnotatedString.withThreadPosition(
    label: String?,
): AnnotatedString =
    if (label == null) this
    else buildAnnotatedString {
        append(this@withThreadPosition)
        if (isNotEmpty()) append(' ')
        appendInlineContent(
            id = ThreadPositionInlineContentId,
            alternateText = label,
        )
    }

@Composable
private fun rememberThreadPositionInlineContent(
    label: String,
    contentDescription: String,
): Map<String, InlineTextContent> {
    val labelStyle = MaterialTheme.typography.labelSmall
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val placeholder = remember(label, labelStyle, density) {
        val labelSize = textMeasurer.measure(
            text = label,
            style = labelStyle,
            maxLines = 1,
        ).size
        with(density) {
            Placeholder(
                width = (labelSize.width + ThreadPositionHorizontalPadding.roundToPx() * 2).toSp(),
                height = (labelSize.height + ThreadPositionVerticalPadding.roundToPx() * 2).toSp(),
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            )
        }
    }
    return mapOf(
        ThreadPositionInlineContentId to InlineTextContent(
            placeholder = placeholder,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = CircleShape,
                    )
                    .semantics {
                        this.contentDescription = contentDescription
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = labelStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        },
    )
}

private fun Post.rememberTextSharedElementKey(
    prefix: String,
): String = "$prefix-${cid.id}-text"

private const val ThreadPositionInlineContentId = "threadPosition"
private val ThreadPositionHorizontalPadding = 6.dp
private val ThreadPositionVerticalPadding = 1.dp
