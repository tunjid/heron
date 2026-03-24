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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.tunjid.composables.gesturezoom.GestureZoomState.Companion.gestureZoomable
import com.tunjid.composables.gesturezoom.GestureZoomState.Options
import com.tunjid.composables.gesturezoom.rememberGestureZoomState
import com.tunjid.heron.data.core.models.AspectRatio
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.aspectRatioOrSquare
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.utilities.asGenericUri
import com.tunjid.heron.gallery.ui.Comments
import com.tunjid.heron.gallery.ui.CommentsState
import com.tunjid.heron.gallery.ui.GalleryImage
import com.tunjid.heron.gallery.ui.GalleryVideo
import com.tunjid.heron.gallery.ui.ImageDownloadState
import com.tunjid.heron.gallery.ui.MediaCreatorAndDescription
import com.tunjid.heron.gallery.ui.MediaInteractions
import com.tunjid.heron.gallery.ui.MediaOverlayBox
import com.tunjid.heron.gallery.ui.PagerStates
import com.tunjid.heron.gallery.ui.galleryHeightFraction
import com.tunjid.heron.gallery.ui.isNotCollapsed
import com.tunjid.heron.gallery.ui.rememberCommentsState
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.ControlsVisibilityEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.media.video.PlaybackStatus
import com.tunjid.heron.media.video.PlayerControlsUiState
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.composePostDestination
import com.tunjid.heron.scaffold.navigation.conversationDestination
import com.tunjid.heron.scaffold.navigation.signInDestination
import com.tunjid.heron.scaffold.scaffold.DragToPopState.Companion.dragToPop
import com.tunjid.heron.scaffold.scaffold.DragToPopState.Companion.rememberDragToPopState
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.ui.post.PostInteractionsSheetState
import com.tunjid.heron.timeline.ui.post.PostInteractionsSheetState.Companion.rememberUpdatedPostInteractionsSheetState
import com.tunjid.heron.timeline.ui.post.PostOption
import com.tunjid.heron.timeline.ui.post.PostOptionsSheetState
import com.tunjid.heron.timeline.ui.post.PostOptionsSheetState.Companion.rememberUpdatedPostOptionsSheetState
import com.tunjid.heron.timeline.ui.profile.ProfileRestrictionDialogState.Companion.rememberProfileRestrictionDialogState
import com.tunjid.heron.timeline.ui.sheets.MutedWordsSheetState.Companion.rememberUpdatedMutedWordsSheetState
import com.tunjid.heron.ui.Indicator
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.platformStatusBars
import com.tunjid.heron.ui.text.links
import com.tunjid.tiler.compose.PivotedTilingEffect
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
internal fun GalleryScreen(
    paneScaffoldState: PaneScaffoldState,
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit,
) {
    val postInteractionSheetState = rememberUpdatedPostInteractionsSheetState(
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
    val mutedWordsSheetState = rememberUpdatedMutedWordsSheetState(
        mutedWordPreferences = state.preferences.mutedWordPreferences,
        onSave = {
            actions(Action.UpdateMutedWord(it))
        },
        onShown = {},
    )
    val profileRestrictionDialogState = rememberProfileRestrictionDialogState(
        onProfileRestricted = { profileRestriction ->
            when (profileRestriction) {
                is PostOption.Moderation.BlockAccount ->
                    actions(
                        Action.BlockAccount(
                            signedInProfileId = profileRestriction.signedInProfileId,
                            profileId = profileRestriction.post.author.did,
                        ),
                    )

                is PostOption.Moderation.MuteAccount ->
                    actions(
                        Action.MuteAccount(
                            signedInProfileId = profileRestriction.signedInProfileId,
                            profileId = profileRestriction.post.author.did,
                        ),
                    )
            }
        },
    )
    val postOptionsSheetState = rememberUpdatedPostOptionsSheetState(
        signedInProfileId = state.signedInProfileId,
        recentConversations = state.recentConversations,
        onShown = { actions(Action.UpdateRecentConversations) },
        onOptionClicked = { option ->
            when (option) {
                is PostOption.ShareInConversation ->
                    actions(
                        Action.Navigate.To(
                            conversationDestination(
                                id = option.conversation.id,
                                members = option.conversation.members,
                                sharedElementPrefix = option.conversation.id.id,
                                sharedUri = option.post.uri.asGenericUri(),
                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                            ),
                        ),
                    )

                // TODO
                is PostOption.ThreadGate -> Unit

                is PostOption.Moderation.BlockAccount ->
                    profileRestrictionDialogState.show(option)

                is PostOption.Moderation.MuteAccount ->
                    profileRestrictionDialogState.show(option)

                is PostOption.Moderation.MuteWords -> mutedWordsSheetState.show()
                is PostOption.Delete -> actions(Action.DeleteRecord(option.postUri))
            }
        },
    )
    val updatedItems by rememberUpdatedState(state.items)
    val pagerState = rememberPagerState(pageCount = updatedItems::size)
    val horizontalPagerStates = remember { PagerStates<PostUri>() }
    val commentsState = rememberCommentsState()

    val dragToPopState = rememberDragToPopState(
        shouldDragToPop = remember(
            pagerState,
            horizontalPagerStates,
        ) {
            var lastHorizontalGestureId: Int = -1
            var overscrollCount = 0

            canPop@{ delta ->
                commentsState.collapse()

                // Already dragging, continue
                if (isDraggingToPop) return@canPop true

                val isVertical = delta.y.absoluteValue > delta.x.absoluteValue
                if (isVertical) return@canPop pagerState.isConstrainedBy(delta.y)

                // Vertical scroll already begun
                if (pagerState.currentPageOffsetFraction != 0f) return@canPop false

                val item = updatedItems.getOrNull(pagerState.currentPage)
                    ?: return@canPop true

                // No items to scroll horizontally
                if (item.media.size <= 1) return@canPop true

                val horizontalPagerState = horizontalPagerStates[item.post.uri]
                    ?: return@canPop true

                val hasDifferentPointerId = lastHorizontalGestureId != gestureId

                // Reset tracking on item change
                if (hasDifferentPointerId) {
                    lastHorizontalGestureId = gestureId
                }

                val isConstrained = horizontalPagerState.isConstrainedBy(delta.x)

                if (isConstrained && hasDifferentPointerId) overscrollCount++
                else if (!isConstrained && delta.x != 0f) overscrollCount = 0

                isConstrained && overscrollCount > 1
            }
        },
    )

    Box(
        modifier = modifier,
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier
                .dragToPop(dragToPopState)
                .fillMaxWidth()
                .fillMaxHeight(
                    fraction = commentsState.galleryHeightFraction,
                ),
            beyondViewportPageCount = PagerPrefetchCount,
            userScrollEnabled = state.canScrollVertically,
            key = { page ->
                updatedItems[page].post.uri.uri
            },
            pageContent = { page ->
                val item = updatedItems[page]

                HorizontalItems(
                    item = item,
                    paneScaffoldState = paneScaffoldState,
                    signedInProfileId = state.signedInProfileId,
                    showEngagementMetrics = state.preferences.local.showPostEngagementMetrics,
                    pagerStates = horizontalPagerStates,
                    commentsState = commentsState,
                    focusedItem = {
                        val page = pagerState.currentPage + pagerState.currentPageOffsetFraction
                        updatedItems.getOrNull(page.fastRoundToInt())
                    },
                    isDraggingToPop = dragToPopState::isDraggingToPop,
                    actions = actions,
                    postInteractionSheetState = postInteractionSheetState,
                    postOptionsSheetState = postOptionsSheetState,
                )
            },
        )

        val commentsPost = state.commentsPost
        Comments(
            state = commentsState,
            paneScaffoldState = paneScaffoldState,
            postOptionsSheetState = postOptionsSheetState,
            modifier = Modifier
                .fillMaxSize(),
            comments = state.comments,
            inputText = state.inputText,
            onTextChanged = { actions(Action.TextChanged(it)) },
            onSendReply = {
                commentsPost ?: return@Comments
                val signedInProfileId = state.signedInProfileId ?: return@Comments
                val inputText = state.inputText
                actions(
                    Action.SendReply(
                        authorId = signedInProfileId,
                        parent = commentsPost,
                        text = inputText.text,
                        links = inputText.annotatedString.links(),
                    ),
                )
            },
            actions = actions,
        )
    }

    state.timelineStateHolder?.let { timelineStateHolder ->
        val timelineState by timelineStateHolder.state.collectAsStateWithLifecycle()
        val updatedTiledItems by rememberUpdatedState(timelineState.tiledItems)
        pagerState.PivotedTilingEffect(
            items = updatedTiledItems,
            onQueryChanged = { query ->
                timelineStateHolder.accept(
                    TimelineState.Action.Tile(
                        tilingAction = TilingState.Action.LoadAround(
                            query = query ?: timelineState.tilingData.currentQuery,
                        ),
                    ),
                )
            },
        )
    }
    LaunchedEffect(Unit) {
        snapshotFlow {
            pagerState.isScrollInProgress
        }
            .collect { isScrolling ->
                if (isScrolling) commentsState.collapse()
            }
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = commentsState.isNotCollapsed,
        onBackCompleted = commentsState::collapse,
    )
}

@Composable
private fun HorizontalItems(
    modifier: Modifier = Modifier,
    item: GalleryItem,
    signedInProfileId: ProfileId?,
    showEngagementMetrics: Boolean,
    pagerStates: PagerStates<PostUri>,
    paneScaffoldState: PaneScaffoldState,
    commentsState: CommentsState,
    focusedItem: () -> GalleryItem?,
    isDraggingToPop: () -> Boolean,
    actions: (Action) -> Unit,
    postInteractionSheetState: PostInteractionsSheetState,
    postOptionsSheetState: PostOptionsSheetState,
) {
    val videoPlayerController = LocalVideoPlayerController.current
    val imageDownloadState = remember(::ImageDownloadState)
    val playerControlsUiState = remember(videoPlayerController) {
        PlayerControlsUiState(videoPlayerController)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                onClick = playerControlsUiState::toggleVisibility,
            ),
    ) {
        val pagerState = pagerStates.manage(item.post.uri) {
            rememberPagerState(
                initialPage = item.startIndex,
            ) {
                item.media.size
            }
        }

        val statusBarHeight = with(LocalDensity.current) {
            UiTokens.statusBarHeight.toPx()
        }
        HorizontalPager(
            modifier = Modifier
                .zIndex(MediaZIndex)
                .fillMaxSize(),
            beyondViewportPageCount = PagerPrefetchCount,
            state = pagerState,
            key = { page -> item.media[page].key },
            pageContent = { page ->
                var windowSize by remember { mutableStateOf(IntSize.Zero) }
                val isInViewport = remember(item, page) {
                    inViewport@{ media: GalleryItem.Media ->
                        val inVerticalViewport = item == focusedItem()
                        if (!inVerticalViewport) return@inViewport false
                        media == item.media.getOrNull(pagerState.currentPage)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged {
                            windowSize = it
                        },
                ) {
                    val itemModifier = Modifier
                        .align { size, space, _ ->
                            val heightDifference = space.height - size.height
                            val verticalOffset =
                                if (heightDifference < statusBarHeight) statusBarHeight.fastRoundToInt()
                                else heightDifference / 2
                            IntOffset(x = 0, y = verticalOffset)
                        }
                    when (val media = item.media[page]) {
                        is GalleryItem.Media.Photo -> {
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
                                modifier = itemModifier
                                    .aspectRatioFor(
                                        windowSize = windowSize,
                                        aspectRatio = media.image,
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
                                item = media,
                                sharedElementPrefix = item.sharedElementPrefix,
                                postUri = item.post.uri,
                                isInViewport = isInViewport,
                            )
                        }

                        is GalleryItem.Media.Video -> GalleryVideo(
                            modifier = itemModifier
                                .aspectRatioFor(
                                    windowSize = windowSize,
                                    aspectRatio = media.video,
                                ),
                            paneTransitionScope = paneScaffoldState,
                            item = media,
                            sharedElementPrefix = item.sharedElementPrefix,
                            postUri = item.post.uri,
                            isInViewport = isInViewport,
                        )
                    }
                }
            },
        )

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .navigationBarsPadding(),
            enter = IndicatorEnterAnimation,
            exit = IndicatorExitAnimation,
            visible = !isDraggingToPop(),
        ) {
            Indicator(
                pagerState = pagerState,
            )
        }

        MediaOverlayBox(
            modifier = Modifier
                .fillMaxSize(),
            media = item.media.getOrNull(pagerState.currentPage),
            isVisible = playerControlsUiState.playerControlsVisible,
        ) { media ->
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        horizontal = 8.dp,
                        vertical = 16.dp,
                    )
                    .windowInsetsPadding(insets = WindowInsets.navigationBars),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MediaCreatorAndDescription(
                        modifier = Modifier
                            .weight(1f),
                        signedInProfileId = signedInProfileId,
                        item = item,
                        post = item.post,
                        paneScaffoldState = paneScaffoldState,
                        actions = actions,
                    )
                    MediaInteractions(
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.platformStatusBars),
                        media = media,
                        imageDownloadState = imageDownloadState,
                        videoPlayerController = videoPlayerController,
                        actions = actions,
                        item = item,
                        paneScaffoldState = paneScaffoldState,
                        showEngagementMetrics = showEngagementMetrics,
                        postInteractionSheetState = postInteractionSheetState,
                        postOptionsSheetState = postOptionsSheetState,
                        commentsState = commentsState,
                    )
                }
                (item.media.firstOrNull() as? GalleryItem.Media.Video)
                    ?.let { videoPlayerController.getVideoStateById(it.video.playlist.uri) }
                    ?.let { videoPlayerState ->
                        PlaybackStatus(
                            modifier = Modifier
                                .fillMaxWidth(),
                            videoPlayerState = videoPlayerState,
                            controlsState = playerControlsUiState,
                        )
                        LaunchedEffect(Unit) {
                            snapshotFlow { videoPlayerState.status }
                                .collectLatest(playerControlsUiState::update)
                        }
                    }
            }
        }

        pagerState.interpolatedVisibleIndexEffect(
            denominator = 10,
            itemsAvailable = item.media.size,
            onIndex = { index ->
                videoPlayerController.playIfVideo(
                    media = item.media.getOrNull(index.roundToInt()),
                )
            },
        )

        LaunchedEffect(Unit) {
            snapshotFlow { focusedItem()?.post?.uri == item.post.uri }
                .collect { inFocus ->
                    if (!inFocus) return@collect
                    videoPlayerController.playIfVideo(
                        media = item.media.getOrNull(pagerState.currentPage),
                    )
                }
        }

        playerControlsUiState.ControlsVisibilityEffect()
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

private fun VideoPlayerController.playIfVideo(
    media: GalleryItem.Media?,
) {
    when (media) {
        null -> Unit
        is GalleryItem.Media.Photo -> Unit
        is GalleryItem.Media.Video -> play(
            media.video.playlist.uri,
        )
    }
}

private fun ScrollableState.isConstrainedBy(
    delta: Float,
): Boolean {
    val constrainedAtStart = !canScrollBackward && delta > 0
    val constrainedAtEnd = !canScrollForward && delta < 0

    return constrainedAtStart || constrainedAtEnd
}

private val IndicatorEnterAnimation = fadeIn()
private val IndicatorExitAnimation = fadeOut()

private const val MediaZIndex = 0f
private const val PagerPrefetchCount = 1
