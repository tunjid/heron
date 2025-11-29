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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.composables.lazy.pendingScrollOffsetState
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.core.types.PostUri
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
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.post.PostInteractionsSheetState.Companion.rememberUpdatedPostInteractionState
import com.tunjid.heron.timeline.ui.post.PostOption
import com.tunjid.heron.timeline.ui.post.PostOptionsSheetState.Companion.rememberUpdatedPostOptionsState
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.threadedVideoPosition
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionStates
import com.tunjid.heron.timeline.ui.postActions
import com.tunjid.heron.timeline.ui.withQuotingPostUriPrefix
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.canAutoPlayVideo
import com.tunjid.heron.timeline.utilities.pendingOffsetFor
import com.tunjid.heron.ui.UiTokens
import com.tunjid.treenav.compose.threepane.ThreePane
import kotlin.math.floor
import kotlinx.datetime.Clock

@Composable
internal fun PostDetailScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyStaggeredGridState()
    val items by rememberUpdatedState(state.items)
    val pendingScrollOffsetState = gridState.pendingScrollOffsetState()

    val videoStates = remember { ThreadedVideoPositionStates(TimelineItem::id) }
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
                        sharedElementPrefix = state.sharedElementPrefix,
                    ),
                ),
            )
        },
    )

    val postOptionsState = rememberUpdatedPostOptionsState(
        signedInProfileId = state.signedInProfileId,
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
                    postActions = remember(state.sharedElementPrefix) {
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
                                actions(
                                    Action.Navigate.To(
                                        recordDestination(
                                            referringRouteOption = NavigationAction.ReferringRouteOption.Parent,
                                            sharedElementPrefix = state.sharedElementPrefix,
                                            record = post,
                                        ),
                                    ),
                                )
                            },
                            onProfileClicked = { profile: Profile, post: Post, quotingPostUri: PostUri? ->
                                pendingScrollOffsetState.value = gridState.pendingOffsetFor(item)
                                actions(
                                    Action.Navigate.To(
                                        profileDestination(
                                            referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                            profile = profile,
                                            avatarSharedElementKey = post.avatarSharedElementKey(
                                                prefix = state.sharedElementPrefix,
                                                quotingPostUri = quotingPostUri,
                                            ).takeIf { post.author.did == profile.did },
                                        ),
                                    ),
                                )
                            },
                            onPostRecordClicked = { record, owningPostUri ->
                                actions(
                                    Action.Navigate.To(
                                        recordDestination(
                                            referringRouteOption = NavigationAction.ReferringRouteOption.Parent,
                                            sharedElementPrefix = state.sharedElementPrefix.withQuotingPostUriPrefix(
                                                quotingPostUri = owningPostUri,
                                            ),
                                            record = record,
                                        ),
                                    ),
                                )
                            },
                            onPostMediaClicked = { media: Embed.Media, index: Int, post: Post, quotingPostUri: PostUri? ->
                                pendingScrollOffsetState.value = gridState.pendingOffsetFor(item)
                                actions(
                                    Action.Navigate.To(
                                        galleryDestination(
                                            post = post,
                                            media = media,
                                            startIndex = index,
                                            sharedElementPrefix = state.sharedElementPrefix.withQuotingPostUriPrefix(
                                                quotingPostUri = quotingPostUri,
                                            ),
                                        ),
                                    ),
                                )
                            },
                            onReplyToPost = { post: Post ->
                                pendingScrollOffsetState.value = gridState.pendingOffsetFor(item)
                                actions(
                                    Action.Navigate.To(
                                        if (paneScaffoldState.isSignedOut) signInDestination()
                                        else composePostDestination(
                                            type = Post.Create.Reply(
                                                parent = post,
                                            ),
                                            sharedElementPrefix = state.sharedElementPrefix,
                                        ),
                                    ),
                                )
                            },
                            onPostMetadataClicked = onPostMetadataClicked@{ postMetadata ->
                                pendingScrollOffsetState.value = gridState.pendingOffsetFor(item)
                                actions(
                                    Action.Navigate.To(
                                        when (postMetadata) {
                                            is Post.Metadata.Likes -> postLikesDestination(
                                                profileId = postMetadata.profileId,
                                                postRecordKey = postMetadata.postRecordKey,
                                            )

                                            is Post.Metadata.Quotes -> postQuotesDestination(
                                                profileId = postMetadata.profileId,
                                                postRecordKey = postMetadata.postRecordKey,
                                            )
                                            is Post.Metadata.Reposts -> postRepostsDestination(
                                                profileId = postMetadata.profileId,
                                                postRecordKey = postMetadata.postRecordKey,
                                            )
                                        },
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
        // Allow for scrolling to the post selected even if others came before.
        item(
            span = StaggeredGridItemSpan.FullLine,
        ) {
            Spacer(Modifier.height(800.dp))
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
}
