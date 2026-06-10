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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Image
import com.tunjid.heron.data.core.models.MediaItem
import com.tunjid.heron.data.core.models.MediaList
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.models.aspectRatioOrSquare
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.platform.Platform
import com.tunjid.heron.data.platform.current
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.timeline.utilities.bucketedRatio
import com.tunjid.heron.timeline.utilities.sensitiveContentBlur
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.localOverlayClip
import com.tunjid.heron.ui.modifiers.TrackingOverlayClip
import com.tunjid.heron.ui.modifiers.ifNotNull
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.modifiers.shapedClickable
import com.tunjid.heron.ui.modifiers.trackOverlayClipBounds
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.UpdatedMovableStickySharedElementOf

@Composable
internal fun PostMedia(
    modifier: Modifier = Modifier,
    feature: MediaList,
    postUri: PostUri,
    sharedElementPrefix: String,
    isBlurred: Boolean,
    matchHeightConstraintsFirst: Boolean,
    paneTransitionScope: PaneTransitionScope,
    onMediaClicked: (Int) -> Unit,
    presentation: Timeline.Presentation,
) {
    val shape = presentation.imageShape
    val baseModifier = Modifier.ifTrue(isBlurred) {
        sensitiveContentBlur(shape)
    }

    val imagesSize = feature.media.size
    val overlayClip = remember(imagesSize) {
        if (imagesSize > 1 && Platform.current.isNativeCompose) TrackingOverlayClip() else null
    }

    LazyRow(
        modifier = modifier
            .ifNotNull(
                item = overlayClip,
                block = Modifier::trackOverlayClipBounds,
            ),
        horizontalArrangement = spacedBy(8.dp),
    ) {
        val tallestMedia = feature.media.minBy { it.aspectRatioOrSquare }

        itemsIndexed(
            items = feature.media,
            key = { _, item ->
                when (item) {
                    is Image -> item.thumb.uri
                    is Video -> item.playlist.uri
                }
            },
            itemContent = { index, item ->
                val itemModifier = baseModifier
                    .mediaPlacement(
                        presentation = presentation,
                        feature = feature,
                        matchHeightConstraintsFirst = matchHeightConstraintsFirst,
                        mediaItem = item,
                        tallestMediaItem = tallestMedia,
                    )
                when (item) {
                    is Image -> PostImage(
                        paneTransitionScope = paneTransitionScope,
                        modifier = itemModifier
                            .shapedClickable(shape) { onMediaClicked(index) },
                        overlayClip = overlayClip,
                        image = item,
                        sharedElementPrefix = sharedElementPrefix,
                        postUri = postUri,
                        presentation = presentation,
                    )
                    is Video -> with(LocalVideoPlayerController.current) {
                        PostVideo(
                            modifier = itemModifier
                                .clickable {
                                    play(videoId = item.playlist.uri)
                                    onMediaClicked(index)
                                },
                            video = item,
                            presentation = presentation,
                            isBlurred = isBlurred,
                            paneTransitionScope = paneTransitionScope,
                            sharedElementPrefix = sharedElementPrefix,
                            postUri = postUri,
                        )
                    }
                }
            },
        )
    }
}

context(lazyItemScope: LazyItemScope)
private fun Modifier.mediaPlacement(
    presentation: Timeline.Presentation,
    feature: MediaList,
    matchHeightConstraintsFirst: Boolean,
    mediaItem: MediaItem,
    tallestMediaItem: MediaItem,
): Modifier = with(lazyItemScope) {
    when (presentation) {
        Timeline.Presentation.Text.WithEmbed -> when (feature.media.size) {
            1 ->
                this@mediaPlacement
                    .then(
                        if (matchHeightConstraintsFirst) Modifier
                        else Modifier.fillParentMaxWidth(),
                    )
                    .aspectRatio(
                        ratio = mediaItem.aspectRatioOrSquare,
                        matchHeightConstraintsFirst = matchHeightConstraintsFirst,
                    )

            else ->
                this@mediaPlacement
                    .height(200.dp)
                    .aspectRatio(
                        ratio = mediaItem.aspectRatioOrSquare,
                    )
        }

        Timeline.Presentation.Media.Condensed ->
            this@mediaPlacement
                .fillParentMaxWidth()
                .aspectRatio(tallestMediaItem.bucketedRatio())
        Timeline.Presentation.Media.Expanded ->
            this@mediaPlacement
                .fillParentMaxWidth()
                .aspectRatio(tallestMediaItem.aspectRatioOrSquare)
        Timeline.Presentation.Media.Grid ->
            this@mediaPlacement
                .fillParentMaxWidth()
                .aspectRatio(1f)
    }
}

@Composable
internal fun PostImage(
    paneTransitionScope: PaneTransitionScope,
    modifier: Modifier,
    overlayClip: TrackingOverlayClip?,
    image: Image,
    sharedElementPrefix: String,
    postUri: PostUri,
    presentation: Timeline.Presentation,
) {
    paneTransitionScope.UpdatedMovableStickySharedElementOf(
        modifier = modifier,
        clipInOverlayDuringTransition = overlayClip ?: paneTransitionScope.localOverlayClip,
        sharedContentState = with(paneTransitionScope) {
            rememberSharedContentState(
                key = image.sharedElementKey(
                    prefix = sharedElementPrefix,
                    postUri = postUri,
                ),
            )
        },
        state = remember(
            image.thumb.uri,
            presentation,
            presentation.imageShape,
        ) {
            ImageArgs(
                url = image.thumb.uri,
                contentDescription = image.alt,
                contentScale = when (presentation) {
                    Timeline.Presentation.Media.Expanded -> ContentScale.Crop
                    Timeline.Presentation.Media.Grid -> ContentScale.Crop
                    Timeline.Presentation.Media.Condensed -> ContentScale.Crop
                    Timeline.Presentation.Text.WithEmbed -> ContentScale.Fit
                },
                shape = presentation.imageShape,
            )
        },
        sharedElement = { state, innerModifier ->
            AsyncImage(
                modifier = innerModifier,
                args = state,
            )
        },
    )
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
