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

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.domain
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.post.feature.FeatureContainer
import com.tunjid.heron.timeline.ui.standard.Publication
import com.tunjid.heron.timeline.ui.withQuotingPostUriPrefix
import com.tunjid.heron.timeline.utilities.sensitiveContentBlur
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Renders an [ExternalEmbed] card (and any backing standard-site [externalRecord]) as a standalone,
 * non-interactive preview — e.g. in the composer before a post exists. The post identity needed by
 * [PostExternal] for shared-element keys is stubbed internally; it never matches a real post.
 */
@OptIn(ExperimentalUuidApi::class)
@Composable
fun ExternalEmbedPreview(
    embed: ExternalEmbed,
    externalRecord: Record.Embeddable.External?,
    paneTransitionScope: PaneTransitionScope,
    modifier: Modifier = Modifier,
) {
    val sharedElementPrefix = remember { Uuid.random().toString() }
    Box(modifier = modifier) {
        PostExternal(
            feature = embed,
            externalRecord = externalRecord,
            postUri = PreviewPostUri,
            sharedElementPrefix = sharedElementPrefix,
            presentation = Timeline.Presentation.Text.WithEmbed,
            isBlurred = false,
            paneTransitionScope = paneTransitionScope,
            onClick = {},
            onSubscriptionToggled = {},
        )
    }
}

@Composable
internal fun PostExternal(
    feature: ExternalEmbed,
    externalRecord: Record.Embeddable.External?,
    postUri: PostUri,
    sharedElementPrefix: String,
    presentation: Timeline.Presentation,
    isBlurred: Boolean,
    paneTransitionScope: PaneTransitionScope,
    onClick: () -> Unit,
    onSubscriptionToggled: (StandardPublication) -> Unit,
) = with(paneTransitionScope) {
    val isGif = feature.isGif()
    FeatureContainer(
        modifier = Modifier,
        onClick = onClick,
    ) {
        Column(
            verticalArrangement = spacedBy(8.dp),
        ) {
            val showContentPreview = externalRecord == null ||
                externalRecord is StandardDocument

            if (showContentPreview) ContentPreview(
                feature = feature,
                isBlurred = isBlurred,
                sharedElementPrefix = sharedElementPrefix,
                postUri = postUri,
                isGif = isGif,
                presentation = presentation,
            )
            val publication = when (externalRecord) {
                is StandardDocument -> externalRecord.publication
                is StandardPublication -> externalRecord
                null -> null
            }.takeIf {
                presentation == Timeline.Presentation.Text.WithEmbed
            }

            if (publication != null) {
                if (showContentPreview) HorizontalDivider()
                Publication(
                    modifier = Modifier
                        .padding(all = 12.dp),
                    paneTransitionScope = paneTransitionScope,
                    sharedElementPrefix = sharedElementPrefix.withQuotingPostUriPrefix(
                        quotingPostUri = postUri,
                    ),
                    publication = publication,
                    onSubscriptionToggled = { toggledPublication, _ ->
                        onSubscriptionToggled(toggledPublication)
                    },
                )
            }
        }
    }
}

@Composable
private fun PaneTransitionScope.ContentPreview(
    feature: ExternalEmbed,
    isBlurred: Boolean,
    sharedElementPrefix: String,
    postUri: PostUri,
    isGif: Boolean,
    presentation: Timeline.Presentation,
) {
    if (!feature.thumb?.uri.isNullOrBlank()) {
        val itemModifier = if (isBlurred) Modifier.sensitiveContentBlur(
            RoundedPolygonShape.Rectangle,
        )
        else Modifier
        PaneStickySharedElement(
            modifier = itemModifier
                .fillMaxWidth()
                .aspectRatio(2f / 1),
            sharedContentState = rememberSharedContentState(
                key = embedSharedElementKey(
                    prefix = sharedElementPrefix,
                    postUri = postUri,
                    text = feature.thumb?.uri,
                ),
            ),
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillParentAxisIfFixedOrWrap(),
                args = remember(isGif, feature.uri, feature.thumb) {
                    ImageArgs(
                        url = if (isGif) feature.uri.uri else feature.thumb?.uri,
                        contentDescription = feature.title,
                        contentScale = ContentScale.Crop,
                        shape = RoundedPolygonShape.Rectangle,
                    )
                },
            )
        }
    }
    if (presentation == Timeline.Presentation.Text.WithEmbed && !isGif) {
        PaneStickySharedElement(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                ),
            sharedContentState = rememberSharedContentState(
                key = embedSharedElementKey(
                    prefix = sharedElementPrefix,
                    postUri = postUri,
                    text = feature.title,
                ),
            ),
        ) {
            PostFeatureTextContent(
                modifier = Modifier
                    .fillParentAxisIfFixedOrWrap(),
                title = feature.title,
                description = null,
                uri = feature.uri,
            )
        }
    }
}

@Composable
fun PostFeatureTextContent(
    modifier: Modifier = Modifier,
    title: String?,
    description: String?,
    uri: GenericUri?,
) {
    Column(
        modifier = modifier,
        verticalArrangement = spacedBy(4.dp),
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = Bold),
            )
        }
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        val host = uri?.domain
        if (!host.isNullOrBlank()) {
            Text(
                text = host,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

private fun ExternalEmbed.isGif(): Boolean {
    val path = uri.uri.substringBefore('?')
    return path.endsWith(Gif_Format, ignoreCase = true)
}

private fun embedSharedElementKey(
    prefix: String,
    postUri: PostUri,
    text: String?,
): String = "$prefix-${postUri.uri}-$text"

private const val Gif_Format = ".gif"

// Placeholder identity for previews where no post exists yet; only feeds shared-element keys.
// Uses a structurally valid AT-URI (a real-shaped did:plc authority + TID rkey) so utilities that
// extract a ProfileId from it never crash on a malformed DID.
private val PreviewPostUri =
    PostUri("at://did:plc:heronpreviewplaceholderx/app.bsky.feed.post/apreviewheron")
