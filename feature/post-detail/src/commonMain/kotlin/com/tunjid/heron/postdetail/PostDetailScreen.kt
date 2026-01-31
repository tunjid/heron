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

package com.tunjid.heron.postdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.composables.lazy.rememberLazyScrollableState
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Post.Create.Reply
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.utilities.asGenericUri
import com.tunjid.heron.interpolatedVisibleIndexEffect
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.composePostDestination
import com.tunjid.heron.scaffold.navigation.conversationDestination
import com.tunjid.heron.scaffold.navigation.galleryDestination
import com.tunjid.heron.scaffold.navigation.pathDestination
import com.tunjid.heron.scaffold.navigation.postLikesDestination
import com.tunjid.heron.scaffold.navigation.postQuotesDestination
import com.tunjid.heron.scaffold.navigation.postRepostsDestination
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.navigation.recordDestination
import com.tunjid.heron.scaffold.navigation.signInDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.paneClip
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.timeline.ui.PostActions
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.post.PostInteractionsSheetState.Companion.rememberUpdatedPostInteractionsSheetState
import com.tunjid.heron.timeline.ui.post.PostMetadata
import com.tunjid.heron.timeline.ui.post.PostOption
import com.tunjid.heron.timeline.ui.post.PostOptionsSheetState.Companion.rememberUpdatedPostOptionsSheetState
import com.tunjid.heron.timeline.ui.post.ThreadGateSheetState.Companion.rememberUpdatedThreadGateSheetState
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.timeline.ui.profile.ProfileRestrictionDialogState.Companion.rememberProfileRestrictionDialogState
import com.tunjid.heron.timeline.ui.sheets.MutedWordsSheetState.Companion.rememberUpdatedMutedWordsSheetState
import com.tunjid.heron.timeline.ui.withQuotingPostUriPrefix
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.canAutoPlayVideo
import com.tunjid.heron.timeline.utilities.pendingOffsetFor
import com.tunjid.heron.ui.UiTokens
import com.tunjid.treenav.compose.threepane.ThreePane
import kotlin.math.floor
import kotlin.time.Clock

@Composable
internal fun PostDetailScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
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
    val items by rememberUpdatedState(state.items)

    val videoStates = remember { ThreadedVideoPositionStates(TimelineItem::id) }
    val navigateTo = remember(actions) {
        { destination: NavigationAction.Destination ->
            actions(Action.Navigate.To(destination))
        }
    }
    val postInteractionSheetState = rememberUpdatedPostInteractionsSheetState(
        isSignedIn = paneScaffoldState.isSignedIn,
        onSignInClicked = {
            actions(Action.Navigate.To(signInDestination()))
        },
        onInteractionConfirmed = {
            actions(Action.SendPostInteraction(it))
        },
        onQuotePostClicked = { repost ->
            navigateTo(
                composePostDestination(
                    type = Post.Create.Quote(repost),
                    sharedElementPrefix = state.sharedElementPrefix,
                ),
            )
        },
    )
    val threadGateSheetState = rememberUpdatedThreadGateSheetState(
        onThreadGateUpdated = {
            actions(Action.SendPostInteraction(it))
        },
    )
    val mutedWordsSheetState = rememberUpdatedMutedWordsSheetState(
        mutedWordPreferences = state.preferences.mutedWordPreferences,
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
        signedInProfileId = state.signedInProfileId,
        recentConversations = state.recentConversations,
        onOptionClicked = { option ->
            when (option) {
                is PostOption.ShareInConversation ->
                    navigateTo(
                        conversationDestination(
                            id = option.conversation.id,
                            members = option.conversation.members,
                            sharedElementPrefix = option.conversation.id.id,
                            sharedUri = option.post.uri.asGenericUri(),
                            referringRouteOption = NavigationAction.ReferringRouteOption.Current,
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
            }
        },
    )

    LazyVerticalStaggeredGrid(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxSize()
            .paneClip(),
        state = gridState,
        columns = StaggeredGridCells.Adaptive(340.dp),
        verticalItemSpacing = 4.dp,
        contentPadding = UiTokens.bottomNavAndInsetPaddingValues(
            top = UiTokens.statusBarHeight + UiTokens.toolbarHeight,
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
                    presentationLookaheadScope = paneScaffoldState,
                    now = remember { Clock.System.now() },
                    item = item,
                    sharedElementPrefix = state.sharedElementPrefix,
                    presentation = Timeline.Presentation.Text.WithEmbed,
                    postActions = remember(state.sharedElementPrefix, state.signedInProfileId) {
                        PostActions { action ->
                            when (action) {
                                is PostAction.OfLinkTarget -> {
                                    val linkTarget = action.linkTarget
                                    if (linkTarget is LinkTarget.Navigable) navigateTo(
                                        pathDestination(
                                            path = linkTarget.path,
                                            referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                        ),
                                    )
                                }

                                is PostAction.OfPost -> {
                                    navigateTo(
                                        recordDestination(
                                            referringRouteOption = NavigationAction.ReferringRouteOption.Parent,
                                            sharedElementPrefix = state.sharedElementPrefix,
                                            otherModels = listOfNotNull(action.warnedAppliedLabels),
                                            record = action.post,
                                        ),
                                    )
                                }

                                is PostAction.OfProfile -> {
                                    pendingScrollOffset = gridState.pendingOffsetFor(item)
                                    navigateTo(
                                        profileDestination(
                                            referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                            profile = action.profile,
                                            avatarSharedElementKey = action.post.avatarSharedElementKey(
                                                prefix = state.sharedElementPrefix,
                                                quotingPostUri = action.quotingPostUri,
                                            ).takeIf { action.post.author.did == action.profile.did },
                                        ),
                                    )
                                }

                                is PostAction.OfRecord -> {
                                    val record = action.record
                                    val owningPostUri = action.owningPostUri
                                    navigateTo(
                                        recordDestination(
                                            referringRouteOption = NavigationAction.ReferringRouteOption.Parent,
                                            sharedElementPrefix = state.sharedElementPrefix.withQuotingPostUriPrefix(
                                                quotingPostUri = owningPostUri,
                                            ),
                                            record = record,
                                        ),
                                    )
                                }

                                is PostAction.OfMedia -> {
                                    pendingScrollOffset = gridState.pendingOffsetFor(item)
                                    navigateTo(
                                        galleryDestination(
                                            post = action.post,
                                            media = action.media,
                                            startIndex = action.index,
                                            sharedElementPrefix = state.sharedElementPrefix.withQuotingPostUriPrefix(
                                                quotingPostUri = action.quotingPostUri,
                                            ),
                                            source = null,
                                        ),
                                    )
                                }

                                is PostAction.OfReply -> {
                                    pendingScrollOffset = gridState.pendingOffsetFor(item)
                                    navigateTo(
                                        if (paneScaffoldState.isSignedOut) signInDestination()
                                        else composePostDestination(
                                            type = Reply(
                                                parent = action.post,
                                            ),
                                            sharedElementPrefix = state.sharedElementPrefix,
                                        ),

                                    )
                                }

                                is PostAction.OfMetadata -> {
                                    val postMetadata = action.metadata
                                    pendingScrollOffset = gridState.pendingOffsetFor(item)
                                    when (postMetadata) {
                                        is PostMetadata.Likes -> navigateTo(
                                            postLikesDestination(
                                                profileId = postMetadata.profileId,
                                                postRecordKey = postMetadata.postRecordKey,
                                            ),
                                        )

                                        is PostMetadata.Quotes -> navigateTo(
                                            postQuotesDestination(
                                                profileId = postMetadata.profileId,
                                                postRecordKey = postMetadata.postRecordKey,
                                            ),
                                        )

                                        is PostMetadata.Reposts -> navigateTo(
                                            postRepostsDestination(
                                                profileId = postMetadata.profileId,
                                                postRecordKey = postMetadata.postRecordKey,
                                            ),
                                        )

                                        is PostMetadata.Gate ->
                                            if (state.signedInProfileId == postMetadata.postUri.profileId()) {
                                                items.firstOrNull { it.post.uri == postMetadata.postUri }
                                                    ?.let(threadGateSheetState::show)
                                            }
                                    }
                                }

                                is PostAction.OfInteraction -> {
                                    postInteractionSheetState.onInteraction(action)
                                }

                                is PostAction.OfMore -> {
                                    postOptionsSheetState.showOptions(action.post)
                                }
                            }
                        }
                    },
                )
            },
        )
        // Allow for scrolling to the post selected even if others came before.
        item(
            span = StaggeredGridItemSpan.FullLine,
        ) {
            Spacer(Modifier.height(800.dp))
        }
    }

    if (paneScaffoldState.paneState.pane == ThreePane.Primary && state.preferences.local.autoPlayTimelineVideos) {
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
}
