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

package com.tunjid.heron.posts

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tunjid.composables.collapsingheader.CollapsingHeaderLayout
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.repository.PostQuotesQuery
import com.tunjid.heron.data.utilities.asGenericUri
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.posts.di.PostsRequest
import com.tunjid.heron.sheets.postoptions.PostOption
import com.tunjid.heron.sheets.profile.ProfileRestrictionDialogState.Companion.rememberProfileRestrictionDialogState
import com.tunjid.heron.sheets.rememberMutedWordsSheetState
import com.tunjid.heron.sheets.rememberPostInteractionsSheetState
import com.tunjid.heron.sheets.rememberPostOptionsSheetState
import com.tunjid.heron.sheets.rememberTimelineThreadGateSheetState
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.ui.DismissableRefreshIndicator
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.timeline.ui.PostActions
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.withQuotingPostUriPrefix
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.contentType
import com.tunjid.heron.timeline.utilities.onDominantVideoChange
import com.tunjid.heron.timeline.utilities.rememberTimelineDisplayState
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.modifiers.gridColumnCount
import com.tunjid.heron.ui.rememberPinnedCollapsingHeaderState
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.composePostDestination
import com.tunjid.heron.ui.scaffold.navigation.conversationDestination
import com.tunjid.heron.ui.scaffold.navigation.galleryDestination
import com.tunjid.heron.ui.scaffold.navigation.pathDestination
import com.tunjid.heron.ui.scaffold.navigation.profileDestination
import com.tunjid.heron.ui.scaffold.navigation.recordDestination
import com.tunjid.heron.ui.scaffold.navigation.signInDestination
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.bottomNavVisibleInset
import com.tunjid.heron.ui.scaffold.scaffold.headerBlur
import com.tunjid.heron.ui.scaffold.scaffold.paneClip
import com.tunjid.heron.ui.scaffold.scaffold.pinnedHeaderScrim
import com.tunjid.heron.ui.scaffold.scaffold.rememberPinnedHeaderState
import com.tunjid.heron.ui.scaffold.scaffold.rememberTopAppBarHeaderState
import com.tunjid.heron.ui.scaffold.scaffold.renderHeaderInTransitionOverlay
import com.tunjid.heron.ui.scaffold.scaffold.topAppBarOffset
import com.tunjid.heron.ui.statusAndToolbarHeight
import com.tunjid.heron.ui.tabIndex
import com.tunjid.mutator.compose.produceStateWithLifecycle
import com.tunjid.mutator.invoke
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.threepane.ThreePane
import heron.feature.posts.generated.resources.Res
import heron.feature.posts.generated.resources.latest
import heron.feature.posts.generated.resources.top
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PostsScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { state.stateHolders.size }
    val collapsingHeaderState = rememberPinnedCollapsingHeaderState(
        pinnedHeight = UiTokens.statusAndToolbarHeight + when (state.request) {
            is PostsRequest.Quotes -> UiTokens.tabsHeight
            PostsRequest.Saved -> 0.dp
        },
        initialExpandedHeight = 800.dp,
    )

    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = state.stateHolders.getOrNull(pagerState.currentPage)
        ?.isRefreshing == true

    PullToRefreshBox(
        modifier = modifier
            .fillMaxSize()
            .paneClip(),
        isRefreshing = isRefreshing,
        state = pullToRefreshState,
        onRefresh = {
            state.stateHolders[pagerState.currentPage].refresh()
        },
        indicator = {
            DismissableRefreshIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset {
                        IntOffset(x = 0, y = collapsingHeaderState.expandedHeight.roundToInt())
                    },
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onDismissRequest = {
                    // TODO: Dismiss
                    when (state.stateHolders[pagerState.currentPage]) {
                        is PostsScreenStateHolders.Bookmarks -> Unit
                        is PostsScreenStateHolders.QuotedPosts -> Unit
                    }
                },
            )
        },
    ) {
        CollapsingHeaderLayout(
            modifier = Modifier
                .fillMaxSize(),
            state = collapsingHeaderState.collapsingHeaderState,
            headerContent = {
                Column(
                    modifier = Modifier
                        .renderHeaderInTransitionOverlay(
                            paneScaffoldState = paneScaffoldState,
                            zIndexInOverlay = UiTokens.lowerThanAppBarSharedElementZIndex(),
                        )
                        .fillMaxWidth()
                        .pinnedHeaderScrim(
                            state = collapsingHeaderState,
                            scrimColor = MaterialTheme.colorScheme.surface,
                        )
                        .offset {
                            IntOffset(
                                x = 0,
                                y = -collapsingHeaderState.translation.roundToInt(),
                            )
                        },
                ) {
                    Spacer(
                        modifier = Modifier
                            .height(UiTokens.statusAndToolbarHeight),
                    )
                    PostsTabs(
                        pagerState = pagerState,
                        stateHolders = state.stateHolders,
                    )
                }
            },
            body = {
                HorizontalPager(
                    modifier = Modifier
                        .fillMaxSize()
                        .headerBlur(
                            state = paneScaffoldState.rememberPinnedHeaderState(
                                pinnedCollapsingHeaderState = collapsingHeaderState,
                            ),
                        ),
                    state = pagerState,
                    key = { page ->
                        state.stateHolders[page].key
                    },
                    pageContent = { page ->
                        PostsTimeline(
                            paneScaffoldState = paneScaffoldState,
                            preferences = state.preferences,
                            stateHolder = state.stateHolders[page],
                            actions = actions,
                        )
                    },
                )
            },
        )
    }
}

@Composable
private fun PostsTimeline(
    paneScaffoldState: PaneScaffoldState,
    stateHolder: PostsScreenStateHolders,
    preferences: Preferences,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = stateHolder.produceStateWithLifecycle()
    val gridState = rememberLazyStaggeredGridState()
    val density = LocalDensity.current
    val now by remember { mutableStateOf(Clock.System.now()) }
    val videoPlayerController = LocalVideoPlayerController.current
    val presentation = remember { Timeline.Presentation.Text.WithEmbed }
    val displayState = rememberTimelineDisplayState()
    val postInteractionSheetState = paneScaffoldState.rememberPostInteractionsSheetState(
        sharedElementPrefix = SharedElementPrefix,
    )

    val threadGateSheetState = paneScaffoldState.rememberTimelineThreadGateSheetState()
    val mutedWordsSheetState = paneScaffoldState.rememberMutedWordsSheetState()

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
    val postOptionsSheetState = paneScaffoldState.rememberPostOptionsSheetState(
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

                is PostOption.ThreadGate ->
                    state.tiledItems.firstOrNull { it.post.uri == option.postUri }
                        ?.let(threadGateSheetState::show)

                is PostOption.Moderation.BlockAccount ->
                    profileRestrictionDialogState.show(option)

                is PostOption.Moderation.MuteAccount ->
                    profileRestrictionDialogState.show(option)

                is PostOption.Moderation.MuteWords -> mutedWordsSheetState.show()
                is PostOption.Delete -> actions(Action.DeleteRecord(option.postUri))
            }
        },
    )
    LookaheadScope {
        LazyVerticalStaggeredGrid(
            modifier = modifier
                .padding(
                    horizontal = animateDpAsState(
                        displayState.horizontalPadding(presentation),
                    ).value,
                )
                .headerBlur(
                    state = paneScaffoldState.rememberTopAppBarHeaderState(
                        topAppBarHeight = UiTokens.statusAndToolbarHeight,
                    ),
                )
                .fillMaxSize()
                .onDominantVideoChange(
                    topLeftInset = {
                        IntOffset(
                            x = 0,
                            y = gridState.layoutInfo.beforeContentPadding,
                        ) + paneScaffoldState.topAppBarOffset()
                    },
                    bottomRightInset = {
                        paneScaffoldState.bottomNavVisibleInset()
                    },
                    isEnabled = {
                        paneScaffoldState.paneState.pane == ThreePane.Primary &&
                            preferences.local.autoPlayTimelineVideos
                    },
                    onIdChanged = { videoId ->
                        if (videoId != null) videoPlayerController.play(videoId = videoId)
                        else videoPlayerController.pauseActiveVideo()
                    },
                )
                .gridColumnCount(
                    density = density,
                ) { numColumns ->
                    if (numColumns > 0) stateHolder(
                        TilingState.Action.GridSize(numColumns = numColumns),
                    )
                },
            state = gridState,
            cacheWindow = displayState.cacheWindow,
            columns = StaggeredGridCells.Adaptive(displayState.cardSize(presentation)),
            verticalItemSpacing = displayState.verticalItemSpacing(presentation),
            contentPadding = UiTokens.bottomNavAndInsetPaddingValues(
                isCompact = paneScaffoldState.prefersCompactBottomNav,
            ),
            horizontalArrangement = Arrangement.spacedBy(
                displayState.horizontalItemSpacing(presentation),
            ),
            userScrollEnabled = !paneScaffoldState.isTransitionActive,
        ) {
            items(
                items = state.tiledItems,
                key = TimelineItem::id,
                contentType = TimelineItem::contentType,
                itemContent = { item ->
                    TimelineItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        paneTransitionScope = paneScaffoldState,
                        presentationLookaheadScope = this@LookaheadScope,
                        now = now,
                        item = item,
                        sharedElementPrefix = SharedElementPrefix,
                        showEngagementMetrics = preferences.local.showPostEngagementMetrics,
                        presentation = presentation,
                        postActions = remember {
                            PostActions { action ->
                                when (action) {
                                    is PostAction.OfLinkTarget -> {
                                        val linkTarget = action.linkTarget
                                        if (linkTarget is LinkTarget.Navigable) actions(
                                            Action.Navigate.To(
                                                pathDestination(
                                                    path = linkTarget.path,
                                                    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                ),
                                            ),
                                        )
                                    }

                                    is PostAction.OfPost -> {
                                        actions(
                                            Action.Navigate.To(
                                                recordDestination(
                                                    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                    sharedElementPrefix = SharedElementPrefix,
                                                    otherModels = listOfNotNull(action.warnedAppliedLabels),
                                                    record = action.post,
                                                ),
                                            ),
                                        )
                                    }

                                    is PostAction.OfProfile -> {
                                        actions(
                                            Action.Navigate.To(
                                                profileDestination(
                                                    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                    profile = action.profile,
                                                    avatarSharedElementKey = action.post
                                                        .avatarSharedElementKey(
                                                            prefix = SharedElementPrefix,
                                                            quotingPostUri = action.quotingPostUri,
                                                        )
                                                        .takeIf { action.post.author.did == action.profile.did },
                                                ),
                                            ),
                                        )
                                    }

                                    is PostAction.OfRecord -> {
                                        actions(
                                            Action.Navigate.To(
                                                recordDestination(
                                                    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                    sharedElementPrefix = SharedElementPrefix
                                                        .withQuotingPostUriPrefix(action.owningPostUri),
                                                    record = action.record,
                                                ),
                                            ),
                                        )
                                    }

                                    is PostAction.OfMedia -> {
                                        actions(
                                            Action.Navigate.To(
                                                galleryDestination(
                                                    post = action.post,
                                                    media = action.media,
                                                    startIndex = action.index,
                                                    sharedElementPrefix = SharedElementPrefix
                                                        .withQuotingPostUriPrefix(action.quotingPostUri),
                                                ),
                                            ),
                                        )
                                    }

                                    is PostAction.OfReply -> {
                                        actions(
                                            Action.Navigate.To(
                                                if (paneScaffoldState.isSignedOut) signInDestination()
                                                else composePostDestination(
                                                    type = Post.Create.Reply(
                                                        parent = action.post,
                                                    ),
                                                    sharedElementPrefix = SharedElementPrefix,
                                                ),
                                            ),
                                        )
                                    }

                                    is PostAction.OfInteraction -> {
                                        postInteractionSheetState.onInteraction(action)
                                    }

                                    is PostAction.OfMore -> {
                                        postOptionsSheetState.showOptions(action.post)
                                    }

                                    is PostAction.OfPublicationSubscription ->
                                        actions(Action.TogglePublicationSubscription(action.publication))

                                    else -> Unit
                                }
                            }
                        },
                    )
                },
            )
        }
    }

    gridState.PivotedTilingEffect(
        items = state.tiledItems,
        onQueryChanged = { query ->
            stateHolder(
                TilingState.Action.LoadAround(
                    query = query ?: state.tilingData.currentQuery,
                ),
            )
        },
    )
}

@Composable
private fun PostsTabs(
    pagerState: PagerState,
    stateHolders: List<PostsScreenStateHolders>,
) {
    val tabs = stateHolders.mapNotNull { holder ->
        when (holder) {
            is PostsScreenStateHolders.Bookmarks -> null
            is PostsScreenStateHolders.QuotedPosts -> Tab(
                title = when (holder.sort) {
                    PostQuotesQuery.Sort.Top -> stringResource(Res.string.top)
                    PostQuotesQuery.Sort.Latest -> stringResource(Res.string.latest)
                },
                hasUpdate = false,
            )
        }
    }
    if (tabs.isEmpty()) return
    val scope = rememberCoroutineScope()

    Tabs(
        modifier = Modifier
            .padding(
                horizontal = 8.dp,
            )
            .fillMaxWidth()
            .clip(CircleShape),
        tabsState = rememberTabsState(
            tabs = tabs,
            selectedTabIndex = pagerState::tabIndex,
            onTabSelected = { page ->
                scope.launch {
                    pagerState.animateScrollToPage(page)
                }
            },
            onTabReselected = { index ->
                stateHolders.getOrNull(index = index)
                    ?.refresh()
            },
        ),
    )
}

private const val SharedElementPrefix = "posts"
