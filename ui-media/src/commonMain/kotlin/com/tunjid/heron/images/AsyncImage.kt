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

package com.tunjid.heron.images

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.tunjid.composables.ui.animate
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.shapes.animate
import coil3.compose.AsyncImage as CoilAsyncImage

data class ImageArgs(
    val url: String?,
    val thumbnailUrl: String? = null,
    val contentDescription: String? = null,
    val contentScale: ContentScale,
    val alignment: Alignment = Alignment.Center,
    val shape: RoundedPolygonShape,
)

@Composable
fun AsyncImage(
    args: ImageArgs,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(args.shape.animate())
    ) {
        var thumbnailVisible by remember(args.thumbnailUrl) {
            mutableStateOf(args.thumbnailUrl != null)
        }
        val contentScale = args.contentScale.animate()

        CoilAsyncImage(
            modifier = Modifier.matchParentSize(),
            model = args.url,
            contentDescription = args.contentDescription,
            contentScale = contentScale,
            onSuccess = { thumbnailVisible = false }
        )
        if (thumbnailVisible) CoilAsyncImage(
            modifier = Modifier.matchParentSize(),
            model = args.thumbnailUrl,
            contentDescription = args.contentDescription,
            contentScale = contentScale,
        )
    }
}