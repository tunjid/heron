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
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.sensitiveContentBlur
import com.tunjid.heron.ui.shapes.toRoundedPolygonShape
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableStickySharedElementOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PostImages(
    feature: ImageList,
    postUri: PostUri,
    sharedElementPrefix: String,
    isBlurred: Boolean,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    onImageClicked: (Int) -> Unit,
    presentation: Timeline.Presentation,
) {
    val shape = animateDpAsState(
        when (presentation) {
            Timeline.Presentation.Text.WithEmbed -> 16.dp
            Timeline.Presentation.Media.Condensed -> 8.dp
            Timeline.Presentation.Media.Expanded -> 0.dp
        },
    ).value
        .let(::RoundedCornerShape)
        .toRoundedPolygonShape()

    val itemModifier = if (isBlurred) {
        Modifier.sensitiveContentBlur(shape)
    } else {
        Modifier
    }

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
                paneMovableElementSharedTransitionScope.updatedMovableStickySharedElementOf(
                    modifier = when (presentation) {
                        Timeline.Presentation.Text.WithEmbed -> when (feature.images.size) {
                            1 ->
                                itemModifier
                                    .fillParentMaxWidth()
                                    .aspectRatio(image.aspectRatioOrSquare)

                            else ->
                                itemModifier
                                    .height(200.dp)
                                    .aspectRatio(image.aspectRatioOrSquare)
                        }

                        Timeline.Presentation.Media.Condensed,
                        Timeline.Presentation.Media.Expanded,
                        -> itemModifier
                            .fillParentMaxWidth()
                            .aspectRatio(tallestAspectRatio)
                    }
                        .clickable { onImageClicked(index) },
                    sharedContentState = with(paneMovableElementSharedTransitionScope) {
                        rememberSharedContentState(
                            key = image.sharedElementKey(
                                prefix = sharedElementPrefix,
                                postUri = postUri,
                            ),
                        )
                    },
                    state = ImageArgs(
                        url = image.thumb.uri,
                        contentDescription = image.alt,
                        contentScale =
                        if (presentation == Timeline.Presentation.Media.Expanded) {
                            ContentScale.Crop
                        } else {
                            ContentScale.Fit
                        },
                        shape = shape,
                    ),
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

fun Image.sharedElementKey(
    prefix: String,
    postUri: PostUri,
) = "$prefix-$postUri-${thumb.uri}"
