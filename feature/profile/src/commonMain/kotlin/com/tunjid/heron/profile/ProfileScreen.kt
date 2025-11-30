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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.composables.collapsingheader.CollapsingHeaderLayout
import com.tunjid.composables.collapsingheader.CollapsingHeaderState
import com.tunjid.composables.collapsingheader.rememberCollapsingHeaderState
import com.tunjid.composables.lazy.pendingScrollOffsetState
import com.tunjid.composables.ui.lerp
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.ProfileUri.Companion.asSelfLabelerUri
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.utilities.asGenericUri
import com.tunjid.heron.data.utilities.path
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.profile.ui.LabelerSettings
import com.tunjid.heron.profile.ui.LabelerState
import com.tunjid.heron.profile.ui.ProfileCollectionSharedElementPrefix
import com.tunjid.heron.profile.ui.RecordList
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.composePostDestination
import com.tunjid.heron.scaffold.navigation.conversationDestination
import com.tunjid.heron.scaffold.navigation.editProfileDestination
import com.tunjid.heron.scaffold.navigation.galleryDestination
import com.tunjid.heron.scaffold.navigation.pathDestination
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.navigation.profileFollowersDestination
import com.tunjid.heron.scaffold.navigation.profileFollowsDestination
import com.tunjid.heron.scaffold.navigation.recordDestination
import com.tunjid.heron.scaffold.navigation.signInDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.SignInPopUpState.Companion.rememberSignInPopUpState
import com.tunjid.heron.scaffold.scaffold.paneClip
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.state.TimelineStateHolder
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.TimelinePresentationSelector
import com.tunjid.heron.timeline.ui.effects.TimelineRefreshEffect
import com.tunjid.heron.timeline.ui.feed.FeedGenerator
import com.tunjid.heron.timeline.ui.list.FeedList
import com.tunjid.heron.timeline.ui.list.StarterPack
import com.tunjid.heron.timeline.ui.post.PostInteractionsSheetState.Companion.rememberUpdatedPostInteractionState
import com.tunjid.heron.timeline.ui.post.PostOption
import com.tunjid.heron.timeline.ui.post.PostOptionsSheetState.Companion.rememberUpdatedPostOptionsState
import com.tunjid.heron.timeline.ui.post.ThreadGateSheetState.Companion.rememberThreadGateSheetState
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.timeline.ui.postActions
import com.tunjid.heron.timeline.ui.profile.ProfileHandle
import com.tunjid.heron.timeline.ui.profile.ProfileName
import com.tunjid.heron.timeline.ui.profile.ProfileViewerState
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.canAutoPlayVideo
import com.tunjid.heron.timeline.utilities.cardSize
import com.tunjid.heron.timeline.utilities.collectionShape
import com.tunjid.heron.timeline.utilities.displayName
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.timeline.utilities.lazyGridHorizontalItemSpacing
import com.tunjid.heron.timeline.utilities.lazyGridVerticalItemSpacing
import com.tunjid.heron.timeline.utilities.orDefault
import com.tunjid.heron.timeline.utilities.pendingOffsetFor
import com.tunjid.heron.timeline.utilities.sharedElementPrefix
import com.tunjid.heron.timeline.utilities.timelineHorizontalPadding
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.OverlappingAvatarRow
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.modifiers.blur
import com.tunjid.heron.ui.navigableLinkTargetHandler
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.tabIndex
import com.tunjid.heron.ui.text.links
import com.tunjid.heron.ui.text.rememberFormattedTextPost
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableStickySharedElementOf
import com.tunjid.treenav.compose.threepane.ThreePane
import heron.feature.profile.generated.resources.Res
import heron.feature.profile.generated.resources.followed_by_others
import heron.feature.profile.generated.resources.followed_by_profiles
import heron.feature.profile.generated.resources.followers
import heron.feature.profile.generated.resources.following
import heron.feature.profile.generated.resources.follows_you
import heron.feature.profile.generated.resources.labels
import heron.feature.profile.generated.resources.posts
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProfileScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    val signInPopUpState = rememberSignInPopUpState {
        actions(Action.Navigate.To(signInDestination()))
    }

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
    val updatedStateHolders by rememberUpdatedState(state.stateHolders)

    val pagerState = rememberPagerState {
        updatedStateHolders.size
    }
    val pullToRefreshState = rememberPullToRefreshState()

    val isRefreshing by produceState(
        initialValue = false,
        key1 = pagerState.currentPage,
        key2 = updatedStateHolders.size,
    ) {
        updatedStateHolders
            .getOrNull(pagerState.currentPage)
            .isRefreshing
            .collect { value = it }
    }

    CollapsingHeaderLayout(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                headerState.width = with(density) { it.width.toDp() }
            }
            .pullToRefresh(
                enabled = updatedStateHolders
                    .getOrNull(pagerState.currentPage)
                    .canRefresh,
                isRefreshing = isRefreshing,
                state = pullToRefreshState,
                onRefresh = {
                    updatedStateHolders
                        .getOrNull(pagerState.currentPage)
                        ?.refresh()
                },
            ),
        state = headerState.headerState,
        headerContent = {
            ProfileHeader(
                paneScaffoldState = paneScaffoldState,
                pullToRefreshState = pullToRefreshState,
                headerState = headerState,
                pagerState = pagerState,
                timelineTabs = timelineTabs(
                    updatedStateHolders = updatedStateHolders,
                    sourceIdsToHasUpdates = state.sourceIdsToHasUpdates,
                ),
                modifier = Modifier
                    .fillMaxWidth(),
                profile = state.profile,
                commonFollowerCount = state.viewerState?.commonFollowersCount,
                commonFollowers = state.commonFollowers,
                isRefreshing = isRefreshing,
                isSignedInProfile = state.isSignedInProfile,
                isSubscribedToLabeler = state.isSubscribedToLabeler,
                viewerState = state.viewerState,
                timelineStateHolders = remember(updatedStateHolders) {
                    updatedStateHolders.filterIsInstance<ProfileScreenStateHolders.Timeline>()
                },
                avatarSharedElementKey = state.avatarSharedElementKey,
                onRefreshTabClicked = { index ->
                    updatedStateHolders.getOrNull(index = index)
                        ?.refresh()
                },
                onViewerStateClicked = { viewerState ->
                    state.signedInProfileId?.let {
                        actions(
                            Action.ToggleViewerState(
                                signedInProfileId = it,
                                viewedProfileId = state.profile.did,
                                following = viewerState?.following,
                                followedBy = viewerState?.followedBy,
                            ),
                        )
                    }
                },
                onNavigate = { destination ->
                    actions(Action.Navigate.To(destination))
                },
                onProfileAvatarClicked = {
                    actions(
                        Action.Navigate.ToAvatar(
                            profile = state.profile,
                            avatarSharedElementKey = state.avatarSharedElementKey,
                        ),
                    )
                },
                onLinkTargetClicked = navigableLinkTargetHandler { navigable ->
                    actions(
                        Action.Navigate.To(
                            pathDestination(
                                path = navigable.path,
                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                            ),
                        ),
                    )
                },
                onEditClick = {
                    actions(
                        Action.Navigate.To(
                            editProfileDestination(
                                profile = state.profile,
                                avatarSharedElementKey = state.avatarSharedElementKey,
                            ),
                        ),
                    )
                },
                onToggleLabelerSubscription = { labelerProfileId, isSubscribed ->
                    actions(
                        Action.UpdatePreferences(
                            Timeline.Update.OfLabeler.Subscription(
                                labelCreatorId = labelerProfileId,
                                subscribed = !isSubscribed,
                            ),
                        ),
                    )
                },
            )
        },
        body = {
            Box(
                modifier = Modifier,
            ) {
                HorizontalPager(
                    modifier = Modifier
                        .paneClip(),
                    state = pagerState,
                    key = { page -> updatedStateHolders[page].key },
                    pageContent = { page ->
                        when (val stateHolder = updatedStateHolders[page]) {
                            is ProfileScreenStateHolders.Records.Feeds -> RecordList(
                                collectionStateHolder = stateHolder,
                                itemKey = { it.cid.id },
                                itemContent = { feedGenerator ->
                                    FeedGenerator(
                                        modifier = Modifier
                                            .fillParentMaxWidth()
                                            .clip(RecordShape)
                                            .animateItem()
                                            .clickable {
                                                actions(
                                                    Action.Navigate.To(
                                                        pathDestination(
                                                            path = feedGenerator.uri.path,
                                                            models = listOf(feedGenerator),
                                                            sharedElementPrefix = ProfileCollectionSharedElementPrefix,
                                                            referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                        ),
                                                    ),
                                                )
                                            }
                                            .recordPadding(),
                                        movableElementSharedTransitionScope = paneScaffoldState,
                                        sharedElementPrefix = ProfileCollectionSharedElementPrefix,
                                        feedGenerator = feedGenerator,
                                        status = state.timelineRecordUrisToPinnedStatus.status(feedGenerator.uri),
                                        onFeedGeneratorStatusUpdated = { update ->
                                            if (paneScaffoldState.isSignedOut) signInPopUpState.show()
                                            else actions(Action.UpdatePreferences(update))
                                        },
                                    )
                                },
                            )

                            is ProfileScreenStateHolders.Records.StarterPacks -> RecordList(
                                collectionStateHolder = stateHolder,
                                itemKey = { it.cid.id },
                                itemContent = { starterPack ->
                                    StarterPack(
                                        modifier = Modifier
                                            .fillParentMaxWidth()
                                            .clip(RecordShape)
                                            .animateItem()
                                            .clickable {
                                                actions(
                                                    Action.Navigate.To(
                                                        pathDestination(
                                                            path = starterPack.uri.path,
                                                            models = listOf(starterPack),
                                                            sharedElementPrefix = ProfileCollectionSharedElementPrefix,
                                                            referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                        ),
                                                    ),
                                                )
                                            }
                                            .recordPadding(),
                                        movableElementSharedTransitionScope = paneScaffoldState,
                                        sharedElementPrefix = ProfileCollectionSharedElementPrefix,
                                        starterPack = starterPack,
                                    )
                                },
                            )

                            is ProfileScreenStateHolders.Records.Lists -> RecordList(
                                collectionStateHolder = stateHolder,
                                itemKey = { it.cid.id },
                                itemContent = { list ->
                                    FeedList(
                                        modifier = Modifier
                                            .fillParentMaxWidth()
                                            .clip(RecordShape)
                                            .animateItem()
                                            .clickable {
                                                actions(
                                                    Action.Navigate.To(
                                                        pathDestination(
                                                            path = list.uri.path,
                                                            models = listOf(list),
                                                            sharedElementPrefix = ProfileCollectionSharedElementPrefix,
                                                            referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                        ),
                                                    ),
                                                )
                                            }
                                            .recordPadding(),
                                        movableElementSharedTransitionScope = paneScaffoldState,
                                        sharedElementPrefix = ProfileCollectionSharedElementPrefix,
                                        list = list,
                                        status = state.timelineRecordUrisToPinnedStatus.status(list.uri),
                                        onListStatusUpdated = { update ->
                                            if (paneScaffoldState.isSignedOut) signInPopUpState.show()
                                            else actions(Action.UpdatePreferences(update))
                                        },
                                    )
                                },
                            )

                            is ProfileScreenStateHolders.Timeline -> ProfileTimeline(
                                signedInProfileId = state.signedInProfileId,
                                paneScaffoldState = paneScaffoldState,
                                timelineStateHolder = stateHolder,
                                actions = actions,
                                recentConversations = state.recentConversations,
                            )
                            is ProfileScreenStateHolders.LabelerSettings -> LabelerSettings(
                                stateHolder = stateHolder,
                            )
                        }
                    },
                )
            }
        },
    )
}

@Composable
private fun timelineTabs(
    updatedStateHolders: List<ProfileScreenStateHolders>,
    sourceIdsToHasUpdates: Map<String, Boolean>,
): List<Tab> = updatedStateHolders.map { holder ->
    when (holder) {
        is ProfileScreenStateHolders.Records<*> -> Tab(
            title = stringResource(remember(holder.state.value::stringResource)),
            hasUpdate = false,
        )

        is ProfileScreenStateHolders.Timeline -> Tab(
            title = holder.state.value.timeline.displayName(),
            hasUpdate = sourceIdsToHasUpdates[holder.state.value.timeline.sourceId] == true,
        )
        is ProfileScreenStateHolders.LabelerSettings -> Tab(
            title = stringResource(Res.string.labels),
            hasUpdate = false,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ProfileHeader(
    paneScaffoldState: PaneScaffoldState,
    pullToRefreshState: PullToRefreshState,
    headerState: HeaderState,
    pagerState: PagerState,
    timelineTabs: List<Tab>,
    modifier: Modifier = Modifier,
    profile: Profile,
    commonFollowerCount: Long?,
    commonFollowers: List<Profile>,
    isRefreshing: Boolean,
    isSignedInProfile: Boolean,
    isSubscribedToLabeler: Boolean,
    viewerState: ProfileViewerState?,
    timelineStateHolders: List<ProfileScreenStateHolders.Timeline>,
    avatarSharedElementKey: String,
    onRefreshTabClicked: (Int) -> Unit,
    onViewerStateClicked: (ProfileViewerState?) -> Unit,
    onNavigate: (NavigationAction.Destination) -> Unit,
    onProfileAvatarClicked: () -> Unit,
    onLinkTargetClicked: (LinkTarget) -> Unit,
    onEditClick: () -> Unit,
    onToggleLabelerSubscription: (ProfileId, Boolean) -> Unit,
) = with(paneScaffoldState) {
    Box(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        ProfileBanner(
            modifier = Modifier
                .align(Alignment.TopCenter),
            paneScaffoldState = paneScaffoldState,
            headerState = headerState,
            profile = profile,
            avatarSharedElementKey = avatarSharedElementKey,
        )
        val surfaceColor = MaterialTheme.colorScheme.surface
        Box(
            modifier = Modifier
                .offset {
                    headerState.bioOffset()
                }
                .padding(top = headerState.bioTopPadding)
                .paneStickySharedElement(
                    sharedContentState = rememberSharedContentState(
                        key = avatarSharedElementKey.withProfileBioTabSharedElementPrefix(),
                    ),
                    zIndexInOverlay = SurfaceZIndex,
                )
                .profileBioTabBackground {
                    surfaceColor.copy(alpha = headerState.bioAlpha)
                },
        )
        Column(
            modifier = Modifier
                .padding(top = headerState.bioTopPadding)
                .offset {
                    headerState.bioOffset()
                },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .drawBehind {
                        drawRect(
                            color = surfaceColor.copy(alpha = headerState.bioAlpha),
                            topLeft = Offset(x = 0f, y = ProfileBioTabHeight.toPx()),
                        )
                    }
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        alpha = headerState.bioAlpha
                    },
            ) {
                Spacer(Modifier.height(24.dp))
                ProfileHeadline(
                    modifier = Modifier.fillMaxWidth(),
                    profile = profile,
                    isSignedInProfile = isSignedInProfile,
                    isSubscribedToLabeler = isSubscribedToLabeler,
                    viewerState = viewerState,
                    onViewerStateClicked = onViewerStateClicked,
                    onEditClick = onEditClick,
                    onToggleLabelerSubscription = onToggleLabelerSubscription,
                )
                ProfileStats(
                    modifier = Modifier.fillMaxWidth(),
                    profile = profile,
                    followsSignInProfile = viewerState?.followedBy != null,
                    onNavigateToProfiles = onNavigate,
                )
                ProfileBio(
                    description = profile.description ?: "",
                    onLinkTargetClicked = onLinkTargetClicked,
                )
                if (!isSignedInProfile && commonFollowers.isNotEmpty()) {
                    Spacer(Modifier.height(Dp.Hairline))
                    CommonFollowers(
                        commonFollowerCount = commonFollowerCount,
                        commonFollowers = commonFollowers,
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
            ProfileTabs(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = headerState.tabsHorizontalPadding,
                    ),
                pagerState = pagerState,
                tabs = timelineTabs,
                timelineStateHolders = timelineStateHolders,
                onRefreshTabClicked = onRefreshTabClicked,
            )
            Spacer(Modifier.height(8.dp))
        }
        ProfileAvatar(
            paneScaffoldState = paneScaffoldState,
            pullToRefreshState = pullToRefreshState,
            modifier = Modifier
                .align(
                    lerp(
                        start = Alignment.TopCenter,
                        stop = Alignment.TopEnd,
                        fraction = headerState.avatarAlignmentLerp,
                    ),
                ),
            headerState = headerState,
            isRefreshing = isRefreshing,
            profile = profile,
            avatarSharedElementKey = avatarSharedElementKey,
            onProfileAvatarClicked = onProfileAvatarClicked,
        )
    }
}

@Composable
private fun ProfileBio(
    description: String?,
    onLinkTargetClicked: (LinkTarget) -> Unit,
) {
    val bio = description.orEmpty()
    val textLinks = AnnotatedString(bio).links()

    val annotatedText = rememberFormattedTextPost(
        text = bio,
        textLinks = textLinks,
        onLinkTargetClicked = onLinkTargetClicked,
    )

    Text(
        text = annotatedText,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ProfileBanner(
    modifier: Modifier = Modifier,
    paneScaffoldState: PaneScaffoldState,
    headerState: HeaderState,
    profile: Profile,
    avatarSharedElementKey: String,
) = with(paneScaffoldState) {
    paneScaffoldState.updatedMovableStickySharedElementOf(
        sharedContentState = rememberSharedContentState(
            key = avatarSharedElementKey.withProfileBannerSharedElementPrefix(),
        ),
        zIndexInOverlay = BannerZIndex,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(BannerAspectRatio)
            .graphicsLayer {
                alpha = headerState.bannerAlpha
            }
            .blur(
                shape = RectangleShape,
                radius = ::BannerBlurRadius,
                progress = headerState::progress,
            ),
        state = remember(
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
        sharedElement = { state, modifier ->
            AsyncImage(state, modifier)
        },
    )
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileAvatar(
    modifier: Modifier = Modifier,
    paneScaffoldState: PaneScaffoldState,
    headerState: HeaderState,
    pullToRefreshState: PullToRefreshState,
    isRefreshing: Boolean,
    profile: Profile,
    avatarSharedElementKey: String,
    onProfileAvatarClicked: () -> Unit,
) = with(paneScaffoldState) {
    val statusBarHeight = UiTokens.statusBarHeight
    Box(
        modifier = modifier
            .padding(top = headerState.avatarTopPadding)
            .size(headerState.avatarSize + 2.dp)
            .offset {
                headerState.avatarOffset(
                    density = this,
                    statusBarHeight = statusBarHeight,
                )
            },
    ) {
        val showWave = isRefreshing || pullToRefreshState.distanceFraction >= 1f
        val scale = animateFloatAsState(
            if (showWave) 1.2f else 1f,
        )
        CircularWavyProgressIndicator(
            progress = { if (isRefreshing) 1f else pullToRefreshState.distanceFraction },
            trackColor = MaterialTheme.colorScheme.surface,
            amplitude = { if (showWave) 1f else 0f },
            modifier = Modifier
                .paneStickySharedElement(
                    sharedContentState = rememberSharedContentState(
                        key = avatarSharedElementKey.withProfileAvatarHaloSharedElementPrefix(),
                    ),
                    zIndexInOverlay = AvatarHaloZIndex,
                )
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
        paneScaffoldState.updatedMovableStickySharedElementOf(
            sharedContentState = with(paneScaffoldState) {
                rememberSharedContentState(
                    key = avatarSharedElementKey,
                )
            },
            zIndexInOverlay = AvatarZIndex,
            modifier = modifier
                .fillMaxSize()
                .padding(headerState.avatarPadding)
                .clickable { onProfileAvatarClicked() },
            state = remember(
                key1 = profile.avatar?.uri,
                key2 = profile.displayName ?: profile.handle,
                key3 = profile.isLabeler,
            ) {
                ImageArgs(
                    url = profile.avatar.orDefault.uri,
                    contentScale = ContentScale.Crop,
                    contentDescription = profile.displayName ?: profile.handle.id,
                    shape =
                    if (profile.isLabeler) profile.did.asSelfLabelerUri().collectionShape()
                    else RoundedPolygonShape.Circle,
                )
            },
            sharedElement = { state, modifier ->
                AsyncImage(state, modifier)
            },
        )
    }
}

@Composable
private fun ProfileHeadline(
    modifier: Modifier = Modifier,
    profile: Profile,
    isSignedInProfile: Boolean,
    isSubscribedToLabeler: Boolean,
    viewerState: ProfileViewerState?,
    onEditClick: () -> Unit,
    onViewerStateClicked: (ProfileViewerState?) -> Unit,
    onToggleLabelerSubscription: (ProfileId, Boolean) -> Unit,
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
            val profileId = profile.did
            AnimatedVisibility(
                visible = viewerState != null || isSignedInProfile || profile.isLabeler,
                content = {
                    if (profile.isLabeler) LabelerState(
                        isSubscribed = isSubscribedToLabeler,
                        onClick = {
                            onToggleLabelerSubscription(
                                profileId,
                                isSubscribedToLabeler,
                            )
                        },
                    )
                    else ProfileViewerState(
                        viewerState = viewerState,
                        isSignedInProfile = isSignedInProfile,
                        onClick = {
                            if (isSignedInProfile) onEditClick()
                            else onViewerStateClicked(viewerState)
                        },
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
    followsSignInProfile: Boolean,
    onNavigateToProfiles: (NavigationAction.Destination) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Statistic(
            value = profile.followersCount ?: 0,
            description = stringResource(Res.string.followers),
            onClick = {
                onNavigateToProfiles(
                    profileFollowersDestination(
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
                    profileFollowsDestination(
                        profileId = profile.did,
                    ),
                )
            },
        )
        Statistic(
            value = profile.postsCount ?: 0,
            description = stringResource(Res.string.posts),
            onClick = {},
        )
        Box(
            Modifier
                .weight(1f),
        ) {
            if (followsSignInProfile) Text(
                modifier = Modifier
                    .align(Alignment.BottomEnd),
                text = stringResource(Res.string.follows_you),
                maxLines = 2,
                style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.outline),
            )
        }
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
private fun CommonFollowers(
    commonFollowerCount: Long?,
    commonFollowers: List<Profile>,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OverlappingAvatarRow(
            modifier = Modifier
                .width(20.dp * commonFollowers.size),
            overlap = 16.dp,
            maxItems = commonFollowers.size,
            content = {
                commonFollowers.forEachIndexed { index, profile ->
                    AsyncImage(
                        modifier = Modifier
                            .zIndex(-index.toFloat()),
                        args = remember(profile.avatar) {
                            ImageArgs(
                                url = profile.avatar?.uri,
                                contentScale = ContentScale.Crop,
                                contentDescription = profile.displayName
                                    ?: profile.handle.id,
                                shape = RoundedPolygonShape.Circle,
                            )
                        },
                    )
                }
            },
        )
        Text(
            text = stringResource(
                Res.string.followed_by_profiles,
                when (val size = commonFollowers.size) {
                    1 -> commonFollowers.first().displayName ?: ""
                    2 -> commonFollowers.joinToString(
                        separator = ", ",
                        transform = { it.displayName ?: "" },
                    )

                    else -> commonFollowers.take(2).joinToString(
                        separator = ", ",
                        transform = { it.displayName ?: "" },
                        postfix = stringResource(
                            Res.string.followed_by_others,
                            (commonFollowerCount?.toInt() ?: size) - 2,
                        ),
                    )
                },
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ProfileTabs(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    tabs: List<Tab>,
    timelineStateHolders: List<ProfileScreenStateHolders.Timeline>,
    onRefreshTabClicked: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = modifier
            .clip(CircleShape),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tabs(
            modifier = Modifier
                .animateContentSize()
                .weight(1f)
                .clip(CircleShape),
            tabsState = rememberTabsState(
                tabs = tabs,
                selectedTabIndex = pagerState::tabIndex,
                onTabSelected = {
                    scope.launch {
                        pagerState.animateScrollToPage(it)
                    }
                },
                onTabReselected = onRefreshTabClicked,
            ),
        )
        TimelinePresentationSelector(
            page = pagerState.currentPage,
            timelineStateHolders = timelineStateHolders,
        )
    }
}

@Composable
private fun ProfileTimeline(
    signedInProfileId: ProfileId?,
    paneScaffoldState: PaneScaffoldState,
    timelineStateHolder: TimelineStateHolder,
    actions: (Action) -> Unit,
    recentConversations: List<Conversation>,
) {
    val gridState = rememberLazyStaggeredGridState()
    val timelineState by timelineStateHolder.state.collectAsStateWithLifecycle()
    val items by rememberUpdatedState(timelineState.tiledItems)
    val pendingScrollOffsetState = gridState.pendingScrollOffsetState()

    val density = LocalDensity.current
    val videoStates = remember { ThreadedVideoPositionStates(TimelineItem::id) }
    val presentation = timelineState.timeline.presentation
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
                        sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
                    ),
                ),
            )
        },
    )
    val threadGateSheetState = rememberThreadGateSheetState(
        onThreadGateUpdated = {
            actions(Action.UpdateThreadGate(it))
        },
    )
    val postOptionsState = rememberUpdatedPostOptionsState(
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
                .onSizeChanged {
                    val itemWidth = with(density) {
                        presentation.cardSize.toPx()
                    }
                    timelineStateHolder.accept(
                        TimelineState.Action.Tile(
                            tilingAction = TilingState.Action.GridSize(
                                numColumns = floor(it.width / itemWidth).roundToInt(),
                            ),
                        ),
                    )
                },
            state = gridState,
            columns = StaggeredGridCells.Adaptive(presentation.cardSize),
            contentPadding = UiTokens.bottomNavAndInsetPaddingValues(),
            verticalItemSpacing = presentation.lazyGridVerticalItemSpacing,
            horizontalArrangement = Arrangement.spacedBy(
                presentation.lazyGridHorizontalItemSpacing,
            ),
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
                            postActions(
                                onLinkTargetClicked = { _, linkTarget ->
                                    if (linkTarget is LinkTarget.Navigable) actions(
                                        Action.Navigate.To(
                                            pathDestination(
                                                path = linkTarget.path,
                                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                            ),
                                        ),
                                    )
                                },
                                onPostClicked = { post: Post ->
                                    pendingScrollOffsetState.value =
                                        gridState.pendingOffsetFor(item)
                                    actions(
                                        Action.Navigate.To(
                                            recordDestination(
                                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                sharedElementPrefix = timelineState.timeline.sharedElementPrefix,
                                                record = post,
                                            ),
                                        ),
                                    )
                                },
                                onProfileClicked = { profile: Profile, post: Post, quotingPostUri: PostUri? ->
                                    pendingScrollOffsetState.value =
                                        gridState.pendingOffsetFor(item)
                                    actions(
                                        Action.Navigate.To(
                                            profileDestination(
                                                referringRouteOption = NavigationAction.ReferringRouteOption.Parent,
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
                                },
                                onPostRecordClicked = { record, owningPostUri ->
                                    pendingScrollOffsetState.value =
                                        gridState.pendingOffsetFor(item)
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
                                },
                                onPostMediaClicked = { media: Embed.Media, index: Int, post: Post, quotingPostUri: PostUri? ->
                                    pendingScrollOffsetState.value =
                                        gridState.pendingOffsetFor(item)
                                    actions(
                                        Action.Navigate.To(
                                            galleryDestination(
                                                post = post,
                                                media = media,
                                                startIndex = index,
                                                sharedElementPrefix = timelineState.timeline.sharedElementPrefix(
                                                    quotingPostUri = quotingPostUri,
                                                ),
                                            ),
                                        ),
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
                                            ),
                                        ),
                                    )
                                },
                                onPostInteraction = postInteractionState::onInteraction,
                                onPostOptionsClicked = postOptionsState::showOptions,
                            )
                        },
                    )
                },
            )
        }
    }

    if (paneScaffoldState.paneState.pane == ThreePane.Primary) {
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
                        query = query ?: timelineState.tilingData.currentQuery,
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

@Composable
private fun TimelinePresentationSelector(
    modifier: Modifier = Modifier,
    page: Int,
    timelineStateHolders: List<ProfileScreenStateHolders.Timeline>,
) {
    val timeline = produceState(
        initialValue = timelineStateHolders.getOrNull(page)?.state?.value?.timeline,
        key1 = page,
        key2 = timelineStateHolders,
    ) {
        val holder = timelineStateHolders.getOrNull(page) ?: return@produceState
        value = holder.state.value.timeline
        holder.state.collect {
            value = it.timeline
        }
    }.value

    if (timeline != null) TimelinePresentationSelector(
        modifier = modifier,
        selected = timeline.presentation,
        available = timeline.supportedPresentations,
        onPresentationSelected = { presentation ->
            timelineStateHolders.getOrNull(page)
                ?.accept
                ?.invoke(
                    TimelineState.Action.UpdatePreferredPresentation(
                        timeline = timeline,
                        presentation = presentation,
                    ),
                )
        },
    )
}

private fun Map<RecordUri?, Boolean>.status(
    recordUri: RecordUri,
) = when (this[recordUri]) {
    true -> Timeline.Home.Status.Pinned
    false -> Timeline.Home.Status.Saved
    null -> Timeline.Home.Status.None
}

@Stable
private class HeaderState(
    val headerState: CollapsingHeaderState,
) {
    var width by mutableStateOf(160.dp * 3)
    private val profileBannerHeight by derivedStateOf { width / BannerAspectRatio }

    val bioTopPadding get() = profileBannerHeight - sizeToken
    val bioAlpha get() = 1f - headerState.progress

    val bannerAlpha get() = 1f - min(0.9f, (headerState.progress * 1.6f))

    val avatarTopPadding get() = bioTopPadding - (ExpandedProfilePhotoSize / 2)
    val avatarSize get() = ExpandedProfilePhotoSize - (expandedToCollapsedAvatar * progress)
    val avatarPadding get() = 4.dp * max(0f, 1f - progress)
    val avatarAlignmentLerp get() = progress
    val tabsHorizontalPadding get() = sizeToken + (CollapsedProfilePhotoSize * progress)

    fun bioOffset() = IntOffset(
        x = 0,
        y = -headerState.translation.roundToInt(),
    )

    fun avatarOffset(
        density: Density,
        statusBarHeight: Dp,
    ) = with(density) {
        IntOffset(
            x = -(16.dp * progress).roundToPx(),
            y = -((topToAnchoredCollapsedAvatar - statusBarHeight) * progress).roundToPx(),
        )
    }

    val sizeToken = 24.dp

    val progress get() = max(0f, headerState.progress)

    private val screenTopToAvatarTop get() = bioTopPadding - (ExpandedProfilePhotoSize / 2)

    private val screenTopToCollapsedAvatarAppBarCenter
        get() = (UiTokens.toolbarHeight - CollapsedProfilePhotoSize) / 2

    private val topToAnchoredCollapsedAvatar
        get() = screenTopToAvatarTop - screenTopToCollapsedAvatarAppBarCenter

    private val expandedToCollapsedAvatar
        get() = ExpandedProfilePhotoSize - CollapsedProfilePhotoSize
}

private val RecordShape = RoundedCornerShape(8.dp)

private fun Modifier.recordPadding() =
    padding(8.dp)

private val ExpandedProfilePhotoSize = 68.dp
private val CollapsedProfilePhotoSize = 36.dp
private val BannerBlurRadius = 40.dp
