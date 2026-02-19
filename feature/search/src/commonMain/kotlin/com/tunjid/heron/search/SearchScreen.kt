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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.ListMember
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.Trend
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.utilities.path
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.composePostDestination
import com.tunjid.heron.scaffold.navigation.galleryDestination
import com.tunjid.heron.scaffold.navigation.pathDestination
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.navigation.recordDestination
import com.tunjid.heron.scaffold.navigation.signInDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.SignInPopUpState.Companion.rememberSignInPopUpState
import com.tunjid.heron.search.ui.searchresults.AutoCompleteProfileSearchResults
import com.tunjid.heron.search.ui.searchresults.GeneralSearchResults
import com.tunjid.heron.search.ui.searchresults.avatarSharedElementKey
import com.tunjid.heron.search.ui.suggestions.SuggestedContent
import com.tunjid.heron.search.ui.suggestions.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey

@Composable
internal fun SearchScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigateTo = remember {
        { destination: NavigationAction.Destination ->
            actions(Action.Navigate.To(destination))
        }
    }
    val signInPopUpState = rememberSignInPopUpState {
        navigateTo(signInDestination())
    }

    val pagerState = rememberPagerState { state.searchStateHolders.size }
    val onProfileClicked: (Profile, String) -> Unit = remember(navigateTo) {
        { profile, sharedElementPrefix ->
            navigateTo(
                profileDestination(
                    profile = profile,
                    avatarSharedElementKey = profile.avatarSharedElementKey(sharedElementPrefix),
                    referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
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
    val onLinkTargetClicked = remember(navigateTo) {
        { linkTarget: LinkTarget ->
            if (linkTarget is LinkTarget.Navigable) navigateTo(
                pathDestination(
                    path = linkTarget.path,
                    referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                ),
            )
        }
    }
    val onPostSearchResultProfileClicked = remember(navigateTo) {
        { profile: Profile, post: Post, sharedElementPrefix: String ->
            navigateTo(
                profileDestination(
                    referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                    profile = profile,
                    avatarSharedElementKey = post.avatarSharedElementKey(
                        sharedElementPrefix,
                    ),
                ),
            )
        }
    }
    val onListMemberClicked = remember(navigateTo) {
        { listMember: ListMember ->
            navigateTo(
                profileDestination(
                    referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                    profile = listMember.subject,
                    avatarSharedElementKey = listMember.avatarSharedElementKey(),
                ),
            )
        }
    }
    val onTrendClicked = remember(navigateTo) {
        { trend: Trend ->
            navigateTo(
                pathDestination(
                    path = trend.link,
                    referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                ),
            )
        }
    }
    val onFeedGeneratorClicked = remember(navigateTo) {
        { feedGenerator: FeedGenerator, sharedElementPrefix: String ->
            navigateTo(
                pathDestination(
                    path = feedGenerator.uri.path,
                    models = listOf(feedGenerator),
                    sharedElementPrefix = sharedElementPrefix,
                    referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
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
    val onPostSearchResultClicked = remember(navigateTo) {
        { post: Post, sharedElementPrefix: String ->
            navigateTo(
                recordDestination(
                    referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                    sharedElementPrefix = sharedElementPrefix,
                    record = post,
                ),
            )
        }
    }
    val onReplyToPost = remember(navigateTo) {
        { post: Post, sharedElementPrefix: String ->
            navigateTo(
                if (paneScaffoldState.isSignedOut) signInDestination()
                else composePostDestination(
                    type = Post.Create.Reply(
                        parent = post,
                    ),
                    sharedElementPrefix = sharedElementPrefix,
                ),
            )
        }
    }
    val onPostRecordClicked = remember(navigateTo) {
        { record: Record, sharedElementPrefix: String ->
            navigateTo(
                recordDestination(
                    referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                    sharedElementPrefix = sharedElementPrefix,
                    record = record,
                ),
            )
        }
    }
    val onMediaClicked = remember(navigateTo) {
        { media: Embed.Media, index: Int, post: Post, sharedElementPrefix: String ->
            navigateTo(
                galleryDestination(
                    post = post,
                    media = media,
                    startIndex = index,
                    sharedElementPrefix = sharedElementPrefix,
                ),
            )
        }
    }
    val sendPostInteraction = remember {
        { interaction: Post.Interaction ->
            actions(Action.SendPostInteraction(interaction))
        }
    }

    AnimatedContent(
        targetState = state.layout,
    ) { targetLayout ->
        when (targetLayout) {
            ScreenLayout.Suggested -> SuggestedContent(
                modifier = modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                paneScaffoldState = paneScaffoldState,
                trends = state.trends,
                suggestedProfiles = state.categoriesToSuggestedProfiles[state.suggestedProfileCategory]
                    ?: emptyList(),
                starterPacksWithMembers = state.starterPacksWithMembers,
                feedGenerators = state.feedGenerators,
                timelineRecordUrisToPinnedStatus = state.timelineRecordUrisToPinnedStatus,
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
                onProfileClicked = onProfileClicked,
                onViewerStateClicked = onViewerStateClicked,
            )

            ScreenLayout.GeneralSearchResults -> GeneralSearchResults(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                pagerState = pagerState,
                state = state,
                paneScaffoldState = paneScaffoldState,
                onRequestRecentLists = {
                    actions(Action.UpdateRecentLists)
                },
                onProfileClicked = onProfileClicked,
                onViewerStateClicked = onViewerStateClicked,
                onLinkTargetClicked = onLinkTargetClicked,
                onPostSearchResultProfileClicked = onPostSearchResultProfileClicked,
                onPostSearchResultClicked = onPostSearchResultClicked,
                onReplyToPost = onReplyToPost,
                onPostRecordClicked = onPostRecordClicked,
                onMediaClicked = onMediaClicked,
                onNavigate = navigateTo,
                onSendPostInteraction = sendPostInteraction,
                onFeedGeneratorClicked = onFeedGeneratorClicked,
                onTimelineUpdateClicked = onTimelineUpdateClicked,
                onSave = { actions(Action.UpdateMutedWord(it)) },
                onMuteAccountClicked = { signInProfileId, profileId ->
                    actions(
                        Action.MuteAccount(
                            signedInProfileId = signInProfileId,
                            profileId = profileId,
                        ),
                    )
                },
                onBlockAccountClicked = { signInProfileId, profileId ->
                    actions(
                        Action.BlockAccount(
                            signedInProfileId = signInProfileId,
                            profileId = profileId,
                        ),
                    )
                },
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
