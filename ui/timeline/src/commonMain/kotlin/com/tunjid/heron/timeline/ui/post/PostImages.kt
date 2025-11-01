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
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Image
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.aspectRatioOrSquare
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.sensitiveContentBlur
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableStickySharedElementOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PostImages(
    modifier: Modifier = Modifier,
    feature: ImageList,
    postUri: PostUri,
    sharedElementPrefix: String,
    isBlurred: Boolean,
    matchHeightConstraintsFirst: Boolean,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    onImageClicked: (Int) -> Unit,
    presentation: Timeline.Presentation,
) {
    val shape = presentation.imageShape

    val itemModifier = if (isBlurred) Modifier.sensitiveContentBlur(shape)
    else Modifier

    LazyRow(
        modifier = modifier,
        horizontalArrangement = spacedBy(8.dp),
    ) {
        val tallestAspectRatio = feature.images.minOf { it.aspectRatioOrSquare }
        itemsIndexed(
            items = feature.images,
            key = { _, item -> item.thumb.uri },
            itemContent = { index, image ->
                paneMovableElementSharedTransitionScope.updatedMovableStickySharedElementOf(
                    modifier = when (presentation) {
                        Timeline.Presentation.Text.WithEmbed -> when (feature.images.size) {
                            1 ->
                                itemModifier
                                    .then(
                                        if (matchHeightConstraintsFirst) Modifier
                                        else Modifier.fillParentMaxWidth(),
                                    )
                                    .aspectRatio(
                                        ratio = image.aspectRatioOrSquare,
                                        matchHeightConstraintsFirst = matchHeightConstraintsFirst,
                                    )

                            else ->
                                itemModifier
                                    .height(200.dp)
                                    .aspectRatio(
                                        ratio = image.aspectRatioOrSquare,
                                    )
                        }

                        Timeline.Presentation.Media.Condensed,
                        Timeline.Presentation.Media.Expanded,
                        -> itemModifier
                            .fillParentMaxWidth()
                            .aspectRatio(tallestAspectRatio)
                        Timeline.Presentation.Media.Grid ->
                            itemModifier
                                .fillParentMaxWidth()
                                .aspectRatio(1f)
                    }
                        .clip(shape)
                        .clickable { onImageClicked(index) },
                    sharedContentState = with(paneMovableElementSharedTransitionScope) {
                        rememberSharedContentState(
                            key = image.sharedElementKey(
                                prefix = sharedElementPrefix,
                                postUri = postUri,
                            ),
                        )
                    },
                    state = remember(image.thumb.uri, presentation, shape) {
                        ImageArgs(
                            url = image.thumb.uri,
                            contentDescription = image.alt,
                            contentScale = when (presentation) {
                                Timeline.Presentation.Media.Expanded -> ContentScale.Crop
                                Timeline.Presentation.Media.Grid -> ContentScale.Crop
                                Timeline.Presentation.Media.Condensed -> ContentScale.Crop
                                Timeline.Presentation.Text.WithEmbed -> ContentScale.Fit
                            },
                            shape = shape,
                        )
                    },
                    sharedElement = { state, innerModifier ->
                        AsyncImage(
                            modifier = innerModifier,
                            args = state,
                        )
                    },
                )
            },
        )
    }
}

private val Timeline.Presentation.imageShape
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed -> TextWithEmbedShape
        Timeline.Presentation.Media.Condensed -> CondensedShape
        Timeline.Presentation.Media.Expanded -> ExpandedShape
        Timeline.Presentation.Media.Grid -> GridShape
    }

fun Image.sharedElementKey(
    prefix: String,
    postUri: PostUri,
) = "$prefix-$postUri-${thumb.uri}"

private val TextWithEmbedShape = RoundedPolygonShape.RoundedRectangle(percent = 0.05f)
private val CondensedShape = RoundedPolygonShape.RoundedRectangle(percent = 0.0001f)
private val ExpandedShape = RoundedPolygonShape.RoundedRectangle(percent = 0f)
private val GridShape = RoundedPolygonShape.RoundedRectangle(percent = 0f)
