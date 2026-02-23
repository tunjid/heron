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

package com.tunjid.heron.gallery.ui

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.gallery.Action
import com.tunjid.heron.gallery.GalleryItem
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.media.video.PlaybackStatus
import com.tunjid.heron.media.video.PlayerControlsUiState
import com.tunjid.heron.media.video.VideoPlayer
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.media.video.VideoPlayerState
import com.tunjid.heron.media.video.VideoStill
import com.tunjid.heron.media.video.rememberUpdatedVideoPlayerState
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.pathDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.timeline.ui.post.MediaPostInteractions
import com.tunjid.heron.timeline.ui.post.PostText
import com.tunjid.heron.timeline.ui.post.sharedElementKey
import com.tunjid.heron.timeline.ui.profile.ProfileWithViewerState
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.ui.isPrimaryOrActive
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.UpdatedMovableStickySharedElementOf
import heron.feature.gallery.generated.resources.Res
import heron.feature.gallery.generated.resources.mute_video
import heron.feature.gallery.generated.resources.unmute_video
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GalleryImage(
    modifier: Modifier = Modifier,
    scaffoldState: PaneScaffoldState,
    item: GalleryItem.Media.Photo,
    postUri: PostUri,
    sharedElementPrefix: String,
    isInViewport: (GalleryItem.Media) -> Boolean,
) {
    scaffoldState.UpdatedMovableStickySharedElementOf(
        modifier = modifier,
        sharedContentState = with(scaffoldState) {
            rememberSharedContentState(
                key = item.image.sharedElementKey(
                    prefix = sharedElementPrefix,
                    postUri = postUri,
                ),
                config = viewportSharedContentConfig(
                    item,
                    isInViewport,
                ),
            )
        },
        state = remember(item.image) {
            ImageArgs(
                url = item.image.fullsize.uri,
                thumbnailUrl = item.image.thumb.uri,
                contentDescription = item.image.alt,
                contentScale = ContentScale.Crop,
                shape = RoundedPolygonShape.Rectangle,
            )
        },
        sharedElement = { args, innerModifier ->
            AsyncImage(
                modifier = innerModifier,
                args = args,
            )
        },
    )
}

@Composable
internal fun GalleryVideo(
    modifier: Modifier = Modifier,
    paneMovableElementSharedTransitionScope: PaneScaffoldState,
    item: GalleryItem.Media.Video,
    postUri: PostUri,
    sharedElementPrefix: String,
    isInViewport: (GalleryItem.Media) -> Boolean,
) {
    val videoPlayerState = LocalVideoPlayerController.current.rememberUpdatedVideoPlayerState(
        videoUrl = item.video.playlist.uri,
        thumbnail = item.video.thumbnail?.uri,
        shape = RoundedPolygonShape.Rectangle,
    )
    if (!paneMovableElementSharedTransitionScope.isPrimaryOrActive) VideoStill(
        modifier = modifier,
        state = videoPlayerState,
    )
    else paneMovableElementSharedTransitionScope.UpdatedMovableStickySharedElementOf(
        modifier = modifier,
        sharedContentState = with(paneMovableElementSharedTransitionScope) {
            rememberSharedContentState(
                key = item.video.sharedElementKey(
                    prefix = sharedElementPrefix,
                    postUri = postUri,
                ),
                config = viewportSharedContentConfig(
                    item,
                    isInViewport,
                ),
            )
        },
        state = videoPlayerState,
        alternateOutgoingSharedElement = { state, innerModifier ->
            VideoStill(
                modifier = innerModifier,
                state = state,
            )
        },
        sharedElement = { state, innerModifier ->
            VideoPlayer(
                modifier = innerModifier,
                state = state,
            )
        },
    )
}

@Composable
internal fun GalleryFooter(
    modifier: Modifier,
    item: GalleryItem.Media,
    videoPlayerController: VideoPlayerController,
    imageDownloadState: ImageDownloadState,
    post: Post?,
    paneScaffoldState: PaneScaffoldState,
    actions: (Action) -> Unit,
    playerControlsUiState: PlayerControlsUiState,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = 16.dp),
        ) {
            when (item) {
                is GalleryItem.Media.Photo -> imageDownloadState.DownloadButton(item)
                is GalleryItem.Media.Video -> videoPlayerController.MuteButton()
            }
        }
        GalleryText(
            post = post,
            paneScaffoldState = paneScaffoldState,
            modifier = Modifier
                .padding(horizontal = 16.dp),
            onClick = {},
            onLinkTargetClicked = { _, target ->
                if (target is LinkTarget.Navigable) actions(
                    Action.Navigate.To(
                        pathDestination(
                            path = target.path,
                            referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                        ),
                    ),
                )
            },
        )

        (item as? GalleryItem.Media.Video)
            ?.let { videoPlayerController.getVideoStateById(it.video.playlist.uri) }
            ?.let {
                VideoPlayerControls(
                    videoPlayerState = it,
                    playerControlsUiState = playerControlsUiState,
                )
            }
    }
}

@Composable
internal fun MediaOverlay(
    modifier: Modifier = Modifier,
    media: GalleryItem.Media?,
    isVisible: Boolean,
    content: @Composable (item: GalleryItem.Media) -> Unit,
) {
    val visible by rememberUpdatedState(media != null && isVisible)
    val alphaState = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )
    val zIndex by remember {
        derivedStateOf {
            when {
                // When animating in, raise the z index of the overlay
                isVisible -> VisibleOverlayZIndex
                // Only when fully transparent is the zIndex dropped below the actual media
                alphaState.value == 0f -> InVisibleOverlayZIndex
                // Animating out, keep on top
                else -> VisibleOverlayZIndex
            }
        }
    }

    // The Overlay is stateful, so it should not be removed from the composition.
    // So instead of AnimatedVisibility, alpha and zIndices are  manipulated.
    Column(
        modifier = modifier
            .zIndex(zIndex)
            .fillMaxSize()
            .graphicsLayer { alpha = alphaState.value }
            .background(color = Color.Black.copy(alpha = 0.8f)),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
        content = {
            if (media != null) content(media)
        },
    )
}

@Composable
internal fun MediaPoster(
    post: Post?,
    signedInProfileId: ProfileId?,
    viewerState: ProfileViewerState?,
    sharedElementPrefix: String,
    paneScaffoldState: PaneScaffoldState,
    modifier: Modifier = Modifier,
    onProfileClicked: (Post) -> Unit,
    onViewerStateToggled: (ProfileViewerState?) -> Unit,
) {
    if (post == null) return

    ProfileWithViewerState(
        modifier = modifier,
        movableElementSharedTransitionScope = paneScaffoldState,
        signedInProfileId = signedInProfileId,
        profile = post.author,
        viewerState = viewerState,
        profileSharedElementKey = {
            post.avatarSharedElementKey(sharedElementPrefix)
        },
        onProfileClicked = {
            onProfileClicked(post)
        },
        onViewerStateClicked = onViewerStateToggled,
    )
}

@Composable
private fun GalleryText(
    post: Post?,
    paneScaffoldState: PaneScaffoldState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLinkTargetClicked: (Post, LinkTarget) -> Unit,
) {
    if (post == null) return

    PostText(
        modifier = modifier,
        post = post,
        sharedElementPrefix = UnmatchedPrefix,
        paneMovableElementSharedTransitionScope = paneScaffoldState,
        maxLines = 3,
        onClick = onClick,
        onLinkTargetClicked = onLinkTargetClicked,
    )
}

@Composable
internal fun MediaInteractions(
    post: Post?,
    showEngagementMetrics: Boolean,
    paneScaffoldState: PaneScaffoldState,
    modifier: Modifier = Modifier,
    onPostInteraction: (PostAction.Options) -> Unit,
) {
    if (post == null) return

    MediaPostInteractions(
        post = post,
        sharedElementPrefix = UnmatchedPrefix,
        showEngagementMetrics = showEngagementMetrics,
        paneMovableElementSharedTransitionScope = paneScaffoldState,
        modifier = modifier,
        onInteraction = onPostInteraction,
    )
}

@Composable
private fun viewportSharedContentConfig(
    media: GalleryItem.Media,
    isInViewport: (GalleryItem.Media) -> Boolean,
): SharedTransitionScope.SharedContentConfig {
    val updatedIsInViewport = rememberUpdatedState(isInViewport)
    return remember(media) {
        object : SharedTransitionScope.SharedContentConfig {
            override val SharedTransitionScope.SharedContentState.isEnabled: Boolean
                get() = updatedIsInViewport.value(media)
        }
    }
}

@Composable
private fun VideoPlayerControls(
    videoPlayerState: VideoPlayerState,
    playerControlsUiState: PlayerControlsUiState,
) {
    PlaybackStatus(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth(),
        videoPlayerState = videoPlayerState,
        controlsState = playerControlsUiState,
    )
    LaunchedEffect(Unit) {
        snapshotFlow { videoPlayerState.status }
            .collectLatest {
                playerControlsUiState.update(it)
            }
    }
}

@Composable
private fun VideoPlayerController.MuteButton(
    modifier: Modifier = Modifier,
) {
    IconButton(
        modifier = modifier,
        onClick = { isMuted = !isMuted },
    ) {
        Icon(
            imageVector =
            if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff
            else Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = stringResource(
                if (isMuted) Res.string.mute_video
                else Res.string.unmute_video,
            ),
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(40.dp),
        )
    }
}

private const val UnmatchedPrefix = "UnmatchedPrefix"
private const val VisibleOverlayZIndex = 1f
private const val InVisibleOverlayZIndex = -1f
