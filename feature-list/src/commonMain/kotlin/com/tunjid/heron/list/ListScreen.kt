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

package com.tunjid.heron.list

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.composables.collapsingheader.CollapsingHeaderLayout
import com.tunjid.composables.collapsingheader.rememberCollapsingHeaderState
import com.tunjid.composables.lazy.pendingScrollOffsetState
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.composePostDestination
import com.tunjid.heron.scaffold.navigation.galleryDestination
import com.tunjid.heron.scaffold.navigation.pathDestination
import com.tunjid.heron.scaffold.navigation.postDestination
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.navigation.signInDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.paneClip
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.isRefreshing
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.state.TimelineStateHolder
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.effects.TimelineRefreshEffect
import com.tunjid.heron.timeline.ui.post.PostInteractionsBottomSheet
import com.tunjid.heron.timeline.ui.post.PostInteractionsSheetState.Companion.rememberUpdatedPostInteractionState
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.timeline.ui.postActions
import com.tunjid.heron.timeline.utilities.cardSize
import com.tunjid.heron.timeline.utilities.description
import com.tunjid.heron.timeline.utilities.pendingOffsetFor
import com.tunjid.heron.timeline.utilities.sharedElementPrefix
import com.tunjid.heron.timeline.utilities.timelineHorizontalPadding
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.UiTokens.bottomNavAndInsetPaddingValues
import com.tunjid.heron.ui.tabIndex
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.threepane.ThreePane
import heron.feature_list.generated.resources.Res
import heron.feature_list.generated.resources.people
import heron.feature_list.generated.resources.posts
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.stringResource
import kotlin.math.floor
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ListScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val updatedStateHolders by rememberUpdatedState(state.stateHolders)
    val pagerState = rememberPagerState { updatedStateHolders.size }
    val scope = rememberCoroutineScope()

    val collapsedHeight = with(density) {
        UiTokens.tabsHeight.toPx()
    }
    val collapsingHeaderState = rememberCollapsingHeaderState(
        collapsedHeight = collapsedHeight,
        initialExpandedHeight = with(density) { 800.dp.toPx() },
    )

    CollapsingHeaderLayout(
        modifier = modifier
            .fillMaxSize(),
        state = collapsingHeaderState,
        headerContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset {
                        IntOffset(
                            x = 0,
                            y = -collapsingHeaderState.translation.roundToInt()
                        )
                    }
            ) {
                Text(
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                    text = state.timelineState?.timeline?.description ?: ""
                )
                Tabs(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape),
                    tabsState = rememberTabsState(
                        tabs = listTabs(
                            hasUpdate = state.timelineState?.hasUpdates == true
                        ),
                        selectedTabIndex = pagerState::tabIndex,
                        onTabSelected = {
                            scope.launch {
                                pagerState.animateScrollToPage(it)
                            }
                        },
                        onTabReselected = { },
                    ),
                )
            }
        },
        body = {
            val pullToRefreshState = rememberPullToRefreshState()
            val isRefreshing by produceState(
                initialValue = false,
                key1 = pagerState.currentPage,
                key2 = updatedStateHolders.size,
            ) {
                updatedStateHolders.getOrNull(pagerState.currentPage)
                    ?.tilingState
                    ?.collect {
                        value = it.isRefreshing
                    }
            }
            PullToRefreshBox(
                modifier = Modifier
                    .fillMaxSize()
                    .paneClip(),
                isRefreshing = isRefreshing,
                state = pullToRefreshState,
                onRefresh = {
                    updatedStateHolders[pagerState.currentPage].refresh()
                },
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter),
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                    )
                },
            ) {
                HorizontalPager(
                    modifier = Modifier
                        .fillMaxSize(),
                    state = pagerState,
                    key = { page -> updatedStateHolders[page].key },
                    pageContent = { page ->
                        when (val stateHolder = updatedStateHolders[page]) {
                            is ListScreenStateHolders.Members -> ListMembers(
                                paneScaffoldState = paneScaffoldState,
                                membersStateHolder = stateHolder,
                                actions = actions,
                            )

                            is ListScreenStateHolders.Timeline -> ListTimeline(
                                paneScaffoldState = paneScaffoldState,
                                timelineStateHolder = stateHolder,
                                actions = actions,
                            )
                        }
                    }
                )
            }
        }
    )
}

@Composable
private fun listTabs(
    hasUpdate: Boolean
): List<Tab> = remember { mutableStateListOf<Tab>() }.apply {
    when {
        isEmpty() -> {
            add(
                Tab(
                    title = stringResource(Res.string.people),
                    hasUpdate = false,
                )
            )
            add(
                Tab(
                    title = stringResource(Res.string.posts),
                    hasUpdate = hasUpdate,
                )
            )
        }

        this[1].hasUpdate != hasUpdate -> this[1] = Tab(
            title = stringResource(Res.string.posts),
            hasUpdate = hasUpdate,
        )
    }
}

@Composable
private fun ListMembers(
    modifier: Modifier = Modifier,
    paneScaffoldState: PaneScaffoldState,
    membersStateHolder: MembersStateHolder,
    actions: (Action) -> Unit,
) {
    val state by membersStateHolder.state.collectAsStateWithLifecycle()
    val updatedMembers by rememberUpdatedState(state.tiledItems)
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxSize()
            .paneClip(),
        state = listState,
        contentPadding = bottomNavAndInsetPaddingValues(
            horizontal = 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(
            items = updatedMembers,
            key = { it.subject.did.id },
            itemContent = { item ->
                com.tunjid.heron.timeline.ui.profile.ProfileWithViewerState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    movableElementSharedTransitionScope = paneScaffoldState,
                    signedInProfileId = null,
                    profile = item.subject,
                    viewerState = item.viewerState,
                    profileSharedElementKey = Profile::listMemberAvatarSharedElementKey,
                    onProfileClicked = { profile ->
                        actions(
                            Action.Navigate.To(
                                profileDestination(
                                    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                    profile = profile,
                                    avatarSharedElementKey = item.subject.listMemberAvatarSharedElementKey()
                                )
                            )
                        )
                    },
                    onViewerStateClicked = { viewerState ->
                        state.signedInProfileId?.let {
                            actions(
                                Action.ToggleViewerState(
                                    signedInProfileId = it,
                                    viewedProfileId = item.subject.did,
                                    following = viewerState?.following,
                                    followedBy = viewerState?.followedBy,
                                )
                            )
                        }
                    },
                )
            }
        )
    }

    listState.PivotedTilingEffect(
        items = updatedMembers,
        onQueryChanged = { query ->
            membersStateHolder.accept(
                TilingState.Action.LoadAround(
                    query ?: state.tilingData.currentQuery
                )
            )
        }
    )
}

@Composable
private fun ListTimeline(
    paneScaffoldState: PaneScaffoldState,
    timelineStateHolder: TimelineStateHolder,
    actions: (Action) -> Unit,
) {
    val gridState = rememberLazyStaggeredGridState()
    val timelineState by timelineStateHolder.state.collectAsStateWithLifecycle()
    val items by rememberUpdatedState(timelineState.tiledItems)
    val pendingScrollOffsetState = gridState.pendingScrollOffsetState()

    val density = LocalDensity.current
    val videoStates = remember { ThreadedVideoPositionStates() }
    val presentation = timelineState.timeline.presentation
    val postInteractionState = rememberUpdatedPostInteractionState(
        isSignedIn = paneScaffoldState.isSignedIn,
    )
    LookaheadScope {
        LazyVerticalStaggeredGrid(
            modifier = Modifier
                .padding(
                    horizontal = animateDpAsState(
                        presentation.timelineHorizontalPadding
                    ).value
                )
                .fillMaxSize()
                .paneClip()
                .onSizeChanged {
                    val itemWidth = with(density) {
                        presentation.cardSize.toPx()
                    }
                    timelineStateHolder.accept(
                        TimelineState.Action.Tile(
                            tilingAction = TilingState.Action.GridSize(
                                floor(it.width / itemWidth).roundToInt()
                            )
                        )
                    )
                },
            state = gridState,
            columns = StaggeredGridCells.Adaptive(presentation.cardSize),
            verticalItemSpacing = 8.dp,
            contentPadding = bottomNavAndInsetPaddingValues(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = !paneScaffoldState.isTransitionActive,
        ) {
            items(
                items = items,
                key = TimelineItem::id,
                itemContent = { item ->
                    TimelineItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .threadedVideoPosition(
                                state = videoStates.getOrCreateStateFor(item)
                            ),
                        paneMovableElementSharedTransitionScope = paneScaffoldState,
                        presentationLookaheadScope = this@LookaheadScope,
                        now = remember { Clock.System.now() },
                        item = item,
                        sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
                        presentation = presentation,
                        labelers = timelineState.labelers,
                        contentPreferences = timelineState.labelPreferences,
                        postActions = remember(timelineState.timeline.sourceId) {
                            postActions(
                                onLinkTargetClicked = { post, linkTarget ->
                                    if (linkTarget is LinkTarget.OfProfile) actions(
                                        Action.Navigate.To(
                                            pathDestination(
                                                path = linkTarget.path,
                                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                            )
                                        )
                                    )
                                },
                                onPostClicked = { post: Post, quotingPostId: PostId? ->
                                    pendingScrollOffsetState.value =
                                        gridState.pendingOffsetFor(item)
                                    actions(
                                        Action.Navigate.To(
                                            postDestination(
                                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                sharedElementPrefix = timelineState.timeline.sharedElementPrefix(
                                                    quotingPostId = quotingPostId,
                                                ),
                                                post = post,
                                            )
                                        )
                                    )
                                },
                                onProfileClicked = { profile: Profile, post: Post, quotingPostId: PostId? ->
                                    pendingScrollOffsetState.value =
                                        gridState.pendingOffsetFor(item)
                                    actions(
                                        Action.Navigate.To(
                                            profileDestination(
                                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                profile = profile,
                                                avatarSharedElementKey = post
                                                    .avatarSharedElementKey(
                                                        prefix = timelineState.timeline.sourceId,
                                                        quotingPostId = quotingPostId,
                                                    )
                                                    .takeIf { post.author.did == profile.did }
                                            )
                                        )
                                    )
                                },
                                onPostMediaClicked = { media: Embed.Media, index: Int, post: Post, quotingPostId: PostId? ->
                                    pendingScrollOffsetState.value =
                                        gridState.pendingOffsetFor(item)
                                    actions(
                                        Action.Navigate.To(
                                            galleryDestination(
                                                post = post,
                                                media = media,
                                                startIndex = index,
                                                sharedElementPrefix = timelineState.timeline.sharedElementPrefix(
                                                    quotingPostId = quotingPostId,
                                                ),
                                            )
                                        )
                                    )
                                },
                                onReplyToPost = { post: Post ->
                                    pendingScrollOffsetState.value =
                                        gridState.pendingOffsetFor(item)
                                    actions(
                                        Action.Navigate.To(
                                            if (paneScaffoldState.isSignedOut) signInDestination()
                                            else composePostDestination(
                                                type = Post.Create.Reply(
                                                    parent = post,
                                                ),
                                                sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
                                            )
                                        )
                                    )
                                },
                                onPostInteraction = postInteractionState::onInteraction,
                            )
                        }
                    )
                }
            )
        }
    }

    PostInteractionsBottomSheet(
        state = postInteractionState,
        onSignInClicked = {
            actions(
                Action.Navigate.To(signInDestination())
            )
        },
        onInteractionConfirmed = {
            actions(
                Action.SendPostInteraction(it)
            )
        },
        onQuotePostClicked = { repost ->
            actions(
                Action.Navigate.To(
                    composePostDestination(
                        type = Post.Create.Quote(repost),
                        sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
                    )
                )
            )
        }
    )

    if (paneScaffoldState.paneState.pane == ThreePane.Primary) {
        val videoPlayerController = LocalVideoPlayerController.current
        gridState.interpolatedVisibleIndexEffect(
            denominator = 10,
            itemsAvailable = items.size,
        ) { interpolatedIndex ->
            val flooredIndex = floor(interpolatedIndex).toInt()
            val fraction = interpolatedIndex - flooredIndex
            items.getOrNull(flooredIndex)
                ?.let(videoStates::retrieveStateFor)
                ?.videoIdAt(fraction)
                ?.let(videoPlayerController::play)
                ?: videoPlayerController.pauseActiveVideo()
        }
    }

    gridState.PivotedTilingEffect(
        items = items,
        onQueryChanged = { query ->
            timelineStateHolder.accept(
                TimelineState.Action.Tile(
                    tilingAction = TilingState.Action.LoadAround(
                        query ?: timelineState.tilingData.currentQuery
                    )
                )
            )
        }
    )

    gridState.TimelineRefreshEffect(
        timelineState = timelineState,
        onRefresh = { animateScrollToItem(index = 0) }
    )
}

private fun Profile.listMemberAvatarSharedElementKey(): String =
    "list-member-${did.id}"