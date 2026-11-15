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

package com.tunjid.heron.externalrecord.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.PolygonShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Event
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Rsvp
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.contentDescription
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.utilities.asGenericUri
import com.tunjid.heron.externalrecord.Action
import com.tunjid.heron.externalrecord.CommentsStateHolder
import com.tunjid.heron.externalrecord.RecordDescription
import com.tunjid.heron.externalrecord.RsvpsStateHolder
import com.tunjid.heron.media.images.AsyncImage
import com.tunjid.heron.media.images.ImageArgs
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
import com.tunjid.heron.timeline.ui.event.Event
import com.tunjid.heron.timeline.ui.profile.ProfileHandle
import com.tunjid.heron.timeline.ui.profile.ProfileName
import com.tunjid.heron.timeline.ui.withQuotingPostUriPrefix
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.contentType
import com.tunjid.heron.timeline.utilities.rememberTimelineDisplayState
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.UiTokens.bottomNavAndInsetPaddingValues
import com.tunjid.heron.ui.horizontalSpace
import com.tunjid.heron.ui.modifiers.shapedClickable
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.composePostDestination
import com.tunjid.heron.ui.scaffold.navigation.conversationDestination
import com.tunjid.heron.ui.scaffold.navigation.galleryDestination
import com.tunjid.heron.ui.scaffold.navigation.pathDestination
import com.tunjid.heron.ui.scaffold.navigation.profileDestination
import com.tunjid.heron.ui.scaffold.navigation.recordDestination
import com.tunjid.heron.ui.scaffold.navigation.signInDestination
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.paneClip
import com.tunjid.heron.ui.shapes.Circle
import com.tunjid.mutator.compose.produceStateWithLifecycle
import com.tunjid.tiler.compose.PivotedTilingEffect
import heron.feature.external_record.generated.resources.Res
import heron.feature.external_record.generated.resources.event_host
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EventHeader(
    paneScaffoldState: PaneScaffoldState,
    sharedElementPrefix: String,
    event: Event,
    actions: (Action) -> Unit,
) {
    Event(
        modifier = Modifier
            .padding(
                vertical = 8.dp,
            ),
        paneTransitionScope = paneScaffoldState,
        sharedElementPrefix = sharedElementPrefix,
        event = event,
        isBlurred = false,
        onRsvpClicked = { rsvpEvent, status ->
            actions(
                Action.RecordInteraction(
                    interaction = PostAction.OfRsvp(
                        event = rsvpEvent,
                        status = status,
                    ),
                ),
            )
        },
    )
    event.description
        ?.takeIf(String::isNotBlank)
        ?.let { description ->
            RecordDescription(
                title = event.name,
                description = description,
                actions = actions,
            )
        }
}

@Composable
fun EventTitle(
    modifier: Modifier = Modifier,
    event: Event,
) {
    Column(
        modifier = modifier
            .padding(
                horizontal = 8.dp,
            ),
    ) {
        Text(
            modifier = Modifier,
            text = event.name,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.titleSmallEmphasized,
        )
        Text(
            modifier = Modifier,
            text = stringResource(
                Res.string.event_host,
                event.author.handle.id,
            ),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
internal fun RsvpProfiles(
    modifier: Modifier = Modifier,
    paneScaffoldState: PaneScaffoldState,
    holder: RsvpsStateHolder,
    actions: (Action) -> Unit,
) {
    val state = holder.produceStateWithLifecycle()
    val rsvps = state.tiledItems
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxSize()
            .paneClip(),
        state = listState,
        contentPadding = bottomNavAndInsetPaddingValues(
            horizontal = 8.dp,
            isCompact = paneScaffoldState.prefersCompactBottomNav,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = !paneScaffoldState.isTransitionActive,
    ) {
        items(
            items = rsvps,
            key = { it.author.did.id },
            itemContent = { rsvp ->
                RsvpProfile(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    paneScaffoldState = paneScaffoldState,
                    rsvp = rsvp,
                    onProfileClicked = { profile ->
                        actions(
                            Action.Navigate.To(
                                profileDestination(
                                    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                    profile = profile,
                                    avatarSharedElementKey = profile.rsvpAvatarSharedElementKey(),
                                ),
                            ),
                        )
                    },
                )
            },
        )
    }

    listState.PivotedTilingEffect(
        items = rsvps,
        onQueryChanged = { query ->
            holder.accept(
                TilingState.Action.LoadAround(
                    query ?: state.tilingData.currentQuery,
                ),
            )
        },
    )
}

@Composable
private fun RsvpProfile(
    modifier: Modifier = Modifier,
    paneScaffoldState: PaneScaffoldState,
    rsvp: Rsvp,
    onProfileClicked: (Profile) -> Unit,
) = with(paneScaffoldState) {
    val profile = rsvp.author
    AttributionLayout(
        modifier = modifier
            .shapedClickable {
                onProfileClicked(profile)
            },
        avatar = {
            PaneStickySharedElement(
                modifier = Modifier
                    .size(UiTokens.avatarSize),
                sharedContentState = rememberSharedContentState(
                    key = profile.rsvpAvatarSharedElementKey(),
                ),
            ) {
                AsyncImage(
                    modifier = Modifier
                        .fillParentAxisIfFixedOrWrap(),
                    args = remember(profile.avatar) {
                        ImageArgs(
                            url = profile.avatar?.uri,
                            contentScale = ContentScale.Crop,
                            contentDescription = profile.contentDescription,
                            shape = PolygonShape.Circle,
                        )
                    },
                )
            }
        },
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
    )
}

@Composable
internal fun EventComments(
    modifier: Modifier = Modifier,
    paneScaffoldState: PaneScaffoldState,
    holder: CommentsStateHolder,
    showEngagementMetrics: Boolean,
    actions: (Action) -> Unit,
) {
    val state = holder.produceStateWithLifecycle()
    val items = state.items
    val listState = rememberLazyListState()

    val now = remember { Clock.System.now() }
    val presentation = Timeline.Presentation.Text.WithEmbed
    val sharedElementPrefix = remember(state.postUri) {
        "event-comments-${state.postUri.uri}"
    }
    val displayState = rememberTimelineDisplayState(
        extraSpacing = paneScaffoldState.navigationRailInsets::horizontalSpace,
    )
    val navigateTo = remember(actions) {
        { destination: NavigationAction.Destination ->
            actions(Action.Navigate.To(destination))
        }
    }
    val postInteractionSheetState = paneScaffoldState.rememberPostInteractionsSheetState(
        sharedElementPrefix = sharedElementPrefix,
    )
    val threadGateSheetState = paneScaffoldState.rememberTimelineThreadGateSheetState()
    val mutedWordsSheetState = paneScaffoldState.rememberMutedWordsSheetState()
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
    val postOptionsSheetState = paneScaffoldState.rememberPostOptionsSheetState(
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
                is PostOption.Delete -> actions(Action.DeleteRecord(option.postUri))
            }
        },
    )

    LookaheadScope {
        LazyColumn(
            modifier = modifier
                .padding(horizontal = 8.dp)
                .fillMaxSize()
                .paneClip(),
            state = listState,
            contentPadding = bottomNavAndInsetPaddingValues(
                isCompact = paneScaffoldState.prefersCompactBottomNav,
            ),
            verticalArrangement = Arrangement.spacedBy(displayState.verticalItemSpacing(presentation)),
            userScrollEnabled = !paneScaffoldState.isTransitionActive,
        ) {
            items(
                items = items,
                key = TimelineItem::id,
                contentType = TimelineItem::contentType,
                itemContent = { item ->
                    TimelineItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        paneTransitionScope = paneScaffoldState,
                        presentationLookaheadScope = this@LookaheadScope,
                        now = now,
                        item = item,
                        sharedElementPrefix = sharedElementPrefix,
                        showEngagementMetrics = showEngagementMetrics,
                        presentation = presentation,
                        postActions = remember(sharedElementPrefix) {
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

                                    is PostAction.OfPost -> navigateTo(
                                        recordDestination(
                                            referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                            sharedElementPrefix = sharedElementPrefix,
                                            otherModels = listOfNotNull(action.warnedAppliedLabels),
                                            record = action.post,
                                        ),
                                    )

                                    is PostAction.OfProfile -> {
                                        val profile = action.profile
                                        val post = action.post
                                        navigateTo(
                                            profileDestination(
                                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                profile = profile,
                                                avatarSharedElementKey = post
                                                    .avatarSharedElementKey(
                                                        prefix = sharedElementPrefix,
                                                        quotingPostUri = action.quotingPostUri,
                                                    )
                                                    .takeIf { post.author.did == profile.did },
                                            ),
                                        )
                                    }

                                    is PostAction.OfRecord -> navigateTo(
                                        recordDestination(
                                            referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                            sharedElementPrefix = sharedElementPrefix.withQuotingPostUriPrefix(
                                                quotingPostUri = action.owningPostUri,
                                            ),
                                            record = action.record,
                                        ),
                                    )

                                    is PostAction.OfMedia -> navigateTo(
                                        galleryDestination(
                                            post = action.post,
                                            media = action.media,
                                            startIndex = action.index,
                                            sharedElementPrefix = sharedElementPrefix.withQuotingPostUriPrefix(
                                                quotingPostUri = action.quotingPostUri,
                                            ),
                                        ),
                                    )

                                    is PostAction.OfReply -> navigateTo(
                                        if (paneScaffoldState.isSignedOut) signInDestination()
                                        else composePostDestination(
                                            type = Post.Create.Reply(
                                                parent = action.post,
                                            ),
                                            sharedElementPrefix = sharedElementPrefix,
                                        ),
                                    )

                                    is PostAction.OfInteraction -> {
                                        postInteractionSheetState.onInteraction(action)
                                    }

                                    is PostAction.OfMore -> {
                                        postOptionsSheetState.showOptions(action.post)
                                    }

                                    is PostAction.RecordInteraction ->
                                        actions(Action.RecordInteraction(action))

                                    else -> Unit
                                }
                            }
                        },
                    )
                },
            )
        }
    }
}

private fun Profile.rsvpAvatarSharedElementKey(): String =
    "event-rsvp-${did.id}"
