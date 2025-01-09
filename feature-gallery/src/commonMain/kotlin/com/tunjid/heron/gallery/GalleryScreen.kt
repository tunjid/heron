/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.gallery

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.post.sharedElementKey
import com.tunjid.heron.ui.SharedElementScope
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf

@Composable
internal fun GalleryScreen(
    sharedElementScope: SharedElementScope,
    modifier: Modifier = Modifier,
    state: State,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        val updatedItems by rememberUpdatedState(state.items)
        val pagerState = rememberPagerState(
            initialPage = state.startIndex
        ) {
            updatedItems.size
        }

        HorizontalPager(
            modifier = Modifier,
            state = pagerState,
            key = { page -> updatedItems[page].key },
            pageContent = { page ->
                when (val item = updatedItems[page]) {
                    is GalleryItem.Photo -> {
                        GalleryImage(
                            modifier = Modifier
                                .fillMaxSize(),
                            sharedElementScope = sharedElementScope,
                            item = item,
                            state = state
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun GalleryImage(
    modifier: Modifier = Modifier,
    sharedElementScope: SharedElementScope,
    item: GalleryItem.Photo,
    state: State,
) {
    sharedElementScope.updatedMovableSharedElementOf(
        modifier = modifier,
        key = item.image.sharedElementKey(
            prefix = state.sharedElementPrefix
        ),
        state = ImageArgs(
            url = item.image.thumb.uri,
            contentDescription = item.image.alt,
            contentScale = ContentScale.Fit,
            shape = RoundedPolygonShape.Rectangle,
        ),
        sharedElement = { args, innerModifier ->
            AsyncImage(
                modifier = innerModifier,
                args = args,
            )
        }
    )
}

