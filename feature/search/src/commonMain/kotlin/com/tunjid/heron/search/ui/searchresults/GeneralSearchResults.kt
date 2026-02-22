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

package com.tunjid.heron.search.ui.searchresults

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.composables.accumulatedoffsetnestedscrollconnection.rememberAccumulatedOffsetNestedScrollConnection
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.search.SearchResult
import com.tunjid.heron.search.SearchState
import com.tunjid.heron.search.State
import com.tunjid.heron.search.id
import com.tunjid.heron.search.key
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.ui.PagerTopGapCloseEffect
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.tabIndex
import com.tunjid.heron.ui.verticalOffsetProgress
import heron.feature.search.generated.resources.Res
import heron.feature.search.generated.resources.feeds
import heron.feature.search.generated.resources.latest
import heron.feature.search.generated.resources.people
import heron.feature.search.generated.resources.top
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GeneralSearchResults(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    state: State,
    paneScaffoldState: PaneScaffoldState,
    onRequestRecentLists: () -> Unit,
    onProfileClicked: (Profile, String) -> Unit,
    onViewerStateClicked: (ProfileWithViewerState) -> Unit,
    onLinkTargetClicked: (LinkTarget) -> Unit,
    onPostSearchResultProfileClicked: (profile: Profile, post: Post, sharedElementPrefix: String) -> Unit,
    onPostSearchResultClicked: (post: Post, sharedElementPrefix: String) -> Unit,
    onReplyToPost: (post: Post, sharedElementPrefix: String) -> Unit,
    onPostRecordClicked: (record: Record, sharedElementPrefix: String) -> Unit,
    onMediaClicked: (media: Embed.Media, index: Int, post: Post, sharedElementPrefix: String) -> Unit,
    onNavigate: (NavigationAction.Destination) -> Unit,
    onSendPostInteraction: (Post.Interaction) -> Unit,
    onFeedGeneratorClicked: (FeedGenerator, String) -> Unit,
    onTimelineUpdateClicked: (Timeline.Update) -> Unit,
    onSave: (mutedWordPreferences: List<MutedWordPreference>) -> Unit,
    onMuteAccountClicked: (signedInProfileId: ProfileId, profileId: ProfileId) -> Unit,
    onBlockAccountClicked: (signedInProfileId: ProfileId, profileId: ProfileId) -> Unit,
    onDeletePostClicked: (RecordUri) -> Unit,
) {
    Box(
        modifier = modifier,
    ) {
        val updatedSearchStateHolders by rememberUpdatedState(
            state.searchStateHolders,
        )
        val scope = rememberCoroutineScope()
        val topClearance = UiTokens.statusBarHeight + UiTokens.toolbarHeight
        var tabsCollapsed by rememberSaveable {
            mutableStateOf(false)
        }
        val tabsBackgroundColor = MaterialTheme.colorScheme.surface
        val tabsBackgroundProgress = animateFloatAsState(if (tabsCollapsed) 0f else 1f)
        val tabsOffsetNestedScrollConnection = rememberAccumulatedOffsetNestedScrollConnection(
            maxOffset = { Offset.Zero },
            minOffset = { Offset(x = 0f, y = -UiTokens.toolbarHeight.toPx()) },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .offset {
                    IntOffset(
                        x = 0,
                        y = topClearance.roundToPx(),
                    ) + tabsOffsetNestedScrollConnection.offset.round()
                }
                .drawBehind {
                    drawRect(
                        color = tabsBackgroundColor,
                        size = size.copy(width = size.width * tabsBackgroundProgress.value),
                    )
                },
        ) {
            Tabs(
                modifier = Modifier
                    .drawBehind {
                        val chipHeight = 32.dp.toPx()
                        drawRoundRect(
                            color = tabsBackgroundColor,
                            topLeft = Offset(x = 0f, y = (size.height - chipHeight) / 2),
                            size = size.copy(height = chipHeight),
                            cornerRadius = CornerRadius(size.maxDimension, size.maxDimension),
                        )
                    }
                    .wrapContentWidth()
                    .animateContentSize(),
                tabsState = rememberTabsState(
                    tabs = searchTabs(
                        isSignedIn = state.signedInProfile != null,
                        isQueryEditable = state.isQueryEditable,
                    ),
                    isCollapsed = tabsCollapsed,
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
        HorizontalPager(
            modifier = Modifier
                .zIndex(0f)
                .nestedScroll(tabsOffsetNestedScrollConnection)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                    ),
                ),
            state = pagerState,
            key = { page ->
                updatedSearchStateHolders[page].state.value.key
            },
            pageContent = { page ->
                val searchResultStateHolder = remember { updatedSearchStateHolders[page] }
                val searchResultState by searchResultStateHolder.state.collectAsStateWithLifecycle()
                val videoStates = remember { ThreadedVideoPositionStates(SearchResult.OfPost::id) }

                when (val resultState = searchResultState) {
                    is SearchState.OfPosts -> {
                        val gridState = rememberLazyStaggeredGridState()
                        PostSearchResults(
                            state = resultState,
                            gridState = gridState,
                            modifier = modifier,
                            signedInProfileId = state.signedInProfile?.did,
                            mutedWordPreferences = state.preferences.mutedWordPreferences,
                            autoPlayTimelineVideos = state.preferences.local.autoPlayTimelineVideos,
                            showEngagementMetrics = state.preferences.local.showPostEngagementMetrics,
                            recentLists = state.recentLists,
                            recentConversations = state.recentConversations,
                            videoStates = videoStates,
                            paneScaffoldState = paneScaffoldState,
                            onRequestRecentLists = onRequestRecentLists,
                            onLinkTargetClicked = onLinkTargetClicked,
                            onPostSearchResultProfileClicked = onPostSearchResultProfileClicked,
                            onPostSearchResultClicked = onPostSearchResultClicked,
                            onReplyToPost = onReplyToPost,
                            onPostRecordClicked = onPostRecordClicked,
                            onMediaClicked = onMediaClicked,
                            onNavigate = onNavigate,
                            onSendPostInteraction = onSendPostInteraction,
                            searchResultActions = searchResultStateHolder.accept,
                            onSave = onSave,
                            onMuteAccountClicked = onMuteAccountClicked,
                            onBlockAccountClicked = onBlockAccountClicked,
                            onDeletePostClicked = onDeletePostClicked,
                        )
                        tabsOffsetNestedScrollConnection.PagerTopGapCloseEffect(
                            pagerState = pagerState,
                            firstVisibleItemIndex = gridState::firstVisibleItemIndex,
                            firstVisibleItemScrollOffset = gridState::firstVisibleItemScrollOffset,
                            scrollBy = gridState::animateScrollBy,
                        )
                    }

                    is SearchState.OfProfiles -> {
                        val listState = rememberLazyListState()
                        ProfileSearchResults(
                            state = resultState,
                            listState = listState,
                            modifier = modifier,
                            paneScaffoldState = paneScaffoldState,
                            onProfileClicked = onProfileClicked,
                            onViewerStateClicked = {
                                onViewerStateClicked(it.profileWithViewerState)
                            },
                            searchResultActions = searchResultStateHolder.accept,
                        )
                        tabsOffsetNestedScrollConnection.PagerTopGapCloseEffect(
                            pagerState = pagerState,
                            firstVisibleItemIndex = listState::firstVisibleItemIndex,
                            firstVisibleItemScrollOffset = listState::firstVisibleItemScrollOffset,
                            scrollBy = listState::animateScrollBy,
                        )
                    }

                    is SearchState.OfFeedGenerators -> {
                        val listState = rememberLazyListState()
                        FeedSearchResults(
                            state = resultState,
                            listState = listState,
                            modifier = modifier,
                            paneScaffoldState = paneScaffoldState,
                            timelineRecordUrisToPinnedStatus = state.timelineRecordUrisToPinnedStatus,
                            onFeedGeneratorClicked = onFeedGeneratorClicked,
                            onTimelineUpdateClicked = onTimelineUpdateClicked,
                            searchResultActions = searchResultStateHolder.accept,
                        )
                        tabsOffsetNestedScrollConnection.PagerTopGapCloseEffect(
                            pagerState = pagerState,
                            firstVisibleItemIndex = listState::firstVisibleItemIndex,
                            firstVisibleItemScrollOffset = listState::firstVisibleItemScrollOffset,
                            scrollBy = listState::animateScrollBy,
                        )
                    }
                }
            },
        )

        LaunchedEffect(tabsOffsetNestedScrollConnection) {
            snapshotFlow {
                tabsOffsetNestedScrollConnection.verticalOffsetProgress() > 0.5f
            }
                .distinctUntilChanged()
                .collect { isCollapsed ->
                    tabsCollapsed = isCollapsed
                }
        }
    }
}

@Composable
private fun searchTabs(
    isSignedIn: Boolean,
    isQueryEditable: Boolean,
): List<Tab> = buildList {
    if (isSignedIn) add(stringResource(resource = Res.string.top))
    if (isSignedIn) add(stringResource(resource = Res.string.latest))
    if (isQueryEditable) add(stringResource(resource = Res.string.people))
    if (isQueryEditable) add(stringResource(resource = Res.string.feeds))
}
    .map { Tab(title = it, hasUpdate = false) }
