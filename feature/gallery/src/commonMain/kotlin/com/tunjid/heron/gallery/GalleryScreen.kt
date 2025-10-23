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

package com.tunjid.heron.gallery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tunjid.composables.gesturezoom.GestureZoomState.Companion.gestureZoomable
import com.tunjid.composables.gesturezoom.GestureZoomState.Options
import com.tunjid.composables.gesturezoom.rememberGestureZoomState
import com.tunjid.heron.data.core.models.AspectRatio
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.models.aspectRatioOrSquare
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.DownloadStatus
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.images.ImageRequest
import com.tunjid.heron.images.LocalImageLoader
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.ControlsVisibilityEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.media.video.PlaybackStatus
import com.tunjid.heron.media.video.PlayerControlsUiState
import com.tunjid.heron.media.video.VideoPlayer
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.media.video.VideoPlayerState
import com.tunjid.heron.media.video.VideoStill
import com.tunjid.heron.media.video.rememberUpdatedVideoPlayerState
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.composePostDestination
import com.tunjid.heron.scaffold.navigation.conversationDestination
import com.tunjid.heron.scaffold.navigation.pathDestination
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.navigation.signInDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.post.MediaPostInteractions
import com.tunjid.heron.timeline.ui.post.PostInteractionsSheetState.Companion.rememberUpdatedPostInteractionState
import com.tunjid.heron.timeline.ui.post.PostText
import com.tunjid.heron.timeline.ui.post.sharedElementKey
import com.tunjid.heron.timeline.ui.profile.ProfileWithViewerState
import com.tunjid.heron.ui.isPrimaryOrActive
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableStickySharedElementOf
import heron.feature.gallery.generated.resources.Res
import heron.feature.gallery.generated.resources.download
import heron.feature.gallery.generated.resources.download_complete
import heron.feature.gallery.generated.resources.download_failed
import heron.feature.gallery.generated.resources.mute_video
import heron.feature.gallery.generated.resources.unmute_video
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GalleryScreen(
    paneScaffoldState: PaneScaffoldState,
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit,
) {
    val videoPlayerController = LocalVideoPlayerController.current
    val playerControlsUiState = remember(videoPlayerController) {
        PlayerControlsUiState(videoPlayerController)
    }
    val imageDownloadState = remember(::ImageDownloadState)
    val postInteractionState = rememberUpdatedPostInteractionState(
        isSignedIn = paneScaffoldState.isSignedIn,
        onSignInClicked = {
            actions(Action.Navigate.To(signInDestination()))
        },
        onInteractionConfirmed = {
            actions(Action.SendPostInteraction(it))
        },
        onQuotePostClicked = { repost ->
            actions(
                Action.Navigate.To(
                    composePostDestination(
                        type = Post.Create.Quote(repost),
                        sharedElementPrefix = state.sharedElementPrefix,
                    ),
                ),
            )
        },
        recentConversations = state.recentConversations,
        onShareInConversationClicked = { conversation, postUri ->
            actions(
                Action.Navigate.To(
                    conversationDestination(
                        id = conversation.id,
                        members = conversation.members,
                        sharedElementPrefix = conversation.id.id,
                        sharedPostUri = postUri,
                        referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                    ),
                ),
            )
        },
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                onClick = playerControlsUiState::toggleVisibility,
            ),
    ) {
        val updatedItems by rememberUpdatedState(state.items)
        val pagerState = rememberPagerState(
            initialPage = state.startIndex,
        ) {
            updatedItems.size
        }

        HorizontalPager(
            modifier = Modifier
                .zIndex(MediaZIndex)
                .fillMaxSize(),
            state = pagerState,
            key = { page -> updatedItems[page].key },
            pageContent = { page ->
                var windowSize by remember { mutableStateOf(IntSize.Zero) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged {
                            windowSize = it
                        },
                ) {
                    when (val item = updatedItems[page]) {
                        is GalleryItem.Photo -> {
                            val zoomState = rememberGestureZoomState(
                                options = remember {
                                    Options(
                                        scale = Options.Scale.Layout,
                                        offset = Options.Offset.Layout,
                                    )
                                },
                            )
                            val coroutineScope = rememberCoroutineScope()
                            GalleryImage(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .aspectRatioFor(
                                        windowSize = windowSize,
                                        aspectRatio = item.image,
                                    )
                                    .gestureZoomable(zoomState)
                                    .combinedClickable(
                                        onClick = playerControlsUiState::toggleVisibility,
                                        onDoubleClick = {
                                            coroutineScope.launch {
                                                zoomState.toggleZoom()
                                            }
                                        },
                                    ),
                                scaffoldState = paneScaffoldState,
                                item = item,
                                sharedElementPrefix = state.sharedElementPrefix,
                                postUri = state.postUri,
                            )
                        }

                        is GalleryItem.Video -> GalleryVideo(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .aspectRatioFor(
                                    windowSize = windowSize,
                                    aspectRatio = item.video,
                                ),
                            paneMovableElementSharedTransitionScope = paneScaffoldState,
                            item = item,
                            sharedElementPrefix = state.sharedElementPrefix,
                            postUri = state.postUri,
                        )
                    }
                }
            },
        )

        MediaOverlay(
            modifier = Modifier
                .fillMaxSize(),
            galleryItem = updatedItems.getOrNull(pagerState.currentPage),
            isVisible = playerControlsUiState.playerControlsVisible,
        ) { item ->
            val viewedProfileId = state.viewedProfileId
            val signedInProfileId = state.signedInProfileId
            MediaPoster(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 20.dp,
                    ),
                post = state.post,
                signedInProfileId = state.signedInProfileId,
                viewerState = state.viewerState,
                sharedElementPrefix = state.posterSharedElementPrefix,
                paneScaffoldState = paneScaffoldState,
                onProfileClicked = { post ->
                    actions(
                        Action.Navigate.To(
                            profileDestination(
                                profile = post.author,
                                avatarSharedElementKey = post.avatarSharedElementKey(
                                    prefix = state.posterSharedElementPrefix,
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

            MediaInteractions(
                post = state.post,
                paneScaffoldState = paneScaffoldState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(horizontal = 16.dp),
                onReplyToPost = { post ->
                    actions(
                        Action.Navigate.To(
                            if (paneScaffoldState.isSignedOut) signInDestination()
                            else composePostDestination(
                                type = Post.Create.Reply(
                                    parent = post,
                                ),
                                sharedElementPrefix = state.sharedElementPrefix,
                            ),
                        ),
                    )
                },
                onPostInteraction = postInteractionState::onInteraction,
            )

            GalleryFooter(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .windowInsetsPadding(insets = WindowInsets.navigationBars),
                item = item,
                videoPlayerController = videoPlayerController,
                imageDownloadState = imageDownloadState,
                post = state.post,
                paneScaffoldState = paneScaffoldState,
                actions = actions,
                playerControlsUiState = playerControlsUiState,
            )
        }

        pagerState.interpolatedVisibleIndexEffect(
            denominator = 10,
            itemsAvailable = updatedItems.size,
            onIndex = { index ->
                when (val media = updatedItems.getOrNull(index.roundToInt())) {
                    null -> Unit
                    is GalleryItem.Photo -> Unit
                    is GalleryItem.Video -> videoPlayerController.play(
                        media.video.playlist.uri,
                    )
                }
            },
        )

        playerControlsUiState.ControlsVisibilityEffect()
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun GalleryImage(
    modifier: Modifier = Modifier,
    scaffoldState: PaneScaffoldState,
    item: GalleryItem.Photo,
    postUri: PostUri,
    sharedElementPrefix: String,
) {
    scaffoldState.updatedMovableStickySharedElementOf(
        modifier = modifier,
        sharedContentState = with(scaffoldState) {
            rememberSharedContentState(
                key = item.image.sharedElementKey(
                    prefix = sharedElementPrefix,
                    postUri = postUri,
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun GalleryVideo(
    modifier: Modifier = Modifier,
    paneMovableElementSharedTransitionScope: PaneScaffoldState,
    item: GalleryItem.Video,
    postUri: PostUri,
    sharedElementPrefix: String,
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
    else paneMovableElementSharedTransitionScope.updatedMovableStickySharedElementOf(
        modifier = modifier,
        sharedContentState = with(paneMovableElementSharedTransitionScope) {
            rememberSharedContentState(
                key = item.video.sharedElementKey(
                    prefix = sharedElementPrefix,
                    postUri = postUri,
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
private fun GalleryFooter(
    modifier: Modifier,
    item: GalleryItem,
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
                .padding(horizontal = 12.dp),
        ) {
            when (item) {
                is GalleryItem.Photo -> imageDownloadState.DownloadButton(item)
                is GalleryItem.Video -> videoPlayerController.MuteButton()
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

        (item as? GalleryItem.Video)
            ?.let { videoPlayerController.getVideoStateById(it.video.playlist.uri) }
            ?.let {
                VideoPlayerControls(
                    videoPlayerState = it,
                    playerControlsUiState = playerControlsUiState,
                )
            }
    }
}

private fun Modifier.aspectRatioFor(
    windowSize: IntSize,
    aspectRatio: AspectRatio,
): Modifier {
    val screenAspectRatio = windowSize.width.toFloat() / windowSize.height.toFloat()
    val isWiderAspectRatioThanMedia = screenAspectRatio > aspectRatio.aspectRatioOrSquare
    return this
        .fillMaxSize()
        .aspectRatio(
            ratio = aspectRatio.aspectRatioOrSquare,
            matchHeightConstraintsFirst = isWiderAspectRatioThanMedia,
        )
}

@Composable
private fun MediaOverlay(
    modifier: Modifier = Modifier,
    galleryItem: GalleryItem?,
    isVisible: Boolean,
    content: @Composable BoxScope.(item: GalleryItem) -> Unit,
) {
    val visible by rememberUpdatedState(galleryItem != null && isVisible)
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
            if (galleryItem != null) content(galleryItem)
        },
    )
}

@Composable
private fun MediaPoster(
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
fun GalleryText(
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
private fun MediaInteractions(
    post: Post?,
    paneScaffoldState: PaneScaffoldState,
    modifier: Modifier = Modifier,
    onReplyToPost: (Post) -> Unit,
    onPostInteraction: (Post.Interaction) -> Unit,
) {
    if (post == null) return

    MediaPostInteractions(
        post = post,
        sharedElementPrefix = UnmatchedPrefix,
        paneMovableElementSharedTransitionScope = paneScaffoldState,
        modifier = modifier,
        onReplyToPost = {
            onReplyToPost(post)
        },
        onPostInteraction = onPostInteraction,
    )
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

@Composable
private fun ImageDownloadState.DownloadButton(
    item: GalleryItem.Photo,
    modifier: Modifier = Modifier,
) {
    val imageLoader = LocalImageLoader.current
    val coroutineScope = rememberCoroutineScope()
    val downloadStatusState = stateFor(item)

    Box(
        modifier = modifier,
    ) {
        val contentModifier = Modifier
            .align(Alignment.Center)
            .size(40.dp)

        val onDownloadClicked: () -> Unit = remember(item.image.fullsize) {
            {
                coroutineScope.launch {
                    imageLoader.download(ImageRequest.Network(item.image.fullsize.uri))
                        .collectLatest { updateStateFor(item, it) }
                }
            }
        }

        AnimatedContent(
            targetState = downloadStatusState,
            contentKey = DownloadStatus?::contentKey,
        ) { status ->
            when (status) {
                DownloadStatus.Complete -> Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(Res.string.download_complete),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = contentModifier,
                )
                DownloadStatus.Failed -> IconButton(
                    modifier = contentModifier,
                    onClick = { updateStateFor(item, null) },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Error,
                        contentDescription = stringResource(Res.string.download_failed),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = contentModifier,
                    )
                }
                DownloadStatus.Indeterminate -> CircularProgressIndicator(
                    modifier = contentModifier,
                )
                is DownloadStatus.Progress -> CircularProgressIndicator(
                    modifier = contentModifier,
                    // let is needed bc of compose lint about method references
                    progress = animateFloatAsState(status.fraction).let { state ->
                        state::value
                    },
                )
                null -> IconButton(
                    modifier = contentModifier,
                    onClick = onDownloadClicked,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = stringResource(Res.string.download),
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = contentModifier,
                    )
                }
            }
        }
    }
}

private val DownloadStatus?.contentKey
    get() = when (this) {
        null -> "-"
        else -> this::class.simpleName
    }

private class ImageDownloadState {
    private val states = mutableStateMapOf<String, DownloadStatus?>()

    fun stateFor(
        item: GalleryItem.Photo,
    ): DownloadStatus? = states[item.image.fullsize.uri]

    fun updateStateFor(
        item: GalleryItem.Photo,
        status: DownloadStatus?,
    ) {
        states[item.image.fullsize.uri] = status
    }
}

private const val UnmatchedPrefix = "UnmatchedPrefix"
private const val VisibleOverlayZIndex = 1f
private const val InVisibleOverlayZIndex = -1f
private const val MediaZIndex = 0f
