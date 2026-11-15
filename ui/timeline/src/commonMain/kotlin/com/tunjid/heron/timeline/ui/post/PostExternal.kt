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
import androidx.compose.foundation.shape.PolygonShape
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
import com.tunjid.heron.data.core.models.Event
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.Gif
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.asEmbeddedGifOrNull
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.domain
import com.tunjid.heron.media.images.AsyncImage
import com.tunjid.heron.media.images.ImageArgs
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.timeline.ui.event.Event
import com.tunjid.heron.timeline.ui.post.feature.FeatureContainer
import com.tunjid.heron.timeline.ui.standard.Document
import com.tunjid.heron.timeline.ui.standard.Publication
import com.tunjid.heron.timeline.ui.withQuotingPostUriPrefix
import com.tunjid.heron.timeline.utilities.sensitiveContentBlur
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.shapes.Rectangle
import kotlin.uuid.Uuid

/**
 * Renders an [ExternalEmbed] card (and any backing standard-site [externalRecord]) as a standalone,
 * non-interactive preview — e.g. in the composer before a post exists. The post identity needed by
 * [PostExternal] for shared-element keys is stubbed internally; it never matches a real post.
 */
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
            onRecordInteraction = {},
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
    onRecordInteraction: (PostAction.RecordInteraction) -> Unit,
) = with(paneTransitionScope) {
    val gif = remember(feature) { feature.asEmbeddedGifOrNull() }
    FeatureContainer(
        modifier = Modifier,
        onClick = onClick,
    ) {
        Column(
            verticalArrangement = spacedBy(8.dp),
        ) {
            val isTextPresentation = presentation is Timeline.Presentation.Text
            val showContentPreview = externalRecord == null || !isTextPresentation

            if (showContentPreview) ContentPreview(
                feature = feature,
                isBlurred = isBlurred,
                sharedElementPrefix = sharedElementPrefix,
                postUri = postUri,
                gif = gif,
                presentation = presentation,
            )
            if (isTextPresentation) externalRecord?.onEachRecord { count, index, record ->
                if (count > 1 && index > 0) HorizontalDivider()
                when (record) {
                    is Event -> Event(
                        modifier = Modifier
                            .padding(all = 12.dp),
                        paneTransitionScope = paneTransitionScope,
                        sharedElementPrefix = sharedElementPrefix.withQuotingPostUriPrefix(
                            quotingPostUri = postUri,
                        ),
                        event = record,
                        isBlurred = isBlurred,
                        onRsvpClicked = { event, status ->
                            onRecordInteraction(
                                PostAction.OfRsvp(
                                    event = event,
                                    status = status,
                                ),
                            )
                        },
                    )
                    is StandardDocument -> Document(
                        modifier = Modifier
                            .padding(all = 12.dp),
                        paneTransitionScope = paneTransitionScope,
                        sharedElementPrefix = sharedElementPrefix.withQuotingPostUriPrefix(
                            quotingPostUri = postUri,
                        ),
                        document = record,
                        // The publication below already shows itself and offers subscribing.
                        onPublicationClicked = null,
                        onSubscriptionToggled = null,
                        onRecommendationToggled = { document ->
                            onRecordInteraction(
                                PostAction.OfDocumentRecommendation(
                                    document = document,
                                ),
                            )
                        },
                    )
                    is StandardPublication -> Publication(
                        modifier = Modifier
                            .padding(all = 12.dp),
                        paneTransitionScope = paneTransitionScope,
                        sharedElementPrefix = sharedElementPrefix.withQuotingPostUriPrefix(
                            quotingPostUri = postUri,
                        ),
                        publication = record,
                        onSubscriptionToggled = { toggledPublication, _ ->
                            onRecordInteraction(
                                PostAction.OfPublicationSubscription(
                                    publication = toggledPublication,
                                ),
                            )
                        },
                    )
                }
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
    gif: Gif.Embedded?,
    presentation: Timeline.Presentation,
) {
    if (gif != null || !feature.thumb?.uri.isNullOrBlank()) {
        val itemModifier = if (isBlurred) Modifier.sensitiveContentBlur(
            PolygonShape.Rectangle,
        )
        else Modifier
        PaneStickySharedElement(
            boundsTransform = sharedElementBoundsTransform,
            clipInOverlayDuringTransition = paneOverlayClip,
            modifier = itemModifier
                .fillMaxWidth()
                .aspectRatio(gif?.aspectRatio ?: ExternalEmbedAspectRatio),
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
                args = remember(gif, feature.thumb, feature.title) {
                    ImageArgs(
                        url = gif?.playbackUri?.uri ?: feature.thumb?.uri,
                        contentDescription = gif?.altText ?: feature.title,
                        contentScale = ContentScale.Crop,
                        shape = PolygonShape.Rectangle,
                    )
                },
            )
        }
    }
    if (presentation == Timeline.Presentation.Text.WithEmbed && gif == null) {
        PaneStickySharedElement(
            boundsTransform = sharedElementBoundsTransform,
            clipInOverlayDuringTransition = paneOverlayClip,
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
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = Bold),
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

private inline fun Record.Embeddable.External.onEachRecord(
    block: (size: Int, index: Int, record: Record.Embeddable.External) -> Unit,
) = when (this) {
    is Event -> {
        block(1, 0, this)
    }
    is StandardDocument -> {
        block(2, 0, this)
        publication?.let {
            block(2, 1, it)
        }
    }
    is StandardPublication -> {
        block(1, 0, this)
    }
}

private fun embedSharedElementKey(
    prefix: String,
    postUri: PostUri,
    text: String?,
): String = "$prefix-${postUri.uri}-$text"

private const val ExternalEmbedAspectRatio = 2f / 1

// Placeholder identity for previews where no post exists yet; only feeds shared-element keys.
// Uses a structurally valid AT-URI (a real-shaped did:plc authority + TID rkey) so utilities that
// extract a ProfileId from it never crash on a malformed DID.
private val PreviewPostUri =
    PostUri("at://did:plc:heronpreviewplaceholderx/app.bsky.feed.post/apreviewheron")
