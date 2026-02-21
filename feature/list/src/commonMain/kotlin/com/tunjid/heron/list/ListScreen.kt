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
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.tunjid.composables.lazy.rememberLazyScrollableState
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.core.models.sourceId
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.utilities.asGenericUri
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.composePostDestination
import com.tunjid.heron.scaffold.navigation.conversationDestination
import com.tunjid.heron.scaffold.navigation.galleryDestination
import com.tunjid.heron.scaffold.navigation.pathDestination
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.navigation.recordDestination
import com.tunjid.heron.scaffold.navigation.signInDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.paneClip
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.isRefreshing
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.state.TimelineStateHolder
import com.tunjid.heron.timeline.ui.DismissableRefreshIndicator
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.timeline.ui.PostActions
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.effects.TimelineRefreshEffect
import com.tunjid.heron.timeline.ui.post.PostInteractionsSheetState.Companion.rememberUpdatedPostInteractionsSheetState
import com.tunjid.heron.timeline.ui.post.PostOption
import com.tunjid.heron.timeline.ui.post.PostOptionsSheetState.Companion.rememberUpdatedPostOptionsSheetState
import com.tunjid.heron.timeline.ui.post.ThreadGateSheetState.Companion.rememberUpdatedThreadGateSheetState
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.timeline.ui.profile.ProfileRestrictionDialogState.Companion.rememberProfileRestrictionDialogState
import com.tunjid.heron.timeline.ui.profile.ProfileWithViewerState
import com.tunjid.heron.timeline.ui.sheets.MutedWordsSheetState.Companion.rememberUpdatedMutedWordsSheetState
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.canAutoPlayVideo
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
import heron.feature.list.generated.resources.Res
import heron.feature.list.generated.resources.people
import heron.feature.list.generated.resources.posts
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

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
        modifier = modifier
            .fillMaxSize()
            .paneClip(),
        isRefreshing = isRefreshing,
        state = pullToRefreshState,
        onRefresh = {
            updatedStateHolders[pagerState.currentPage].refresh()
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
                    when (val holder = updatedStateHolders[pagerState.currentPage]) {
                        is ListScreenStateHolders.Members -> Unit
                        is ListScreenStateHolders.Timeline -> holder.accept(
                            TimelineState.Action.DismissRefresh,
                        )
                    }
                },
            )
        },
    ) {
        CollapsingHeaderLayout(
            modifier = Modifier
                .fillMaxSize(),
            state = collapsingHeaderState,
            headerContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset {
                            IntOffset(
                                x = 0,
                                y = -collapsingHeaderState.translation.roundToInt(),
                            )
                        },
                ) {
                    Text(
                        modifier = Modifier
                            .padding(
                                horizontal = 16.dp,
                                vertical = 8.dp,
                            ),
                        text = state.timelineState?.timeline?.description ?: "",
                    )
                    Tabs(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape),
                        tabsState = rememberTabsState(
                            tabs = listTabs(
                                hasUpdate = state.timelineState?.hasUpdates == true,
                            ),
                            selectedTabIndex = pagerState::tabIndex,
                            onTabSelected = { page ->
                                scope.launch {
                                    pagerState.animateScrollToPage(page)
                                }
                            },
                            onTabReselected = { index ->
                                updatedStateHolders.getOrNull(index = index)
                                    ?.refresh()
                            },
                        ),
                    )
                }
            },
            body = {
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
                                signedInProfileId = state.signedInProfileId,
                                paneScaffoldState = paneScaffoldState,
                                timelineStateHolder = stateHolder,
                                actions = actions,
                                recentLists = state.recentLists,
                                recentConversations = state.recentConversations,
                                mutedWordsPreferences = state.preferences.mutedWordPreferences,
                                autoPlayTimelineVideos = state.preferences.local.autoPlayTimelineVideos,
                            )
                        }
                    },
                )
            },
        )
    }
}

@Composable
private fun listTabs(
    hasUpdate: Boolean,
): List<Tab> = remember { mutableStateListOf<Tab>() }.apply {
    when {
        isEmpty() -> {
            add(
                Tab(
                    title = stringResource(Res.string.people),
                    hasUpdate = false,
                ),
            )
            add(
                Tab(
                    title = stringResource(Res.string.posts),
                    hasUpdate = hasUpdate,
                ),
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
            isCompact = paneScaffoldState.prefersCompactBottomNav,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(
            items = updatedMembers,
            key = { it.subject.did.id },
            itemContent = { item ->
                ProfileWithViewerState(
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
                                    avatarSharedElementKey = item.subject.listMemberAvatarSharedElementKey(),
                                ),
                            ),
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
                                ),
                            )
                        }
                    },
                )
            },
        )
    }

    listState.PivotedTilingEffect(
        items = updatedMembers,
        onQueryChanged = { query ->
            membersStateHolder.accept(
                TilingState.Action.LoadAround(
                    query ?: state.tilingData.currentQuery,
                ),
            )
        },
    )
}

@Composable
private fun ListTimeline(
    signedInProfileId: ProfileId?,
    paneScaffoldState: PaneScaffoldState,
    timelineStateHolder: TimelineStateHolder,
    actions: (Action) -> Unit,
    recentLists: List<FeedList>,
    recentConversations: List<Conversation>,
    mutedWordsPreferences: List<MutedWordPreference>,
    autoPlayTimelineVideos: Boolean,
) {
    var pendingScrollOffset by rememberSaveable { mutableIntStateOf(0) }
    val gridState = rememberLazyScrollableState(
        init = ::LazyStaggeredGridState,
        firstVisibleItemIndex = LazyStaggeredGridState::firstVisibleItemIndex,
        firstVisibleItemScrollOffset = LazyStaggeredGridState::firstVisibleItemScrollOffset,
        restore = { firstVisibleItemIndex, firstVisibleItemScrollOffset ->
            LazyStaggeredGridState(
                initialFirstVisibleItemIndex = firstVisibleItemIndex,
                initialFirstVisibleItemOffset = firstVisibleItemScrollOffset + pendingScrollOffset,
            )
        },
    )
    val timelineState by timelineStateHolder.state.collectAsStateWithLifecycle()
    val items by rememberUpdatedState(timelineState.tiledItems)

    val density = LocalDensity.current
    val videoStates = remember { ThreadedVideoPositionStates(TimelineItem::id) }
    val presentation = timelineState.timeline.presentation
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
                        sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
                    ),
                ),
            )
        },
    )
    val threadGateSheetState = rememberUpdatedThreadGateSheetState(
        recentLists = recentLists,
        onRequestRecentLists = {
            actions(Action.UpdateRecentLists)
        },
        onThreadGateUpdated = {
            actions(Action.SendPostInteraction(it))
        },
    )
    val mutedWordsSheetState = rememberUpdatedMutedWordsSheetState(
        mutedWordPreferences = mutedWordsPreferences,
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
        signedInProfileId = signedInProfileId,
        recentConversations = recentConversations,
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
                    items.firstOrNull { it.post.uri == option.postUri }
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
            modifier = Modifier
                .padding(
                    horizontal = animateDpAsState(
                        presentation.timelineHorizontalPadding,
                    ).value,
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
                                floor(it.width / itemWidth).roundToInt(),
                            ),
                        ),
                    )
                },
            state = gridState,
            columns = StaggeredGridCells.Adaptive(presentation.cardSize),
            verticalItemSpacing = 8.dp,
            contentPadding = bottomNavAndInsetPaddingValues(
                isCompact = paneScaffoldState.prefersCompactBottomNav,
            ),
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
                                state = videoStates.getOrCreateStateFor(item),
                            ),
                        paneMovableElementSharedTransitionScope = paneScaffoldState,
                        presentationLookaheadScope = this@LookaheadScope,
                        now = remember { Clock.System.now() },
                        item = item,
                        sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
                        presentation = presentation,
                        postActions = remember(timelineState.timeline.sourceId) {
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
                                        val post = action.post
                                        pendingScrollOffset = gridState.pendingOffsetFor(item)
                                        actions(
                                            Action.Navigate.To(
                                                recordDestination(
                                                    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                    sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
                                                    otherModels = buildList {
                                                        action.warnedAppliedLabels?.let(::add)
                                                        if (action.isMainPost) {
                                                            add(timelineState.timeline.source)
                                                            add(timelineState.tilingData.currentQuery.data)
                                                        }
                                                    },
                                                    record = post,
                                                ),
                                            ),
                                        )
                                    }

                                    is PostAction.OfProfile -> {
                                        val profile = action.profile
                                        val post = action.post
                                        val quotingPostUri = action.quotingPostUri
                                        pendingScrollOffset = gridState.pendingOffsetFor(item)
                                        actions(
                                            Action.Navigate.To(
                                                profileDestination(
                                                    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                    profile = profile,
                                                    avatarSharedElementKey = post
                                                        .avatarSharedElementKey(
                                                            prefix = timelineState.timeline.sourceId,
                                                            quotingPostUri = quotingPostUri,
                                                        )
                                                        .takeIf { post.author.did == profile.did },
                                                ),
                                            ),
                                        )
                                    }

                                    is PostAction.OfRecord -> {
                                        val record = action.record
                                        val owningPostUri = action.owningPostUri
                                        pendingScrollOffset = gridState.pendingOffsetFor(item)
                                        actions(
                                            Action.Navigate.To(
                                                recordDestination(
                                                    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                    sharedElementPrefix = timelineState.timeline.sharedElementPrefix(
                                                        quotingPostUri = owningPostUri,
                                                    ),
                                                    record = record,
                                                ),
                                            ),
                                        )
                                    }

                                    is PostAction.OfMedia -> {
                                        val media = action.media
                                        val index = action.index
                                        val post = action.post
                                        val quotingPostUri = action.quotingPostUri
                                        pendingScrollOffset = gridState.pendingOffsetFor(item)
                                        actions(
                                            Action.Navigate.To(
                                                galleryDestination(
                                                    post = post,
                                                    media = media,
                                                    startIndex = index,
                                                    sharedElementPrefix = timelineState.timeline.sharedElementPrefix(
                                                        quotingPostUri = quotingPostUri,
                                                    ),
                                                    otherModels = when {
                                                        action.isMainPost -> listOf(
                                                            timelineState.timeline.source,
                                                            timelineState.tilingData.currentQuery.data,
                                                        )
                                                        else -> emptyList()
                                                    },
                                                ),
                                            ),
                                        )
                                    }

                                    is PostAction.OfReply -> {
                                        val post = action.post
                                        pendingScrollOffset = gridState.pendingOffsetFor(item)
                                        actions(
                                            Action.Navigate.To(
                                                if (paneScaffoldState.isSignedOut) signInDestination()
                                                else composePostDestination(
                                                    type = Post.Create.Reply(
                                                        parent = post,
                                                    ),
                                                    sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
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

                                    else -> Unit
                                }
                            }
                        },
                    )
                },
            )
        }
    }

    if (paneScaffoldState.paneState.pane == ThreePane.Primary && autoPlayTimelineVideos) {
        val videoPlayerController = LocalVideoPlayerController.current
        gridState.interpolatedVisibleIndexEffect(
            denominator = 10,
            itemsAvailable = items.size,
        ) { interpolatedIndex ->
            val flooredIndex = floor(interpolatedIndex).toInt()
            val fraction = interpolatedIndex - flooredIndex
            items.getOrNull(flooredIndex)
                ?.takeIf(TimelineItem::canAutoPlayVideo)
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
                        query ?: timelineState.tilingData.currentQuery,
                    ),
                ),
            )
        },
    )

    gridState.TimelineRefreshEffect(
        timelineState = timelineState,
        onRefresh = { animateScrollToItem(index = 0) },
    )
}

private fun Profile.listMemberAvatarSharedElementKey(): String =
    "list-member-${did.id}"
