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

package com.tunjid.heron.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.JoinFull
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.composables.accumulatedoffsetnestedscrollconnection.rememberAccumulatedOffsetNestedScrollConnection
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.ListMember
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.Trend
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.utilities.path
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.composePostDestination
import com.tunjid.heron.scaffold.navigation.conversationDestination
import com.tunjid.heron.scaffold.navigation.galleryDestination
import com.tunjid.heron.scaffold.navigation.pathDestination
import com.tunjid.heron.scaffold.navigation.postDestination
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.navigation.signInDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.SignInPopUpState.Companion.rememberSignInPopUpState
import com.tunjid.heron.search.ui.PostSearchResult
import com.tunjid.heron.search.ui.ProfileSearchResult
import com.tunjid.heron.search.ui.SuggestedStarterPack
import com.tunjid.heron.search.ui.Trend
import com.tunjid.heron.search.ui.avatarSharedElementKey
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.feed.FeedGenerator
import com.tunjid.heron.timeline.ui.post.PostInteractionsSheetState.Companion.rememberUpdatedPostInteractionState
import com.tunjid.heron.timeline.ui.post.PostOptionsSheetState.Companion.rememberUpdatedPostOptionsState
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.timeline.ui.profile.ProfileWithViewerState
import com.tunjid.heron.timeline.ui.withQuotingPostUriPrefix
import com.tunjid.heron.timeline.utilities.cardSize
import com.tunjid.heron.timeline.utilities.lazyGridHorizontalItemSpacing
import com.tunjid.heron.ui.PagerTopGapCloseEffect
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.UiTokens.bottomNavAndInsetPaddingValues
import com.tunjid.heron.ui.tabIndex
import com.tunjid.heron.ui.verticalOffsetProgress
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import com.tunjid.treenav.compose.threepane.ThreePane
import heron.feature.search.generated.resources.Res
import heron.feature.search.generated.resources.discover_feeds
import heron.feature.search.generated.resources.feeds
import heron.feature.search.generated.resources.latest
import heron.feature.search.generated.resources.people
import heron.feature.search.generated.resources.starter_packs
import heron.feature.search.generated.resources.suggested_accounts
import heron.feature.search.generated.resources.top
import heron.feature.search.generated.resources.trending_title
import kotlin.math.floor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SearchScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val signInPopUpState = rememberSignInPopUpState {
        actions(Action.Navigate.To(signInDestination()))
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
                        sharedElementPrefix = null,
                    ),
                ),
            )
        },
    )

    val postOptionsState = rememberUpdatedPostOptionsState(
        signedInProfileId = state.signedInProfile?.did,
        recentConversations = state.recentConversations,
        onShareInConversationClicked = { currentPost, conversation ->
            actions(
                Action.Navigate.To(
                    conversationDestination(
                        id = conversation.id,
                        members = conversation.members,
                        sharedElementPrefix = conversation.id.id,
                        sharedPostUri = currentPost.uri,
                        referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                    ),
                ),
            )
        },
    )

    val pagerState = rememberPagerState { state.searchStateHolders.size }
    val onProfileClicked: (ProfileWithViewerState) -> Unit = remember {
        { profileWithViewerState ->
            actions(
                Action.Navigate.To(
                    profileDestination(
                        profile = profileWithViewerState.profile,
                        avatarSharedElementKey = profileWithViewerState
                            .profile
                            .searchProfileAvatarSharedElementKey(),
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                    ),
                ),
            )
        }
    }
    val onViewerStateClicked: (ProfileWithViewerState) -> Unit =
        remember(state.signedInProfile?.did) {
            { profileWithViewerState ->
                state.signedInProfile?.did?.let {
                    actions(
                        Action.ToggleViewerState(
                            signedInProfileId = it,
                            viewedProfileId = profileWithViewerState.profile.did,
                            following = profileWithViewerState.viewerState?.following,
                            followedBy = profileWithViewerState.viewerState?.followedBy,
                        ),
                    )
                }
            }
        }
    val onProfileSearchResultClicked: (SearchResult.OfProfile) -> Unit = remember {
        { profileSearchResult ->
            actions(
                Action.Navigate.To(
                    profileDestination(
                        profile = profileSearchResult.profileWithViewerState.profile,
                        avatarSharedElementKey = profileSearchResult.avatarSharedElementKey(),
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                    ),
                ),
            )
        }
    }
    val onLinkTargetClicked = remember {
        { _: SearchResult.OfPost, linkTarget: LinkTarget ->
            if (linkTarget is LinkTarget.Navigable) actions(
                Action.Navigate.To(
                    pathDestination(
                        path = linkTarget.path,
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                    ),
                ),
            )
        }
    }
    val onPostSearchResultProfileClicked = remember {
        { profile: Profile, post: Post, sharedElementPrefix: String ->
            actions(
                Action.Navigate.To(
                    profileDestination(
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                        profile = profile,
                        avatarSharedElementKey = post.avatarSharedElementKey(
                            sharedElementPrefix,
                        ),
                    ),
                ),
            )
        }
    }
    val onListMemberClicked = remember {
        { listMember: ListMember ->
            actions(
                Action.Navigate.To(
                    profileDestination(
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                        profile = listMember.subject,
                        avatarSharedElementKey = listMember.avatarSharedElementKey(),
                    ),
                ),
            )
        }
    }
    val onTrendClicked = remember {
        { trend: Trend ->
            actions(
                Action.Navigate.To(
                    pathDestination(
                        path = trend.link,
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                    ),
                ),
            )
        }
    }
    val onFeedGeneratorClicked = remember {
        { feedGenerator: FeedGenerator ->
            actions(
                Action.Navigate.To(
                    pathDestination(
                        path = feedGenerator.uri.path,
                        models = listOf(feedGenerator),
                        sharedElementPrefix = SearchFeedGeneratorSharedElementPrefix,
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                    ),
                ),
            )
        }
    }
    val onTimelineUpdateClicked = remember {
        { update: Timeline.Update ->
            if (paneScaffoldState.isSignedOut) signInPopUpState.show()
            else actions(Action.UpdateFeedGeneratorStatus(update))
        }
    }
    val onPostSearchResultClicked = remember {
        { post: Post, sharedElementPrefix: String ->
            actions(
                Action.Navigate.To(
                    postDestination(
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                        sharedElementPrefix = sharedElementPrefix,
                        post = post,
                    ),
                ),
            )
        }
    }
    val onReplyToPost = remember {
        { result: SearchResult.OfPost ->
            actions(
                Action.Navigate.To(
                    if (paneScaffoldState.isSignedOut) signInDestination()
                    else composePostDestination(
                        type = Post.Create.Reply(
                            parent = result.post,
                        ),
                        sharedElementPrefix = result.sharedElementPrefix,
                    ),
                ),
            )
        }
    }
    val onMediaClicked = remember {
        { media: Embed.Media, index: Int, result: SearchResult.OfPost, quotingPostUri: PostUri? ->
            actions(
                Action.Navigate.To(
                    galleryDestination(
                        post = result.post,
                        media = media,
                        startIndex = index,
                        sharedElementPrefix = result.sharedElementPrefix.withQuotingPostUriPrefix(
                            quotingPostUri = quotingPostUri,
                        ),
                    ),
                ),
            )
        }
    }
    val onPostInteraction = postInteractionState::onInteraction
    val onPostOptionsClicked = postOptionsState::showOptions

    AnimatedContent(
        targetState = state.layout,
    ) { targetLayout ->
        when (targetLayout) {
            ScreenLayout.Suggested -> SuggestedContent(
                modifier = modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                movableElementSharedTransitionScope = paneScaffoldState,
                trends = state.trends,
                suggestedProfiles = state.categoriesToSuggestedProfiles[state.suggestedProfileCategory]
                    ?: emptyList(),
                starterPacksWithMembers = state.starterPacksWithMembers,
                feedGenerators = state.feedGenerators,
                feedGeneratorUrisToPinnedStatus = state.feedGeneratorUrisToPinnedStatus,
                onProfileClicked = onProfileClicked,
                onViewerStateClicked = onViewerStateClicked,
                onListMemberClicked = onListMemberClicked,
                onTrendClicked = onTrendClicked,
                onFeedGeneratorClicked = onFeedGeneratorClicked,
                onUpdateTimelineClicked = onTimelineUpdateClicked,
            )

            ScreenLayout.AutoCompleteProfiles -> AutoCompleteProfileSearchResults(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                paneMovableElementSharedTransitionScope = paneScaffoldState,
                results = state.autoCompletedProfiles,
                onProfileClicked = onProfileSearchResultClicked,
                onViewerStateClicked = onViewerStateClicked,
            )

            ScreenLayout.GeneralSearchResults -> TabbedSearchResults(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                pagerState = pagerState,
                state = state,
                paneScaffoldState = paneScaffoldState,
                onProfileClicked = onProfileSearchResultClicked,
                onViewerStateClicked = onViewerStateClicked,
                onLinkTargetClicked = onLinkTargetClicked,
                onPostSearchResultProfileClicked = onPostSearchResultProfileClicked,
                onPostSearchResultClicked = onPostSearchResultClicked,
                onReplyToPost = onReplyToPost,
                onMediaClicked = onMediaClicked,
                onPostInteraction = onPostInteraction,
                onFeedGeneratorClicked = onFeedGeneratorClicked,
                onTimelineUpdateClicked = onTimelineUpdateClicked,
                onPostOptionsClicked = onPostOptionsClicked,
            )
        }
    }

    LifecycleStartEffect(Unit) {
        actions(
            Action.FetchSuggestedProfiles(
                category = state.suggestedProfileCategory,
            ),
        )
        onStopOrDispose { }
    }
}

@Composable
private fun SuggestedContent(
    modifier: Modifier = Modifier,
    movableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    trends: List<Trend>,
    suggestedProfiles: List<ProfileWithViewerState>,
    starterPacksWithMembers: List<SuggestedStarterPack>,
    feedGenerators: List<FeedGenerator>,
    feedGeneratorUrisToPinnedStatus: Map<FeedGeneratorUri?, Boolean>,
    onTrendClicked: (Trend) -> Unit,
    onProfileClicked: (ProfileWithViewerState) -> Unit,
    onViewerStateClicked: (ProfileWithViewerState) -> Unit,
    onListMemberClicked: (ListMember) -> Unit,
    onFeedGeneratorClicked: (FeedGenerator) -> Unit,
    onUpdateTimelineClicked: (Timeline.Update) -> Unit,
) {
    val now = remember { Clock.System.now() }
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = bottomNavAndInsetPaddingValues(
            top = UiTokens.statusBarHeight + UiTokens.toolbarHeight,
        ),
    ) {
        item {
            TrendTitle(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .animateItem(),
                icon = Icons.AutoMirrored.Rounded.ShowChart,
                title = stringResource(Res.string.trending_title),
            )
        }
        itemsIndexed(
            items = trends.take(5),
            key = { _, trend -> trend.link },
            itemContent = { index, trend ->
                Trend(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .clickable { onTrendClicked(trend) }
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                    index = index,
                    now = now,
                    trend = trend,
                    onTrendClicked = onTrendClicked,
                )
            },
        )
        if (suggestedProfiles.isNotEmpty()) item {
            TrendTitle(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .animateItem(),
                icon = Icons.Rounded.AccountCircle,
                title = stringResource(Res.string.suggested_accounts),
            )
        }
        items(
            items = suggestedProfiles.take(5),
            key = { suggestedProfile -> suggestedProfile.profile.did.id },
            itemContent = { suggestedProfile ->
                ProfileWithViewerState(
                    modifier = Modifier
                        .clickable { onProfileClicked(suggestedProfile) }
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                    movableElementSharedTransitionScope = movableElementSharedTransitionScope,
                    signedInProfileId = null,
                    profile = suggestedProfile.profile,
                    viewerState = suggestedProfile.viewerState,
                    profileSharedElementKey = Profile::searchProfileAvatarSharedElementKey,
                    onProfileClicked = { onProfileClicked(suggestedProfile) },
                    onViewerStateClicked = { onViewerStateClicked(suggestedProfile) },
                )
            },
        )
        if (starterPacksWithMembers.isNotEmpty()) item {
            TrendTitle(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .animateItem(),
                icon = Icons.Rounded.JoinFull,
                title = stringResource(Res.string.starter_packs),
            )
        }
        items(
            items = starterPacksWithMembers.take(5),
            key = { starterPackWithMember -> starterPackWithMember.starterPack.cid.id },
            itemContent = { starterPackWithMember ->
                SuggestedStarterPack(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                    movableElementSharedTransitionScope = movableElementSharedTransitionScope,
                    starterPackWithMembers = starterPackWithMember,
                    onListMemberClicked = onListMemberClicked,
                )
            },
        )
        if (feedGenerators.isNotEmpty()) item {
            TrendTitle(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .animateItem(),
                icon = Icons.Rounded.RssFeed,
                title = stringResource(Res.string.discover_feeds),
            )
        }
        items(
            items = feedGenerators.take(5),
            key = { feedGenerator -> feedGenerator.cid.id },
            itemContent = { feedGenerator ->
                FeedGenerator(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .animateItem(),
                    movableElementSharedTransitionScope = movableElementSharedTransitionScope,
                    sharedElementPrefix = SearchFeedGeneratorSharedElementPrefix,
                    feedGenerator = feedGenerator,
                    status = when (feedGeneratorUrisToPinnedStatus[feedGenerator.uri]) {
                        true -> FeedGenerator.Status.Pinned
                        false -> FeedGenerator.Status.Saved
                        null -> FeedGenerator.Status.None
                    },
                    onFeedGeneratorClicked = onFeedGeneratorClicked,
                    onFeedGeneratorStatusUpdated = onUpdateTimelineClicked,
                )
            },
        )
        item {
            Spacer(
                Modifier
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .height(UiTokens.bottomNavHeight),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrendTitle(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
) {
    Column(
        modifier = modifier.padding(
            vertical = 8.dp,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            Icon(
                modifier = Modifier
                    .size(24.dp),
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AutoCompleteProfileSearchResults(
    paneMovableElementSharedTransitionScope: PaneScaffoldState,
    modifier: Modifier = Modifier,
    results: List<SearchResult.OfProfile>,
    onProfileClicked: (SearchResult.OfProfile) -> Unit,
    onViewerStateClicked: (ProfileWithViewerState) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = bottomNavAndInsetPaddingValues(
            top = UiTokens.statusBarHeight + UiTokens.toolbarHeight,
            horizontal = 16.dp,
        ),
    ) {
        items(
            items = results,
            key = { it.profileWithViewerState.profile.did.id },
            itemContent = { result ->
                ProfileSearchResult(
                    paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                    result = result,
                    onProfileClicked = onProfileClicked,
                    onViewerStateClicked = { onViewerStateClicked(it.profileWithViewerState) },
                )
            },
        )
    }
}

@Composable
private fun TabbedSearchResults(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    state: State,
    paneScaffoldState: PaneScaffoldState,
    onProfileClicked: (SearchResult.OfProfile) -> Unit,
    onViewerStateClicked: (ProfileWithViewerState) -> Unit,
    onLinkTargetClicked: (SearchResult.OfPost, LinkTarget) -> Unit,
    onPostSearchResultProfileClicked: (profile: Profile, post: Post, sharedElementPrefix: String) -> Unit,
    onPostSearchResultClicked: (post: Post, sharedElementPrefix: String) -> Unit,
    onReplyToPost: (SearchResult.OfPost) -> Unit,
    onMediaClicked: (media: Embed.Media, index: Int, result: SearchResult.OfPost, quotingPostUri: PostUri?) -> Unit,
    onPostInteraction: (Post.Interaction) -> Unit,
    onPostOptionsClicked: (Post) -> Unit,
    onFeedGeneratorClicked: (FeedGenerator) -> Unit,
    onTimelineUpdateClicked: (Timeline.Update) -> Unit,
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
                            videoStates = videoStates,
                            paneScaffoldState = paneScaffoldState,
                            onLinkTargetClicked = onLinkTargetClicked,
                            onPostSearchResultProfileClicked = onPostSearchResultProfileClicked,
                            onPostSearchResultClicked = onPostSearchResultClicked,
                            onReplyToPost = onReplyToPost,
                            onMediaClicked = onMediaClicked,
                            onPostInteraction = onPostInteraction,
                            onPostOptionsClicked = onPostOptionsClicked,
                            searchResultActions = searchResultStateHolder.accept,
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
                            feedGeneratorUrisToPinnedStatus = state.feedGeneratorUrisToPinnedStatus,
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

@Composable
private fun PostSearchResults(
    state: SearchState.OfPosts,
    gridState: LazyStaggeredGridState,
    modifier: Modifier,
    videoStates: ThreadedVideoPositionStates<SearchResult.OfPost>,
    paneScaffoldState: PaneScaffoldState,
    onLinkTargetClicked: (SearchResult.OfPost, LinkTarget) -> Unit,
    onPostSearchResultProfileClicked: (profile: Profile, post: Post, sharedElementPrefix: String) -> Unit,
    onPostSearchResultClicked: (post: Post, sharedElementPrefix: String) -> Unit,
    onReplyToPost: (SearchResult.OfPost) -> Unit,
    onMediaClicked: (media: Embed.Media, index: Int, result: SearchResult.OfPost, quotingPostUri: PostUri?) -> Unit,
    onPostInteraction: (Post.Interaction) -> Unit,
    onPostOptionsClicked: (Post) -> Unit,
    searchResultActions: (SearchState.Tile) -> Unit,
) {
    val now = remember { Clock.System.now() }
    val results by rememberUpdatedState(state.tiledItems)
    LazyVerticalStaggeredGrid(
        modifier = modifier,
        state = gridState,
        columns = StaggeredGridCells.Adaptive(
            Timeline.Presentation.Text.WithEmbed.cardSize,
        ),
        verticalItemSpacing = 16.dp,
        contentPadding = bottomNavAndInsetPaddingValues(
            top = UiTokens.statusBarHeight + UiTokens.toolbarHeight + UiTokens.tabsHeight,
        ),
        horizontalArrangement = Arrangement.spacedBy(
            Timeline.Presentation.Text.WithEmbed.lazyGridHorizontalItemSpacing,
        ),
    ) {
        items(
            items = results,
            key = { it.post.cid.id },
            itemContent = { result ->
                PostSearchResult(
                    modifier = Modifier
                        .threadedVideoPosition(
                            state = videoStates.getOrCreateStateFor(result),
                        )
                        .animateItem(),
                    paneMovableElementSharedTransitionScope = paneScaffoldState,
                    now = now,
                    result = result,
                    onLinkTargetClicked = onLinkTargetClicked,
                    onProfileClicked = onPostSearchResultProfileClicked,
                    onPostClicked = onPostSearchResultClicked,
                    onReplyToPost = onReplyToPost,
                    onMediaClicked = onMediaClicked,
                    onPostInteraction = onPostInteraction,
                    onPostOptionsClicked = onPostOptionsClicked,
                )
            },
        )
    }
    if (paneScaffoldState.paneState.pane == ThreePane.Primary) {
        val videoPlayerController = LocalVideoPlayerController.current
        gridState.interpolatedVisibleIndexEffect(
            denominator = 10,
            itemsAvailable = results.size,
        ) { interpolatedIndex ->
            val flooredIndex = floor(interpolatedIndex).toInt()
            val fraction = interpolatedIndex - flooredIndex
            results.getOrNull(flooredIndex)
                ?.takeIf(SearchResult.OfPost::canAutoPlayVideo)
                ?.let(videoStates::retrieveStateFor)
                ?.videoIdAt(fraction)
                ?.let(videoPlayerController::play)
                ?: videoPlayerController.pauseActiveVideo()
        }
    }
    gridState.PivotedTilingEffect(
        items = results,
        onQueryChanged = { query ->
            searchResultActions(
                SearchState.Tile(
                    tilingAction = TilingState.Action.LoadAround(
                        query ?: state.tilingData.currentQuery,
                    ),
                ),
            )
        },
    )
}

@Composable
private fun ProfileSearchResults(
    state: SearchState.OfProfiles,
    listState: LazyListState,
    modifier: Modifier,
    paneScaffoldState: PaneScaffoldState,
    onProfileClicked: (SearchResult.OfProfile) -> Unit,
    onViewerStateClicked: (SearchResult.OfProfile) -> Unit,
    searchResultActions: (SearchState.Tile) -> Unit,
) {
    val results by rememberUpdatedState(state.tiledItems)
    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = bottomNavAndInsetPaddingValues(
            top = UiTokens.statusBarHeight + UiTokens.toolbarHeight + UiTokens.tabsHeight,
        ),
    ) {
        items(
            items = results,
            key = { it.profileWithViewerState.profile.did.id },
            itemContent = { result ->
                ProfileSearchResult(
                    paneMovableElementSharedTransitionScope = paneScaffoldState,
                    result = result,
                    onProfileClicked = onProfileClicked,
                    onViewerStateClicked = onViewerStateClicked,
                )
            },
        )
    }
    listState.PivotedTilingEffect(
        items = results,
        onQueryChanged = { query ->
            searchResultActions(
                SearchState.Tile(
                    tilingAction = TilingState.Action.LoadAround(
                        query ?: state.tilingData.currentQuery,
                    ),
                ),
            )
        },
    )
}

@Composable
private fun FeedSearchResults(
    state: SearchState.OfFeedGenerators,
    listState: LazyListState,
    modifier: Modifier,
    paneScaffoldState: PaneScaffoldState,
    feedGeneratorUrisToPinnedStatus: Map<FeedGeneratorUri?, Boolean>,
    onFeedGeneratorClicked: (FeedGenerator) -> Unit,
    onTimelineUpdateClicked: (Timeline.Update) -> Unit,
    searchResultActions: (SearchState.Tile) -> Unit,
) {
    val results by rememberUpdatedState(state.tiledItems)
    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = bottomNavAndInsetPaddingValues(
            top = UiTokens.statusBarHeight + UiTokens.toolbarHeight + UiTokens.tabsHeight,
        ),
    ) {
        items(
            items = results,
            key = { it.feedGenerator.cid.id },
            itemContent = { result ->
                FeedGenerator(
                    modifier = Modifier
                        .animateItem(),
                    movableElementSharedTransitionScope = paneScaffoldState,
                    sharedElementPrefix = SearchFeedGeneratorSharedElementPrefix,
                    feedGenerator = result.feedGenerator,
                    status = when (feedGeneratorUrisToPinnedStatus[result.feedGenerator.uri]) {
                        true -> FeedGenerator.Status.Pinned
                        false -> FeedGenerator.Status.Saved
                        null -> FeedGenerator.Status.None
                    },
                    onFeedGeneratorClicked = onFeedGeneratorClicked,
                    onFeedGeneratorStatusUpdated = onTimelineUpdateClicked,
                )
            },
        )
    }
    listState.PivotedTilingEffect(
        items = results,
        onQueryChanged = { query ->
            searchResultActions(
                SearchState.Tile(
                    tilingAction = TilingState.Action.LoadAround(
                        query ?: state.tilingData.currentQuery,
                    ),
                ),
            )
        },
    )
}

private fun Profile.searchProfileAvatarSharedElementKey(): String =
    "suggested-profile-${did.id}"

internal const val SearchFeedGeneratorSharedElementPrefix = "search-feedGenerator"
