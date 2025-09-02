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
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.domain
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.post.feature.FeatureContainer
import com.tunjid.heron.timeline.utilities.sensitiveContentBlur
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PostExternal(
    feature: ExternalEmbed,
    postUri: PostUri,
    sharedElementPrefix: String,
    presentation: Timeline.Presentation,
    isBlurred: Boolean,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    onClick: () -> Unit,
) = with(paneMovableElementSharedTransitionScope) {
    val isGif = feature.isGif()
    FeatureContainer(
        modifier = Modifier.paneStickySharedElement(
            sharedContentState = rememberSharedContentState(
                key = embedSharedElementKey(
                    prefix = sharedElementPrefix,
                    postUri = postUri,
                    text = feature.uri.uri,
                ),
            ),
        ),
        onClick = onClick,
    ) {
        Column(verticalArrangement = spacedBy(8.dp)) {
            if (!feature.thumb?.uri.isNullOrBlank()) {
                val itemModifier = if (isBlurred) Modifier.sensitiveContentBlur(
                    RoundedPolygonShape.Rectangle,
                )
                else Modifier
                AsyncImage(
                    modifier = itemModifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 1)
                        .paneStickySharedElement(
                            sharedContentState = rememberSharedContentState(
                                key = embedSharedElementKey(
                                    prefix = sharedElementPrefix,
                                    postUri = postUri,
                                    text = feature.thumb?.uri,
                                ),
                            ),
                        ),
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
            if (presentation == Timeline.Presentation.Text.WithEmbed && !isGif) {
                PostFeatureTextContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                        )
                        .paneStickySharedElement(
                            sharedContentState = rememberSharedContentState(
                                key = embedSharedElementKey(
                                    prefix = sharedElementPrefix,
                                    postUri = postUri,
                                    text = feature.title,
                                ),
                            ),
                        ),
                    title = feature.title,
                    description = null,
                    uri = feature.uri,
                )
            }
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
