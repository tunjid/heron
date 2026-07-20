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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
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
import com.tunjid.composables.accumulatedoffsetnestedscrollconnection.rememberAccumulatedOffsetNestedScrollConnection
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.search.RouteQuery
import com.tunjid.heron.search.SearchState
import com.tunjid.heron.search.State
import com.tunjid.heron.search.key
import com.tunjid.heron.search.presentationOptions
import com.tunjid.heron.search.supportsNonPostSearch
import com.tunjid.heron.timeline.ui.TimelinePresentationSelector
import com.tunjid.heron.ui.PagerTopGapCloseEffect
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.tabIndex
import com.tunjid.heron.ui.verticalOffsetProgress
import com.tunjid.mutator.compose.produceStateWithLifecycle
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
    onProfileClicked: (Profile, String) -> Unit,
    onViewerStateClicked: (ProfileWithViewerState) -> Unit,
    onLinkTargetClicked: (LinkTarget) -> Unit,
    onPostSearchResultProfileClicked: (profile: Profile, post: Post, sharedElementPrefix: String) -> Unit,
    onPostSearchResultClicked: (post: Post, sharedElementPrefix: String) -> Unit,
    onReplyToPost: (post: Post, sharedElementPrefix: String) -> Unit,
    onPostRecordClicked: (record: Record, sharedElementPrefix: String) -> Unit,
    onPublicationSubscriptionToggled: (StandardPublication) -> Unit,
    onMediaClicked: (media: Embed.Media, index: Int, post: Post, sharedElementPrefix: String) -> Unit,
    onNavigate: (NavigationAction.Destination) -> Unit,
    onFeedGeneratorClicked: (FeedGenerator, String) -> Unit,
    onTimelineUpdateClicked: (Timeline.Update) -> Unit,
    onMuteAccountClicked: (signedInProfileId: ProfileId, profileId: ProfileId) -> Unit,
    onBlockAccountClicked: (signedInProfileId: ProfileId, profileId: ProfileId) -> Unit,
    onDeletePostClicked: (RecordUri) -> Unit,
    onPresentationSelected: (Timeline.Presentation) -> Unit,
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
                .padding(horizontal = 8.dp)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f),
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
                            .animateContentSize(),
                        tabsState = rememberTabsState(
                            tabs = searchTabs(
                                isSignedIn = state.signedInProfile != null,
                                query = state.query,
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
                val availablePresentations = state.presentationOptions(pagerState.currentPage)
                val resolvedPresentation = remember(
                    state.preferredPresentation,
                    availablePresentations,
                ) {
                    if (state.preferredPresentation in availablePresentations) state.preferredPresentation
                    else Timeline.Presentation.Text.WithEmbed
                }
                TimelinePresentationSelector(
                    selected = resolvedPresentation,
                    available = availablePresentations,
                    onPresentationSelected = onPresentationSelected,
                )
            }
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
                updatedSearchStateHolders[page].state.key
            },
            pageContent = { page ->
                val searchResultStateHolder = remember { updatedSearchStateHolders[page] }
                when (val searchResultState = searchResultStateHolder.produceStateWithLifecycle()) {
                    is SearchState.OfPosts -> {
                        val gridState = rememberLazyStaggeredGridState()
                        PostSearchResults(
                            state = searchResultState,
                            gridState = gridState,
                            modifier = modifier,
                            presentation = state.preferredPresentation,
                            autoPlayTimelineVideos = state.preferences.local.autoPlayTimelineVideos,
                            isActivePage = { pagerState.currentPage == page },
                            showEngagementMetrics = state.preferences.local.showPostEngagementMetrics,
                            paneScaffoldState = paneScaffoldState,
                            onLinkTargetClicked = onLinkTargetClicked,
                            onPostSearchResultProfileClicked = onPostSearchResultProfileClicked,
                            onPostSearchResultClicked = onPostSearchResultClicked,
                            onReplyToPost = onReplyToPost,
                            onPostRecordClicked = onPostRecordClicked,
                            onPublicationSubscriptionToggled = onPublicationSubscriptionToggled,
                            onMediaClicked = onMediaClicked,
                            onNavigate = onNavigate,
                            searchResultActions = searchResultStateHolder.accept,
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
                            state = searchResultState,
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
                            state = searchResultState,
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
    query: RouteQuery,
): List<Tab> {
    val top = stringResource(resource = Res.string.top)
    val latest = stringResource(resource = Res.string.latest)
    val people = stringResource(resource = Res.string.people)
    val feeds = stringResource(resource = Res.string.feeds)
    val supportsNonPostSearch = query.supportsNonPostSearch
    // only pass 1 string resource here to prevent allocation on >4 remember args
    return remember(isSignedIn, supportsNonPostSearch, top) {
        buildList {
            if (isSignedIn) {
                add(top)
                add(latest)
            }
            if (supportsNonPostSearch) {
                add(people)
                add(feeds)
            }
        }.map { Tab(title = it, hasUpdate = false) }
    }
}
