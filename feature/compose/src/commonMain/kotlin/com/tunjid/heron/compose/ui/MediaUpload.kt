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
import androidx.compose.foundation.layout.BoxScope
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
import com.tunjid.heron.data.files.RestrictedFile
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.images.rememberUpdatedImageState
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.media.video.VideoPlayer
import com.tunjid.heron.media.video.rememberUpdatedVideoPlayerState
import com.tunjid.heron.ui.shapes.toRoundedPolygonShape
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun MediaUploadItems(
    modifier: Modifier = Modifier,
    photos: List<RestrictedFile.Media.Photo>,
    video: RestrictedFile.Media.Video?,
    removeMediaItem: (RestrictedFile.Media) -> Unit,
    onMediaItemUpdated: (RestrictedFile.Media) -> Unit,
) = LookaheadScope {
    Box(modifier = modifier) {
        val itemSum = photos.size + if (video == null) 0 else 1
        if (video != null) VideoUpload(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            video = video,
            removeMediaItem = removeMediaItem,
            onMediaItemUpdated = onMediaItemUpdated,
        )
        else Row(
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
                                matchHeightConstraintsFirst = true,
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

@Composable
private fun ImageUpload(
    modifier: Modifier = Modifier,
    photo: RestrictedFile.Media.Photo,
    removeMediaItem: (RestrictedFile.Media) -> Unit,
    onMediaItemUpdated: (RestrictedFile.Media) -> Unit,
) {
    MediaUpload(
        modifier = modifier,
        media = photo,
        removeMediaItem = removeMediaItem,
        content = {
            val state = rememberUpdatedImageState(
                args = ImageArgs(
                    item = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    shape = MediaUploadItemShape,
                ),
            )
            AsyncImage(
                modifier = Modifier
                    .matchParentSize(),
                state = state,
            )
            LaunchedEffect(state) {
                snapshotFlow { state.imageSize }
                    .collect { size ->
                        onMediaItemUpdated(
                            photo.withSize(
                                width = size.width,
                                height = size.height,
                            ),
                        )
                    }
            }
        },
    )
}

@Composable
private fun VideoUpload(
    modifier: Modifier = Modifier,
    video: RestrictedFile.Media.Video,
    removeMediaItem: (RestrictedFile.Media) -> Unit,
    onMediaItemUpdated: (RestrictedFile.Media) -> Unit,
) {
    MediaUpload(
        modifier = modifier,
        media = video,
        removeMediaItem = removeMediaItem,
        content = {
            video.path?.let { videoPath ->
                val videoPlayerController = LocalVideoPlayerController.current
                val videoPlayerState = videoPlayerController.rememberUpdatedVideoPlayerState(
                    videoUrl = videoPath,
                    thumbnail = null,
                    shape = MediaUploadItemShape,
                )
                VideoPlayer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    state = videoPlayerState,
                )
                LaunchedEffect(videoPath) {
                    videoPlayerController.play(videoPath)
                    snapshotFlow { videoPlayerState.hasRenderedFirstFrame }
                        .first(true::equals)
                    videoPlayerController.pauseActiveVideo()

                    snapshotFlow { videoPlayerState.videoSize }
                        .collect { size ->
                            onMediaItemUpdated(
                                video.withSize(
                                    width = size.width,
                                    height = size.height,
                                ),
                            )
                        }
                }
            }
        },
    )
}

@Composable
private fun MediaUpload(
    modifier: Modifier = Modifier,
    media: RestrictedFile.Media,
    removeMediaItem: (RestrictedFile.Media) -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier,
    ) {
        content()
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
                .clickable { removeMediaItem(media) },
            imageVector = Icons.Rounded.DoNotDisturbOn,
            contentDescription = null,
        )
    }
}

private val MediaUploadItemShape = RoundedCornerShape(8.dp).toRoundedPolygonShape()
