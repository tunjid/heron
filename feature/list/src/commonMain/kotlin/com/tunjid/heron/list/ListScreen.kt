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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tunjid.composables.collapsingheader.CollapsingHeaderLayout
import com.tunjid.composables.collapsingheader.rememberCollapsingHeaderState
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.ListMember
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.core.models.sourceId
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.utilities.asGenericUri
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.sheets.postoptions.PostOption
import com.tunjid.heron.sheets.profile.ProfileRestrictionDialogState.Companion.rememberProfileRestrictionDialogState
import com.tunjid.heron.sheets.rememberMutedWordsSheetState
import com.tunjid.heron.sheets.rememberPostInteractionsSheetState
import com.tunjid.heron.sheets.rememberPostOptionsSheetState
import com.tunjid.heron.sheets.rememberTimelineThreadGateSheetState
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.state.TimelineStateHolder
import com.tunjid.heron.timeline.ui.DismissableRefreshIndicator
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.timeline.ui.PostActions
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.effects.TimelineRefreshEffect
import com.tunjid.heron.timeline.ui.profile.ProfileWithViewerState
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.contentType
import com.tunjid.heron.timeline.utilities.description
import com.tunjid.heron.timeline.utilities.onDominantVideoChange
import com.tunjid.heron.timeline.utilities.rememberTimelineDisplayState
import com.tunjid.heron.timeline.utilities.sharedElementPrefix
import com.tunjid.heron.ui.DestructiveDialogButton
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.SimpleDialog
import com.tunjid.heron.ui.SimpleDialogText
import com.tunjid.heron.ui.SimpleDialogTitle
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.UiTokens.bottomNavAndInsetPaddingValues
import com.tunjid.heron.ui.modifiers.gridColumnCount
import com.tunjid.heron.ui.roundedMaxDelta
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.composePostDestination
import com.tunjid.heron.ui.scaffold.navigation.conversationDestination
import com.tunjid.heron.ui.scaffold.navigation.galleryDestination
import com.tunjid.heron.ui.scaffold.navigation.pathDestination
import com.tunjid.heron.ui.scaffold.navigation.profileDestination
import com.tunjid.heron.ui.scaffold.navigation.recordDestination
import com.tunjid.heron.ui.scaffold.navigation.signInDestination
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.paneClip
import com.tunjid.heron.ui.tabIndex
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.mutator.compose.produceStateWithLifecycle
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.threepane.ThreePane
import heron.feature.list.generated.resources.Res
import heron.feature.list.generated.resources.people
import heron.feature.list.generated.resources.posts
import heron.feature.list.generated.resources.remove_list_member
import heron.feature.list.generated.resources.remove_list_member_confirmation
import heron.ui.core.generated.resources.no
import heron.ui.core.generated.resources.yes
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
    val pagerState = rememberPagerState { state.stateHolders.size }
    val scope = rememberCoroutineScope()

    val collapsedHeight = with(density) {
        UiTokens.tabsHeight.toPx()
    }
    val collapsingHeaderState = rememberCollapsingHeaderState(
        collapsedHeight = collapsedHeight,
        initialExpandedHeight = with(density) { 800.dp.toPx() },
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
                    when (val holder = state.stateHolders[pagerState.currentPage]) {
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
                    state.timelineState
                        ?.timeline
                        ?.description
                        ?.takeIf(String::isNotBlank)
                        ?.let { description ->
                            Text(
                                modifier = Modifier
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp,
                                    ),
                                text = description,
                            )
                        }
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
                                state.stateHolders.getOrNull(index = index)
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
                    key = { page -> state.stateHolders[page].key },
                    pageContent = { page ->
                        when (val stateHolder = state.stateHolders[page]) {
                            is ListScreenStateHolders.Members -> ListMembers(
                                paneScaffoldState = paneScaffoldState,
                                membersStateHolder = stateHolder,
                                actions = actions,
                            )

                            is ListScreenStateHolders.Timeline -> ListTimeline(
                                paneScaffoldState = paneScaffoldState,
                                timelineStateHolder = stateHolder,
                                actions = actions,
                                autoPlayTimelineVideos = state.preferences.local.autoPlayTimelineVideos,
                                showEngagementMetrics = state.preferences.local.showPostEngagementMetrics,
                            )
                        }
                    },
                )
            },
        )
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { currentPage ->
                actions(Action.CurrentPageChanged(currentPage))
            }
    }
}

@Composable
private fun listTabs(
    hasUpdate: Boolean,
): List<Tab> = listOf(
    Tab(
        title = stringResource(Res.string.posts),
        hasUpdate = hasUpdate,
    ),
    Tab(
        title = stringResource(Res.string.people),
        hasUpdate = false,
    ),
)

@Composable
private fun ListMembers(
    modifier: Modifier = Modifier,
    paneScaffoldState: PaneScaffoldState,
    membersStateHolder: MembersStateHolder,
    actions: (Action) -> Unit,
) {
    val state = membersStateHolder.produceStateWithLifecycle()
    val updatedMembers = state.tiledItems
    val listState = rememberLazyListState()

    var listMemberToDelete by remember { mutableStateOf<ListMember?>(null) }

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProfileWithViewerState(
                        modifier = Modifier
                            .weight(1f),
                        paneTransitionScope = paneScaffoldState,
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
                    if (state.signedInProfileId == state.listUri.profileId()) {
                        IconButton(
                            onClick = {
                                listMemberToDelete = item
                            },
                            content = {
                                Icon(
                                    imageVector = Icons.Rounded.RemoveCircle,
                                    contentDescription = stringResource(Res.string.remove_list_member),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                        )
                    }
                }
            },
        )
    }

    listMemberToDelete?.let { member ->
        SimpleDialog(
            onDismissRequest = {
                listMemberToDelete = null
            },
            title = {
                SimpleDialogTitle(
                    text = stringResource(Res.string.remove_list_member),
                )
            },
            text = {
                SimpleDialogText(
                    text = stringResource(
                        Res.string.remove_list_member_confirmation,
                        member.subject.handle.id,
                    ),
                )
            },
            confirmButton = {
                DestructiveDialogButton(
                    text = stringResource(CommonStrings.yes),
                ) {
                    actions(Action.DeleteRecord(member.uri))
                    listMemberToDelete = null
                }
            },
            dismissButton = {
                NeutralDialogButton(
                    text = stringResource(CommonStrings.no),
                    onClick = {
                        listMemberToDelete = null
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
    paneScaffoldState: PaneScaffoldState,
    timelineStateHolder: TimelineStateHolder,
    actions: (Action) -> Unit,
    autoPlayTimelineVideos: Boolean,
    showEngagementMetrics: Boolean,
) {
    val gridState = rememberLazyStaggeredGridState()
    val timelineState = timelineStateHolder.produceStateWithLifecycle()
    val items = timelineState.tiledItems

    val now = remember(timelineState.timeline.lastRefreshed) { Clock.System.now() }
    val density = LocalDensity.current
    val videoPlayerController = LocalVideoPlayerController.current
    val presentation = timelineState.timeline.presentation
    val displayState = rememberTimelineDisplayState()
    val postInteractionSheetState = paneScaffoldState.rememberPostInteractionsSheetState(
        sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
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
                        displayState.horizontalPadding(presentation),
                    ).value,
                )
                .fillMaxSize()
                .paneClip()
                .onDominantVideoChange(
                    // No top inset: the grid is laid out beneath the collapsing header, so its own
                    // bounds already exclude it.
                    bottomRightInset = {
                        paneScaffoldState.bottomNavigationNestedScrollConnection.roundedMaxDelta
                    },
                    isEnabled = {
                        paneScaffoldState.paneState.pane == ThreePane.Primary &&
                            autoPlayTimelineVideos
                    },
                    onIdChanged = { videoId ->
                        if (videoId != null) videoPlayerController.play(videoId = videoId)
                        else videoPlayerController.pauseActiveVideo()
                    },
                )
                .gridColumnCount(
                    density = density,
                    maxColumnWidth = displayState.cardSize(presentation),
                ) { numColumns ->
                    timelineStateHolder.accept(
                        TimelineState.Action.Tile(
                            tilingAction = TilingState.Action.GridSize(
                                numColumns = numColumns,
                            ),
                        ),
                    )
                },
            state = gridState,
            columns = StaggeredGridCells.Adaptive(displayState.cardSize(presentation)),
            verticalItemSpacing = displayState.verticalItemSpacing(presentation),
            contentPadding = bottomNavAndInsetPaddingValues(
                isCompact = paneScaffoldState.prefersCompactBottomNav,
            ),
            horizontalArrangement = Arrangement.spacedBy(
                displayState.horizontalItemSpacing(presentation),
            ),
            userScrollEnabled = !paneScaffoldState.isTransitionActive,
        ) {
            items(
                items = items,
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
                        sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
                        showEngagementMetrics = showEngagementMetrics,
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
