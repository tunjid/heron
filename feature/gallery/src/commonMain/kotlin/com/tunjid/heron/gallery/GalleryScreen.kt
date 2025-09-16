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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tunjid.composables.gesturezoom.GestureZoomState.Companion.gestureZoomable
import com.tunjid.composables.gesturezoom.GestureZoomState.Options
import com.tunjid.composables.gesturezoom.rememberGestureZoomState
import com.tunjid.heron.data.core.models.AspectRatio
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.aspectRatioOrSquare
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
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
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

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

        videoPlayerController.MediaOverlay(
            modifier = Modifier
                .fillMaxSize(),
            galleryItem = updatedItems.getOrNull(pagerState.currentPage),
            isVisible = playerControlsUiState.playerControlsVisible,
        ) { videoPlayerState ->
            MediaPoster(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 20.dp,
                    ),
                post = state.post,
                sharedElementPrefix = state.sharedElementPrefix,
                paneScaffoldState = paneScaffoldState,
                onProfileClicked = { post ->
                    actions(
                        Action.Navigate.To(
                            profileDestination(
                                profile = post.author,
                                avatarSharedElementKey = post.avatarSharedElementKey(
                                    prefix = state.sharedElementPrefix,
                                ),
                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                            ),
                        ),
                    )
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
                onDownloadClick = {
                },
                isMuted = videoPlayerController.isMuted,
                onMuteClick = {
                    videoPlayerController.isMuted = !videoPlayerController.isMuted
                },
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .windowInsetsPadding(insets = WindowInsets.navigationBars),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                VideoText(
                    post = state.post,
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
                if (videoPlayerState != null) PlaybackStatus(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth(),
                    videoPlayerState = videoPlayerState,
                    controlsState = playerControlsUiState,
                )
            }

            LaunchedEffect(Unit) {
                snapshotFlow { videoPlayerState?.status }
                    .filterNotNull()
                    .collectLatest {
                        playerControlsUiState.update(it)
                    }
            }
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
fun VideoPlayerController.MediaOverlay(
    modifier: Modifier = Modifier,
    galleryItem: GalleryItem?,
    isVisible: Boolean,
    content: @Composable BoxScope.(VideoPlayerState?) -> Unit,
) {
    when (galleryItem) {
        null -> Unit
        is GalleryItem.Photo -> {
            AnimatedVisibility(
                modifier = modifier,
                visible = isVisible,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color.Black.copy(alpha = 0.8f)),
                    content = {
                        content(null)
                    },
                )
            }
        }
        is GalleryItem.Video -> {
            val videoPlayerState = getVideoStateById(
                videoId = galleryItem.video.playlist.uri,
            )

            if (videoPlayerState != null) AnimatedVisibility(
                modifier = modifier,
                visible = isVisible,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color.Black.copy(alpha = 0.8f)),
                    content = {
                        content(videoPlayerState)
                    },
                )
            }
        }
    }
}

@Composable
fun MediaPoster(
    post: Post?,
    sharedElementPrefix: String,
    paneScaffoldState: PaneScaffoldState,
    modifier: Modifier = Modifier,
    onProfileClicked: (Post) -> Unit,
) {
    if (post == null) return

    ProfileWithViewerState(
        modifier = modifier,
        movableElementSharedTransitionScope = paneScaffoldState,
        signedInProfileId = null,
        profile = post.author,
        viewerState = null,
        profileSharedElementKey = {
            post.avatarSharedElementKey(sharedElementPrefix)
        },
        onProfileClicked = {
            onProfileClicked(post)
        },
        onViewerStateClicked = {
        },
    )
}

@Composable
fun VideoText(
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
fun MediaInteractions(
    post: Post?,
    paneScaffoldState: PaneScaffoldState,
    modifier: Modifier = Modifier,
    isMuted: Boolean,
    onReplyToPost: (Post) -> Unit,
    onPostInteraction: (Post.Interaction) -> Unit,
    onDownloadClick: () -> Unit,
    onMuteClick: () -> Unit
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
        onDownloadClick = onDownloadClick,
        isMuted = isMuted,
        onMuteClick = onMuteClick,
    )
}

private const val UnmatchedPrefix = "UnmatchedPrefix"
