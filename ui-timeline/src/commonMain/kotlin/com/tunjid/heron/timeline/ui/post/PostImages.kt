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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Image
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.aspectRatioOrSquare
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.heron.ui.shapes.toRoundedPolygonShape
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PostImages(
    feature: ImageList,
    sharedElementPrefix: String,
    panedSharedElementScope: PanedSharedElementScope,
    onImageClicked: (Int) -> Unit,
    presentation: Timeline.Presentation,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = spacedBy(8.dp),
    ) {
        val tallestAspectRatio = feature.images.minOf { it.aspectRatioOrSquare }
        itemsIndexed(
            items = feature.images,
            key = { _, item -> item.thumb.uri },
            itemContent = { index, image ->
                panedSharedElementScope.updatedMovableSharedElementOf(
                    modifier = when (presentation) {
                        Timeline.Presentation.TextAndEmbed -> when (feature.images.size) {
                            1 -> Modifier
                                .fillParentMaxWidth()
                                .aspectRatio(image.aspectRatioOrSquare)

                            else -> Modifier
                                .height(200.dp)
                                .aspectRatio(image.aspectRatioOrSquare)
                        }
                        Timeline.Presentation.CondensedMedia,
                        Timeline.Presentation.ExpandedMedia -> Modifier
                            .fillParentMaxWidth()
                            .aspectRatio(tallestAspectRatio)
                    }
                        .clickable { onImageClicked(index) },
                    key = image.sharedElementKey(
                        prefix = sharedElementPrefix
                    ),
                    state = ImageArgs(
                        url = image.thumb.uri,
                        contentDescription = image.alt,
                        contentScale =
                            if (presentation == Timeline.Presentation.ExpandedMedia)
                                ContentScale.Crop else ContentScale.Fit,
                        shape = animateDpAsState(
                            when (presentation) {
                                Timeline.Presentation.TextAndEmbed -> 16.dp
                                Timeline.Presentation.CondensedMedia -> 8.dp
                                Timeline.Presentation.ExpandedMedia -> 0.dp
                            }
                        ).value.let(::RoundedCornerShape).toRoundedPolygonShape(),
                    ),
                    sharedElement = { state, innerModifier ->
                        AsyncImage(
                            modifier = innerModifier,
                            args = state,
                        )
                    }
                )
            }
        )
    }
}

fun Image.sharedElementKey(
    prefix: String,
) = "$prefix-${thumb.uri}"
