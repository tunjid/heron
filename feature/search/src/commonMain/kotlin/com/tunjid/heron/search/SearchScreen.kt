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
import com.tunjid.heron.data.utilities.asGenericUri
import com.tunjid.heron.data.utilities.path
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.composePostDestination
import com.tunjid.heron.scaffold.navigation.conversationDestination
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
import com.tunjid.heron.timeline.ui.post.PostInteractionsSheetState.Companion.rememberUpdatedPostInteractionState
import com.tunjid.heron.timeline.ui.post.PostOption
import com.tunjid.heron.timeline.ui.post.PostOptionsSheetState.Companion.rememberUpdatedPostOptionsState
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey

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

                is PostOption.ThreadGate -> Unit
            }
        },
    )

    val pagerState = rememberPagerState { state.searchStateHolders.size }
    val onProfileClicked: (Profile, String) -> Unit = remember {
        { profile, sharedElementPrefix ->
            actions(
                Action.Navigate.To(
                    profileDestination(
                        profile = profile,
                        avatarSharedElementKey = profile.avatarSharedElementKey(sharedElementPrefix),
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
    val onLinkTargetClicked = remember {
        { linkTarget: LinkTarget ->
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
        { feedGenerator: FeedGenerator, sharedElementPrefix: String ->
            actions(
                Action.Navigate.To(
                    pathDestination(
                        path = feedGenerator.uri.path,
                        models = listOf(feedGenerator),
                        sharedElementPrefix = sharedElementPrefix,
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
                    recordDestination(
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                        sharedElementPrefix = sharedElementPrefix,
                        record = post,
                    ),
                ),
            )
        }
    }
    val onReplyToPost = remember {
        { post: Post, sharedElementPrefix: String ->
            actions(
                Action.Navigate.To(
                    if (paneScaffoldState.isSignedOut) signInDestination()
                    else composePostDestination(
                        type = Post.Create.Reply(
                            parent = post,
                        ),
                        sharedElementPrefix = sharedElementPrefix,
                    ),
                ),
            )
        }
    }
    val onPostRecordClicked = remember {
        { record: Record, sharedElementPrefix: String ->
            actions(
                Action.Navigate.To(
                    recordDestination(
                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                        sharedElementPrefix = sharedElementPrefix,
                        record = record,
                    ),
                ),
            )
        }
    }
    val onMediaClicked = remember {
        { media: Embed.Media, index: Int, post: Post, sharedElementPrefix: String ->
            actions(
                Action.Navigate.To(
                    galleryDestination(
                        post = post,
                        media = media,
                        startIndex = index,
                        sharedElementPrefix = sharedElementPrefix,
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
                onProfileClicked = onProfileClicked,
                onViewerStateClicked = onViewerStateClicked,
                onLinkTargetClicked = onLinkTargetClicked,
                onPostSearchResultProfileClicked = onPostSearchResultProfileClicked,
                onPostSearchResultClicked = onPostSearchResultClicked,
                onReplyToPost = onReplyToPost,
                onPostRecordClicked = onPostRecordClicked,
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
