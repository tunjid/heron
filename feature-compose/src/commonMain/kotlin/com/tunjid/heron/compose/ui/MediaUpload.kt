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

package com.tunjid.heron.compose.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp
import com.tunjid.heron.compose.MediaItem
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.images.rememberUpdatedImageState
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.shapes.toRoundedPolygonShape


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun MediaUploadItems(
    modifier: Modifier = Modifier,
    photos: List<MediaItem.Photo>,
    video: MediaItem.Video?,
    removeMediaItem: (MediaItem) -> Unit,
    onMediaItemUpdated: (MediaItem) -> Unit,
) = LookaheadScope {
    Box(modifier = modifier) {
        val itemSum = photos.size + if (video == null) 0 else 1
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(if (itemSum < 2) 0.6f else 1f),
            horizontalArrangement = spacedBy(8.dp),
        ) {
            photos.forEach {
                key(it.path) {
                    ImageUpload(
                        modifier = Modifier
                            .animateBounds(this@LookaheadScope)
                            .weight(1f)
                            .aspectRatio(
                                ratio = 1f,
                                matchHeightConstraintsFirst = true
                            ),
                        photo = it,
                        removeMediaItem = removeMediaItem,
                        onMediaItemUpdated = onMediaItemUpdated,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ImageUpload(
    modifier: Modifier = Modifier,
    photo: MediaItem.Photo,
    removeMediaItem: (MediaItem) -> Unit,
    onMediaItemUpdated: (MediaItem) -> Unit,
    ) {
    Box(
        modifier = modifier,
    ) {
        val state = rememberUpdatedImageState(
            args = ImageArgs(
                file = photo.file,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                shape = MediaUploadItemShape,
            )
        )
        AsyncImage(
            modifier = Modifier
                .matchParentSize(),
            state = state,
        )
        Icon(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape,
                )
                .size(32.dp)
                .clip(CircleShape)
                .clickable { removeMediaItem(photo) },
            imageVector = Icons.Rounded.DoNotDisturbOn,
            contentDescription = null,
        )

        LaunchedEffect(state) {
            snapshotFlow { state.imageSize }
                .collect{ size ->
                    onMediaItemUpdated(
                        photo.updateSize(size)
                    )
                }
        }
    }
}

private val MediaUploadItemShape = RoundedCornerShape(8.dp).toRoundedPolygonShape()
