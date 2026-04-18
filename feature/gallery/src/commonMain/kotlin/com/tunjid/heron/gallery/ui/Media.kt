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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import com.tunjid.heron.gallery.posterSharedElementPrefix
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.media.video.VideoPlayer
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.media.video.VideoStill
import com.tunjid.heron.media.video.rememberUpdatedVideoPlayerState
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.pathDestination
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.timeline.ui.post.MediaPostInteractions
import com.tunjid.heron.timeline.ui.post.PostInteractionsSheetState
import com.tunjid.heron.timeline.ui.post.PostOptionsSheetState
import com.tunjid.heron.timeline.ui.post.PostText
import com.tunjid.heron.timeline.ui.post.sharedElementKey
import com.tunjid.heron.timeline.ui.profile.ProfileWithViewerState
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.ui.ScrimmedContent
import com.tunjid.heron.ui.isPrimaryOrActive
import com.tunjid.heron.ui.modifiers.shapedClickable
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.treenav.compose.UpdatedMovableStickySharedElementOf
import heron.feature.gallery.generated.resources.Res
import heron.feature.gallery.generated.resources.mute_video
import heron.feature.gallery.generated.resources.unmute_video
import heron.ui.core.generated.resources.close
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
    paneTransitionScope: PaneScaffoldState,
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
    if (!paneTransitionScope.isPrimaryOrActive) VideoStill(
        modifier = modifier,
        state = videoPlayerState,
    )
    else paneTransitionScope.UpdatedMovableStickySharedElementOf(
        modifier = modifier,
        sharedContentState = with(paneTransitionScope) {
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
internal fun MediaActions(
    modifier: Modifier = Modifier,
    media: GalleryItem.Media,
    imageDownloadState: ImageDownloadState,
    videoPlayerController: VideoPlayerController,
    onCloseClicked: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier
                .size(OverlayIconSize)
                .shapedClickable(
                    shape = CircleShape,
                    onClick = onCloseClicked,
                ),
            imageVector = Icons.Rounded.Close,
            tint = MaterialTheme.colorScheme.outline,
            contentDescription = stringResource(CommonStrings.close),
        )
        when (media) {
            is GalleryItem.Media.Photo -> imageDownloadState.DownloadButton(media)
            is GalleryItem.Media.Video -> videoPlayerController.MuteButton()
        }
    }
}

@Composable
internal fun MediaCreatorAndDescription(
    modifier: Modifier,
    signedInProfileId: ProfileId?,
    item: GalleryItem,
    paneScaffoldState: PaneScaffoldState,
    actions: (Action) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val viewedProfileId = item.post.author.did
        MediaPoster(
            post = item.post,
            signedInProfileId = signedInProfileId,
            viewerState = item.viewerState,
            sharedElementPrefix = item.posterSharedElementPrefix,
            paneScaffoldState = paneScaffoldState,
            onProfileClicked = { post ->
                actions(
                    Action.Navigate.To(
                        profileDestination(
                            profile = post.author,
                            avatarSharedElementKey = post.avatarSharedElementKey(
                                prefix = item.posterSharedElementPrefix,
                            ),
                            referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                        ),
                    ),
                )
            },
            onViewerStateToggled = remember(signedInProfileId, viewedProfileId) {
                { viewerState ->
                    signedInProfileId?.let {
                        actions(
                            Action.ToggleViewerState(
                                signedInProfileId = it,
                                viewedProfileId = viewedProfileId,
                                following = viewerState?.following,
                                followedBy = viewerState?.followedBy,
                            ),
                        )
                    }
                }
            },
        )
        GalleryText(
            post = item.post,
            paneScaffoldState = paneScaffoldState,
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
    }
}

@Composable
internal fun MediaOverlayBox(
    modifier: Modifier = Modifier,
    media: GalleryItem.Media?,
    isVisible: Boolean,
    content: @Composable BoxScope.(item: GalleryItem.Media) -> Unit,
) {
    ScrimmedContent {
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
        Box(
            modifier = modifier
                .zIndex(zIndex)
                .fillMaxSize()
                .graphicsLayer { alpha = alphaState.value }
                .background(color = Color.Black.copy(alpha = 0.8f)),
            content = {
                if (media != null) content(media)
            },
        )
    }
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
        paneTransitionScope = paneScaffoldState,
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
        paneTransitionScope = paneScaffoldState,
        maxLines = 3,
        onClick = onClick,
        onLinkTargetClicked = onLinkTargetClicked,
    )
}

@Composable
internal fun MediaInteractions(
    modifier: Modifier = Modifier,
    media: GalleryItem.Media,
    imageDownloadState: ImageDownloadState,
    videoPlayerController: VideoPlayerController,
    actions: (Action) -> Unit,
    item: GalleryItem,
    paneScaffoldState: PaneScaffoldState,
    showEngagementMetrics: Boolean,
    postInteractionSheetState: PostInteractionsSheetState,
    postOptionsSheetState: PostOptionsSheetState,
    commentsState: CommentsState,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MediaActions(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp),
            media = media,
            imageDownloadState = imageDownloadState,
            videoPlayerController = videoPlayerController,
            onCloseClicked = {
                actions(Action.Navigate.Pop)
            },
        )
        MediaPostInteractions(
            post = item.post,
            iconSize = OverlayIconSize,
            sharedElementPrefix = UnmatchedPrefix,
            showEngagementMetrics = showEngagementMetrics,
            paneTransitionScope = paneScaffoldState,
            onInteraction = { interaction ->
                when (interaction) {
                    is PostAction.OfInteraction -> postInteractionSheetState.onInteraction(
                        interaction,
                    )
                    is PostAction.OfMetadata -> Unit
                    is PostAction.OfMore -> postOptionsSheetState.showOptions(
                        interaction.post,
                    )
                    is PostAction.OfReply -> {
                        commentsState.expand()
                        actions(
                            Action.LoadComments(
                                post = item.post,
                                order = null,
                            ),
                        )
                    }
                }
            },
        )
    }
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
private fun VideoPlayerController.MuteButton(
    modifier: Modifier = Modifier,
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
        modifier = modifier
            .size(OverlayIconSize)
            .shapedClickable(CircleShape) {
                isMuted = !isMuted
            },
    )
}

internal val OverlayIconSize = 36.dp

private const val UnmatchedPrefix = "UnmatchedPrefix"
private const val VisibleOverlayZIndex = 1f
private const val InVisibleOverlayZIndex = -1f
