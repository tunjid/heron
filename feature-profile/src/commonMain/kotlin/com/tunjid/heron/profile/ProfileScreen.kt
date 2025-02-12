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

package com.tunjid.heron.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.composables.collapsingheader.CollapsingHeaderLayout
import com.tunjid.composables.collapsingheader.CollapsingHeaderState
import com.tunjid.composables.collapsingheader.rememberCollapsingHeaderState
import com.tunjid.composables.ui.lerp
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.domain.timeline.TimelineLoadAction
import com.tunjid.heron.domain.timeline.TimelineStateHolder
import com.tunjid.heron.domain.timeline.TimelineStatus
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.paneClip
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.effects.PauseVideoOnTabChangeEffect
import com.tunjid.heron.timeline.ui.effects.TimelineRefreshEffect
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.timeline.ui.profile.ProfileHandle
import com.tunjid.heron.timeline.ui.profile.ProfileName
import com.tunjid.heron.timeline.ui.profile.ProfileViewerState
import com.tunjid.heron.timeline.utilities.displayName
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.tabIndex
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import com.tunjid.treenav.compose.threepane.ThreePane
import heron.feature_profile.generated.resources.Res
import heron.feature_profile.generated.resources.followers
import heron.feature_profile.generated.resources.following
import heron.feature_profile.generated.resources.posts
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.stringResource
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
internal fun ProfileScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    val collapsedHeight = with(density) {
        (UiTokens.toolbarHeight + UiTokens.statusBarHeight).toPx()
    }

    val collapsingHeaderState = rememberCollapsingHeaderState(
        collapsedHeight = collapsedHeight,
        initialExpandedHeight = with(density) { 800.dp.toPx() },
    )
    val headerState = remember(collapsingHeaderState) {
        HeaderState(collapsingHeaderState)
    }
    val updatedTimelineStateHolders by rememberUpdatedState(
        state.timelineStateHolders
    )
    val pagerState = rememberPagerState {
        updatedTimelineStateHolders.size
    }

    CollapsingHeaderLayout(
        modifier = modifier
            .onPlaced { headerState.width = with(density) { it.size.width.toDp() } },
        state = headerState.headerState,
        headerContent = {
            ProfileHeader(
                movableSharedElementScope = paneScaffoldState,
                headerState = headerState,
                pagerState = pagerState,
                timelineTabs = (0..<updatedTimelineStateHolders.size).map { page ->
                    val timeline = remember {
                        updatedTimelineStateHolders.stateHolderAt(page).state
                            .value
                            .timeline
                    }
                    check(timeline is Timeline.Profile)
                    Tab(
                        title = timeline.displayName(),
                        hasUpdate = state.sourceIdsToHasUpdates[timeline.sourceId] == true,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth(),
                profile = state.profile,
                isSignedInProfile = state.isSignedInProfile,
                viewerState = state.viewerState,
                avatarSharedElementKey = state.avatarSharedElementKey,
                onRefreshTabClicked = { index ->
                    updatedTimelineStateHolders.stateHolderAt(
                        index = index
                    ).accept(TimelineLoadAction.Refresh)
                },
                onViewerStateClicked = { viewerState ->
                    state.signedInProfileId?.let {
                        actions(
                            Action.ToggleViewerState(
                                signedInProfileId = it,
                                viewedProfileId = state.profile.did,
                                following = viewerState?.following,
                                followedBy = viewerState?.followedBy,
                            )
                        )
                    }
                },
                onNavigateToProfiles = { navigationAction ->
                    actions(Action.Navigate.DelegateTo(navigationAction))
                },
            )
        },
        body = {
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
            ) {
                HorizontalPager(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .paneClip(),
                    state = pagerState,
                    key = { page -> page },
                    contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                    pageContent = { page ->
                        val timelineStateHolder = remember {
                            updatedTimelineStateHolders.stateHolderAt(page)
                        }
                        ProfileTimeline(
                            panedSharedElementScope = paneScaffoldState,
                            timelineStateHolder = timelineStateHolder,
                            actions = actions,
                        )
                    }
                )

                pagerState.PauseVideoOnTabChangeEffect()
            }
        }
    )
}

@Composable
private fun ProfileHeader(
    movableSharedElementScope: MovableSharedElementScope,
    headerState: HeaderState,
    pagerState: PagerState,
    timelineTabs: List<Tab>,
    modifier: Modifier = Modifier,
    profile: Profile,
    isSignedInProfile: Boolean,
    viewerState: ProfileViewerState?,
    avatarSharedElementKey: String,
    onRefreshTabClicked: (Int) -> Unit,
    onViewerStateClicked: (ProfileViewerState?) -> Unit,
    onNavigateToProfiles: (NavigationAction.Common.ToProfiles.Profile) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        ProfileBanner(
            modifier = Modifier
                .align(Alignment.TopCenter),
            headerState = headerState,
            profile = profile,
        )
        Column(
            modifier = modifier
                .padding(top = headerState.bioTopPadding)
                .offset {
                    headerState.bioOffset()
                }
        ) {
            Column(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = headerState.bioAlpha),
                        shape = remember {
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                            )
                        }
                    )
                    .padding(start = headerState.sizeToken, end = headerState.sizeToken)
                    .graphicsLayer {
                        alpha = headerState.bioAlpha
                    },
            ) {
                Spacer(Modifier.height(32.dp))
                ProfileHeadline(
                    modifier = Modifier.fillMaxWidth(),
                    profile = profile,
                    isSignedInProfile = isSignedInProfile,
                    viewerState = viewerState,
                    onViewerStateClicked = onViewerStateClicked,
                )
                ProfileStats(
                    modifier = Modifier.fillMaxWidth(),
                    profile = profile,
                    onNavigateToProfiles = onNavigateToProfiles,
                )
                Text(text = profile.description ?: "")
                Spacer(Modifier.height(16.dp))
            }
            ProfileTabs(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = headerState.tabsStartPadding,
                        end = headerState.tabsEndPadding,
                    ),
                headerState = headerState,
                pagerState = pagerState,
                tabs = timelineTabs,
                onRefreshTabClicked = onRefreshTabClicked,
            )
            Spacer(Modifier.height(8.dp))
        }
        ProfileAvatar(
            movableSharedElementScope = movableSharedElementScope,
            modifier = Modifier
                .align(
                    lerp(
                        start = Alignment.TopCenter,
                        stop = Alignment.TopEnd,
                        fraction = headerState.avatarAlignmentLerp,
                    )
                ),
            headerState = headerState,
            profile = profile,
            avatarSharedElementKey = avatarSharedElementKey,
        )
    }
}

@Composable
private fun ProfileBanner(
    modifier: Modifier = Modifier,
    headerState: HeaderState,
    profile: Profile,
) {
    AsyncImage(
        modifier = modifier
            .fillMaxWidth()
            .height(headerState.profileBannerHeight)
            .graphicsLayer {
                alpha = headerState.bannerAlpha
            },
        args = remember(
            key1 = profile.banner?.uri,
            key2 = profile.displayName,
            key3 = profile.handle,
        ) {
            ImageArgs(
                url = profile.banner?.uri,
                contentScale = ContentScale.Crop,
                contentDescription = profile.displayName ?: profile.handle.id,
                shape = RoundedPolygonShape.Rectangle,
            )
        },
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ProfileAvatar(
    modifier: Modifier = Modifier,
    movableSharedElementScope: MovableSharedElementScope,
    headerState: HeaderState,
    profile: Profile,
    avatarSharedElementKey: String,
) {
    val statusBarHeight = UiTokens.statusBarHeight
    Card(
        modifier = modifier
            .padding(top = headerState.avatarTopPadding)
            .size(headerState.avatarSize)
            .offset {
                headerState.avatarOffset(
                    density = this,
                    statusBarHeight = statusBarHeight
                )
            },
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        movableSharedElementScope.updatedMovableSharedElementOf(
            key = avatarSharedElementKey,
            modifier = modifier
                .fillMaxSize()
                .padding(headerState.avatarPadding),
            state = remember(
                key1 = profile.avatar?.uri,
                key2 = profile.displayName,
                key3 = profile.handle,
            ) {
                ImageArgs(
                    url = profile.avatar?.uri,
                    contentScale = ContentScale.Crop,
                    contentDescription = profile.displayName ?: profile.handle.id,
                    shape = RoundedPolygonShape.Circle,
                )
            },
            sharedElement = { state, modifier ->
                AsyncImage(state, modifier)
            }
        )
    }
}


@Composable
private fun ProfileHeadline(
    modifier: Modifier = Modifier,
    profile: Profile,
    isSignedInProfile: Boolean,
    viewerState: ProfileViewerState?,
    onViewerStateClicked: (ProfileViewerState?) -> Unit,
) {
    AttributionLayout(
        modifier = modifier,
        avatar = null,
        label = {
            Column {
                ProfileName(
                    modifier = Modifier,
                    profile = profile,
                    ellipsize = false,
                )
                Spacer(Modifier.height(4.dp))
                ProfileHandle(
                    modifier = Modifier,
                    profile = profile,
                )
            }
        },
        action = {
            AnimatedVisibility(
                visible = viewerState != null || isSignedInProfile,
                content = {
                    ProfileViewerState(
                        viewerState = viewerState,
                        isSignedInProfile = isSignedInProfile,
                        onClick = {
                            onViewerStateClicked(viewerState)
                        }
                    )
                },
            )
        },
    )
}

@Composable
private fun ProfileStats(
    modifier: Modifier = Modifier,
    profile: Profile,
    onNavigateToProfiles: (NavigationAction.Common.ToProfiles.Profile) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        Statistic(
            value = profile.followersCount ?: 0,
            description = stringResource(Res.string.followers),
            onClick = {
                onNavigateToProfiles(
                    NavigationAction.Common.ToProfiles.Profile.Followers(
                        profileId = profile.did,
                    ),
                )
            },
        )
        Statistic(
            value = profile.followsCount ?: 0,
            description = stringResource(Res.string.following),
            onClick = {
                onNavigateToProfiles(
                    NavigationAction.Common.ToProfiles.Profile.Following(
                        profileId = profile.did,
                    ),
                )
            },
        )
        Statistic(
            value = profile.postsCount ?: 0,
            description = stringResource(Res.string.posts),
            onClick = {}
        )
    }
}

@Composable
fun Statistic(
    value: Long,
    description: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            modifier = Modifier,
            text = format(value),
            maxLines = 1,
            style = LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            modifier = Modifier,
            text = description,
            maxLines = 1,
            style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.outline),
        )
    }
}

@Composable
private fun ProfileTabs(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    headerState: HeaderState,
    tabs: List<Tab>,
    onRefreshTabClicked: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    Tabs(
        modifier = modifier
            .clip(CircleShape)
            .offset { headerState.tabsOffset(density = this) },
        tabs = tabs,
        selectedTabIndex = pagerState.tabIndex,
        onTabSelected = {
            scope.launch {
                pagerState.animateScrollToPage(it)
            }
        },
        onTabReselected = onRefreshTabClicked,
    )
}

@Composable
private fun ProfileTimeline(
    panedSharedElementScope: PanedSharedElementScope,
    timelineStateHolder: TimelineStateHolder,
    actions: (Action) -> Unit,
) {
    val gridState = rememberLazyStaggeredGridState()
    val timelineState by timelineStateHolder.state.collectAsStateWithLifecycle()
    val items by rememberUpdatedState(timelineState.items)

    val onPostClicked = remember {
        { post: Post ->
            actions(
                Action.Navigate.DelegateTo(
                    NavigationAction.Common.ToPost(
                        referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                        sharedElementPrefix = timelineState.timeline.sourceId,
                        post = post,
                    )
                )
            )
        }
    }
    val onProfileClicked = remember {
        { post: Post, profile: Profile ->
            actions(
                Action.Navigate.DelegateTo(
                    NavigationAction.Common.ToProfile(
                        referringRouteOption = NavigationAction.ReferringRouteOption.Parent,
                        profile = profile,
                        avatarSharedElementKey = post.avatarSharedElementKey(
                            prefix = timelineState.timeline.sourceId,
                        ).takeIf { post.author.did == profile.did }
                    )
                )
            )
        }
    }
    val onPostMediaClicked = remember {
        { post: Post, media: Embed.Media, index: Int ->
            actions(
                Action.Navigate.DelegateTo(
                    NavigationAction.Common.ToMedia(
                        post = post,
                        media = media,
                        startIndex = index,
                        sharedElementPrefix = timelineState.timeline.sourceId,
                    )
                )
            )
        }
    }

    val onReplyToPost = remember {
        { post: Post ->
            actions(
                Action.Navigate.DelegateTo(
                    NavigationAction.Common.ComposePost(
                        type = Post.Create.Reply(
                            parent = post,
                        ),
                        sharedElementPrefix = timelineState.timeline.sourceId,
                    )
                )
            )
        }
    }

    val density = LocalDensity.current
    val videoStates = remember { ThreadedVideoPositionStates() }

    PullToRefreshBox(
        modifier = Modifier
            .pullToRefresh(
                isRefreshing = timelineState.status is TimelineStatus.Refreshing,
                state = rememberPullToRefreshState(),
                onRefresh = { timelineStateHolder.accept(TimelineLoadAction.Refresh) }
            )
            .fillMaxSize(),
        isRefreshing = timelineState.status is TimelineStatus.Refreshing,
        state = rememberPullToRefreshState(),
        onRefresh = { timelineStateHolder.accept(TimelineLoadAction.Refresh) }
    ) {
        LazyVerticalStaggeredGrid(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    timelineStateHolder.accept(
                        TimelineLoadAction.GridSize(
                            floor(it.width / with(density) { CardSize.toPx() }).roundToInt()
                        )
                    )
                },
            state = gridState,
            columns = StaggeredGridCells.Adaptive(CardSize),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        panedSharedElementScope = panedSharedElementScope,
                        now = remember { Clock.System.now() },
                        item = item,
                        sharedElementPrefix = timelineState.timeline.sourceId,
                        onPostClicked = onPostClicked,
                        onProfileClicked = onProfileClicked,
                        onPostMediaClicked = onPostMediaClicked,
                        onReplyToPost = onReplyToPost,
                        onPostInteraction = {
                            actions(
                                Action.SendPostInteraction(it)
                            )
                        },
                    )
                }
            )
        }
    }

    if (panedSharedElementScope.paneState.pane == ThreePane.Primary) {
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
                TimelineLoadAction.LoadAround(query ?: timelineState.currentQuery)
            )
        }
    )

    gridState.TimelineRefreshEffect(
        timelineState = timelineState,
        onRefresh = { animateScrollToItem(index = 0) }
    )
}

@Stable
private class HeaderState(
    val headerState: CollapsingHeaderState,
) {
    var width by mutableStateOf(160.dp * 3)

    val bioTopPadding get() = profileBannerHeight - sizeToken
    val bioAlpha get() = 1f - headerState.progress

    val profileBannerHeight by derivedStateOf { width * (9 / 16f) }
    val bannerAlpha get() = 1f - min(0.9f, (headerState.progress * 1.6f))

    val avatarTopPadding get() = bioTopPadding - (ExpandedProfilePhotoSize / 2)
    val avatarSize get() = ExpandedProfilePhotoSize - (expandedToCollapsedAvatar * progress)
    val avatarPadding get() = 4.dp * (1f - progress)
    val avatarAlignmentLerp get() = progress
    val tabsStartPadding get() = sizeToken
    val tabsEndPadding get() = sizeToken + (CollapsedProfilePhotoSize * progress)

    fun bioOffset() = IntOffset(
        x = 0,
        y = -headerState.translation.roundToInt()
    )

    fun avatarOffset(
        density: Density,
        statusBarHeight: Dp,
    ) = with(density) {
        IntOffset(
            x = -(16.dp * progress).roundToPx(),
            y = -((topToAnchoredCollapsedAvatar - statusBarHeight) * progress).roundToPx()
        )
    }

    fun tabsOffset(
        density: Density,
    ) = with(density) {
        IntOffset(
            x = (headerState.progress * UiTokens.tabsHeight.toPx()).roundToInt(),
            y = 0,
        )
    }

    val sizeToken = 24.dp

    private val progress get() = headerState.progress

    private val screenTopToAvatarTop get() = bioTopPadding - (ExpandedProfilePhotoSize / 2)

    private val screenTopToCollapsedAvatarAppBarCenter
        get() = (UiTokens.toolbarHeight - CollapsedProfilePhotoSize) / 2

    private val topToAnchoredCollapsedAvatar
        get() = screenTopToAvatarTop - screenTopToCollapsedAvatarAppBarCenter

    private val expandedToCollapsedAvatar
        get() = ExpandedProfilePhotoSize - CollapsedProfilePhotoSize
}

private val ExpandedProfilePhotoSize = 68.dp
private val CollapsedProfilePhotoSize = 36.dp
private val CardSize = 340.dp
