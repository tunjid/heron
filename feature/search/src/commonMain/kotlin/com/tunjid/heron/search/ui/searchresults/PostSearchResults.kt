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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.utilities.asGenericUri
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.search.SearchState
import com.tunjid.heron.search.id
import com.tunjid.heron.search.sharedElementPrefix
import com.tunjid.heron.sheets.postoptions.PostOption
import com.tunjid.heron.sheets.profile.ProfileRestrictionDialogState.Companion.rememberProfileRestrictionDialogState
import com.tunjid.heron.sheets.rememberMutedWordsSheetState
import com.tunjid.heron.sheets.rememberPostInteractionsSheetState
import com.tunjid.heron.sheets.rememberPostOptionsSheetState
import com.tunjid.heron.sheets.rememberTimelineThreadGateSheetState
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.timeline.ui.PostActions
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.withQuotingPostUriPrefix
import com.tunjid.heron.timeline.utilities.onDominantVideoChange
import com.tunjid.heron.timeline.utilities.rememberTimelineDisplayState
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.UiTokens.bottomNavAndInsetPaddingValues
import com.tunjid.heron.ui.roundedMaxDelta
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.conversationDestination
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.threepane.ThreePane
import kotlin.time.Clock

@Composable
internal fun PostSearchResults(
    state: SearchState.OfPosts,
    gridState: LazyStaggeredGridState,
    modifier: Modifier,
    presentation: Timeline.Presentation,
    autoPlayTimelineVideos: Boolean,
    isActivePage: () -> Boolean,
    showEngagementMetrics: Boolean,
    paneScaffoldState: PaneScaffoldState,
    onLinkTargetClicked: (LinkTarget) -> Unit,
    onPostSearchResultProfileClicked: (profile: Profile, post: Post, sharedElementPrefix: String) -> Unit,
    onPostSearchResultClicked: (post: Post, sharedElementPrefix: String) -> Unit,
    onReplyToPost: (post: Post, sharedElementPrefix: String) -> Unit,
    onPostRecordClicked: (record: Record, sharedElementPrefix: String) -> Unit,
    onPublicationSubscriptionToggled: (StandardPublication) -> Unit,
    onMediaClicked: (media: Embed.Media, index: Int, post: Post, sharedElementPrefix: String) -> Unit,
    onNavigate: (NavigationAction.Destination) -> Unit,
    searchResultActions: (SearchState.Tile) -> Unit,
    onMuteAccountClicked: (signedInProfileId: ProfileId, profileId: ProfileId) -> Unit,
    onBlockAccountClicked: (signedInProfileId: ProfileId, profileId: ProfileId) -> Unit,
    onDeletePostClicked: (RecordUri) -> Unit,
) {
    val now = remember { Clock.System.now() }
    val displayState = rememberTimelineDisplayState()
    val videoPlayerController = LocalVideoPlayerController.current
    val results by rememberUpdatedState(state.tiledItems)
    val sharedElementPrefix = state.sharedElementPrefix
    val postInteractionSheetState = paneScaffoldState.rememberPostInteractionsSheetState(
        sharedElementPrefix = null,
    )
    val threadGateSheetState = paneScaffoldState.rememberTimelineThreadGateSheetState()
    val mutedWordsSheetState = paneScaffoldState.rememberMutedWordsSheetState()

    val profileRestrictionDialogState = rememberProfileRestrictionDialogState(
        onProfileRestricted = { profileRestriction ->
            when (profileRestriction) {
                is PostOption.Moderation.BlockAccount ->
                    onBlockAccountClicked(
                        profileRestriction.signedInProfileId,
                        profileRestriction.post.author.did,
                    )

                is PostOption.Moderation.MuteAccount ->
                    onMuteAccountClicked(
                        profileRestriction.signedInProfileId,
                        profileRestriction.post.author.did,
                    )
            }
        },
    )
    val postOptionsSheetState = paneScaffoldState.rememberPostOptionsSheetState(
        onOptionClicked = { option ->
            when (option) {
                is PostOption.ShareInConversation ->
                    onNavigate(
                        conversationDestination(
                            id = option.conversation.id,
                            members = option.conversation.members,
                            sharedElementPrefix = option.conversation.id.id,
                            sharedUri = option.post.uri.asGenericUri(),
                            referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                        ),
                    )

                is PostOption.ThreadGate ->
                    results.firstOrNull { it.timelineItem.post.uri == option.postUri }
                        ?.timelineItem
                        ?.let(threadGateSheetState::show)

                is PostOption.Moderation.BlockAccount ->
                    profileRestrictionDialogState.show(option)

                is PostOption.Moderation.MuteAccount ->
                    profileRestrictionDialogState.show(option)

                is PostOption.Moderation.MuteWords -> mutedWordsSheetState.show()
                is PostOption.Delete -> onDeletePostClicked(option.postUri)
            }
        },
    )
    val postActions = remember(
        sharedElementPrefix,
        onLinkTargetClicked,
        onPostSearchResultClicked,
        onPostSearchResultProfileClicked,
        onPostRecordClicked,
        onPublicationSubscriptionToggled,
        onMediaClicked,
        onReplyToPost,
    ) {
        PostActions { action ->
            when (action) {
                is PostAction.OfLinkTarget -> onLinkTargetClicked(action.linkTarget)

                is PostAction.OfPost -> onPostSearchResultClicked(
                    action.post,
                    sharedElementPrefix,
                )

                is PostAction.OfProfile -> onPostSearchResultProfileClicked(
                    action.profile,
                    action.post,
                    sharedElementPrefix.withQuotingPostUriPrefix(action.quotingPostUri),
                )

                is PostAction.OfRecord -> onPostRecordClicked(
                    action.record,
                    sharedElementPrefix.withQuotingPostUriPrefix(action.owningPostUri),
                )

                is PostAction.OfPublicationSubscription -> onPublicationSubscriptionToggled(
                    action.publication,
                )

                is PostAction.OfMedia -> onMediaClicked(
                    action.media,
                    action.index,
                    action.post,
                    sharedElementPrefix.withQuotingPostUriPrefix(action.quotingPostUri),
                )

                is PostAction.OfReply -> onReplyToPost(action.post, sharedElementPrefix)

                is PostAction.OfInteraction -> postInteractionSheetState.onInteraction(action)

                is PostAction.OfMore -> postOptionsSheetState.showOptions(action.post)

                else -> Unit
            }
        }
    }
    LazyVerticalStaggeredGrid(
        modifier = modifier.onDominantVideoChange(
            topLeftInset = {
                IntOffset(
                    x = 0,
                    y = gridState.layoutInfo.beforeContentPadding,
                ) - paneScaffoldState.topAppBarNestedScrollConnection.roundedMaxDelta
            },
            bottomRightInset = {
                paneScaffoldState.bottomNavigationNestedScrollConnection.roundedMaxDelta
            },
            isEnabled = {
                paneScaffoldState.paneState.pane == ThreePane.Primary &&
                    autoPlayTimelineVideos &&
                    isActivePage()
            },
            onIdChanged = { videoId ->
                if (videoId != null) videoPlayerController.play(videoId = videoId)
                else videoPlayerController.pauseActiveVideo()
            },
        ),
        state = gridState,
        columns = StaggeredGridCells.Adaptive(
            displayState.cardSize(presentation),
        ),
        verticalItemSpacing = displayState.verticalItemSpacing(presentation),
        contentPadding = bottomNavAndInsetPaddingValues(
            top = UiTokens.statusBarHeight + UiTokens.toolbarHeight + UiTokens.tabsHeight,
            isCompact = paneScaffoldState.prefersCompactBottomNav,
        ),
        horizontalArrangement = Arrangement.spacedBy(
            displayState.horizontalItemSpacing(presentation),
        ),
    ) {
        items(
            items = results,
            key = { it.id },
            itemContent = { result ->
                TimelineItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    paneTransitionScope = paneScaffoldState,
                    presentationLookaheadScope = paneScaffoldState,
                    now = now,
                    item = result.timelineItem,
                    sharedElementPrefix = state.sharedElementPrefix,
                    showEngagementMetrics = showEngagementMetrics,
                    presentation = presentation,
                    postActions = postActions,
                )
            },
        )
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
