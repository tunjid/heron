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

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.core.models.ContentLabelPreference
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileTab
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.StandardSubscription
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.TimelinePreference
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.core.models.profileTimelineType
import com.tunjid.heron.data.core.models.timelineRecordUri
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.ProfileUri.Companion.asSelfLabelerUri
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.TimelineRequest
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.profile.ProfileScreenStateHolders.Records.Documents
import com.tunjid.heron.profile.di.profileHandleOrId
import com.tunjid.heron.timeline.state.recordStateHolder
import com.tunjid.heron.timeline.state.timelineStateHolder
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.coroutines.RouteViewModel
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.routeString
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

internal typealias ProfileStateHolder = ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface ProfileViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualProfileViewModel
}

@Stable
@AssistedInject
class ActualProfileViewModel(
    authRepository: AuthRepository,
    recordRepository: RecordRepository,
    profileRepository: ProfileRepository,
    timelineRepository: TimelineRepository,
    userDataRepository: UserDataRepository,
    writeQueue: WriteQueue,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : RouteViewModel(scope, route),
    ProfileStateHolder by scope.actionSuspendingStateMutator(
        state = State(route).toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { state, actions ->
            launchCommonFollowerMutations(
                state = state,
                profileId = route.profileHandleOrId,
                profileRepository = profileRepository,
            )
            launchProfileRelationshipMutations(
                state = state,
                profileId = route.profileHandleOrId,
                profileRepository = profileRepository,
            )
            launchSupportedAppMutations(
                state = state,
                profileId = route.profileHandleOrId,
                profileRepository = profileRepository,
            )
            launchFeedGeneratorUrisToStatusMutations(
                state = state,
                timelineRepository = timelineRepository,
            )
            launchSubscribedLabelerMutations(
                state = state,
                recordRepository = recordRepository,
            )
            launchLoadPreferencesMutations(
                state = state,
                userDataRepository = userDataRepository,
            )
            launchLoadProfileMutations(
                state = state,
                profileId = route.profileHandleOrId,
                viewModelScope = scope,
                writeQueue = writeQueue,
                authRepository = authRepository,
                recordRepository = recordRepository,
                profileRepository = profileRepository,
                timelineRepository = timelineRepository,
            )
            actions.launchMutationsIn(
                productionScope = this,
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.UpdatePageWithUpdates -> action.flow.collect { event ->
                        if (state.sourceIdsToHasUpdates[event.sourceId] != event.hasUpdates) {
                            state.sourceIdsToHasUpdates += (event.sourceId to event.hasUpdates)
                        }
                    }
                    is Action.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(state)

                    is Action.ToggleViewerState -> action.flow.launchToggleViewerStateMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )

                    is Action.UpdatePreferences -> action.flow.launchFeedGeneratorStatusMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )

                    is Action.Navigate -> action.flow.collect { navAction ->
                        navActions(navAction.navigationMutation)
                    }
                    is Action.BioLinkClicked -> action.flow.collect { event ->
                        when (val target = event.target) {
                            is LinkTarget.Navigable -> navActions {
                                routeString(target.path, queryParams = emptyMap())
                                    .toRoute
                                    .let(navState::push)
                            }
                            else -> Unit
                        }
                    }
                    is Action.Block -> action.flow.launchBlockAccountMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.Mute -> action.flow.launchMuteAccountMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.PageChanged -> action.flow.collect { event ->
                        state.currentPage = event.page
                    }
                    is Action.TogglePublicationSubscription -> action.flow.launchTogglePublicationSubscriptionMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.DeleteRecord -> action.flow.launchDeleteRecordMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.UpdateLiveStatus -> action.flow.launchLiveStatusMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private fun launchLoadPreferencesMutations(
    state: State.SnapshotMutable,
    userDataRepository: UserDataRepository,
) = userDataRepository.preferences.launchedCollect {
    state.preferences = it
}

context(productionScope: CoroutineScope)
private fun launchSubscribedLabelerMutations(
    state: State.SnapshotMutable,
    recordRepository: RecordRepository,
) = recordRepository.subscribedLabelers.launchedCollect { labelers ->
    state.subscribedLabelers = labelers
}

context(productionScope: CoroutineScope)
private fun launchCommonFollowerMutations(
    state: State.SnapshotMutable,
    profileId: Id.Profile,
    profileRepository: ProfileRepository,
) = profileRepository.commonFollowers(
    otherProfileId = profileId,
    limit = 6,
).launchedCollect {
    state.commonFollowers = it
}

context(productionScope: CoroutineScope)
private fun launchLoadProfileMutations(
    state: State.SnapshotMutable,
    profileId: Id.Profile,
    viewModelScope: CoroutineScope,
    writeQueue: WriteQueue,
    authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    timelineRepository: TimelineRepository,
    recordRepository: RecordRepository,
) {
    val sharedProfile = profileRepository.profile(profileId)
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            replay = 1,
        )

    val sharedSignInState = authRepository.signedInUser
        .map { signedInProfile ->
            val signedInProfileId = signedInProfile?.did
            val isSignedInProfile = signedInProfile?.let {
                it.did.id == profileId.id || it.handle.id == profileId.id
            } ?: false
            signedInProfileId to isSignedInProfile
        }
        .distinctUntilChanged()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            replay = 1,
        )

    sharedProfile.launchedCollect { profile ->
        state.profile = profile
    }
    sharedSignInState.launchedCollect { (signedInProfileId, isSignedInProfile) ->
        state.signedInProfileId = signedInProfileId
        state.isSignedInProfile = isSignedInProfile
    }
    combine(
        flow = sharedSignInState,
        flow2 = profileRepository.tabs(profileId),
        flow3 = sharedProfile
            .map { it.did to it.isLabeler }
            .distinctUntilChanged(),
        transform = ::Triple,
    ).launchedCollectLatest { (signedInProfileIdToIsSignedInProfile, tabs, profileIdToIsLabeler) ->
        val (signedInProfileId, isSignedInProfile) = signedInProfileIdToIsSignedInProfile
        val isSignedIn = signedInProfileId != null
        val (resolvedProfileId, isLabeler) = profileIdToIsLabeler
        val tabsToHolders = state.stateHolders
            .associateBy(ProfileScreenStateHolders::tab)

        val holders = coroutineScope {
            tabs
                .filter { tab ->
                    tab.shouldShow(
                        isSignedIn = isSignedIn,
                        isSignedInProfile = isSignedInProfile,
                        isLabeler = isLabeler,
                    )
                }
                .map { tab ->
                    async {
                        // Make sure the ViewModel CoroutineScope is used
                        // for the state holder
                        tabsToHolders[tab] ?: viewModelScope.profileScreenStateHolder(
                            tab = tab,
                            profileId = resolvedProfileId,
                            recordRepository = recordRepository,
                            timelineRepository = timelineRepository,
                        )
                    }
                }
                .awaitAll()
        }
            .filterNotNull()
            .toMutableList()

        if (isLabeler) holders.add(
            index = 0,
            element = state.stateHolders
                .filterIsInstance<ProfileScreenStateHolders.LabelerSettings>()
                .firstOrNull() ?: viewModelScope.labelerSettingsStateHolder(
                profileId = resolvedProfileId,
                writeQueue = writeQueue,
                timelineRepository = timelineRepository,
                recordRepository = recordRepository,
            ),
        )

        state.stateHolders = holders
    }
}

context(productionScope: CoroutineScope)
private fun launchProfileRelationshipMutations(
    state: State.SnapshotMutable,
    profileId: Id.Profile,
    profileRepository: ProfileRepository,
) = profileRepository.profileRelationships(setOf(profileId)).launchedCollect {
    state.viewerState = it.firstOrNull()
}

context(productionScope: CoroutineScope)
private fun launchSupportedAppMutations(
    state: State.SnapshotMutable,
    profileId: Id.Profile,
    profileRepository: ProfileRepository,
) = profileRepository.supportedApps(profileId).launchedCollect {
    state.supportedApps = it
}

context(productionScope: CoroutineScope)
private fun launchFeedGeneratorUrisToStatusMutations(
    state: State.SnapshotMutable,
    timelineRepository: TimelineRepository,
) = timelineRepository.preferences
    .distinctUntilChangedBy { it.timelinePreferences }
    .launchedCollect { preferences ->
        state.timelineRecordUrisToPinnedStatus = preferences.timelinePreferences
            .associateBy(
                keySelector = TimelinePreference::timelineRecordUri,
                valueTransform = TimelinePreference::pinned,
            )
    }

private fun Action.Block.toBlockWritable(): Writable.Restriction =
    Writable.Restriction(
        when (this) {
            is Action.Block.Add -> Profile.Restriction.Block.Add(
                signedInProfileId = signedInProfileId,
                profileId = profileId,
            )
            is Action.Block.Remove -> Profile.Restriction.Block.Remove(
                signedInProfileId = signedInProfileId,
                profileId = profileId,
                blockUri = blockUri,
            )
        },
    )

context(productionScope: CoroutineScope)
private fun Flow<Action.Block>.launchBlockAccountMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { it.toBlockWritable() },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

private fun Action.Mute.toMuteWritable(): Writable.Restriction =
    Writable.Restriction(
        when (this) {
            is Action.Mute.Add -> Profile.Restriction.Mute.Add(
                signedInProfileId = signedInProfileId,
                profileId = profileId,
            )
            is Action.Mute.Remove -> Profile.Restriction.Mute.Remove(
                signedInProfileId = signedInProfileId,
                profileId = profileId,
            )
        },
    )

context(productionScope: CoroutineScope)
private fun Flow<Action.Mute>.launchMuteAccountMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { it.toMuteWritable() },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

private fun Action.UpdateLiveStatus.toLiveStatusWritable(): Writable.StatusUpdate =
    Writable.StatusUpdate(
        when (this) {
            is Action.UpdateLiveStatus.GoLive -> Profile.StatusUpdate.GoLive(
                signedInProfileId = signedInProfileId,
                streamUrl = streamUrl,
                durationMinutes = duration,
            )
            is Action.UpdateLiveStatus.EndLive -> Profile.StatusUpdate.EndLive(
                signedInProfileId = signedInProfileId,
            )
        },
    )

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateLiveStatus>.launchLiveStatusMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { it.toLiveStatusWritable() },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchedCollect { event ->
    state.messages -= event.message
}

context(productionScope: CoroutineScope)
private fun Flow<Action.TogglePublicationSubscription>.launchTogglePublicationSubscriptionMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
        when (action) {
            is Action.TogglePublicationSubscription.Subscribe -> Writable.StandardSite.Subscribe(
                create = StandardSubscription.Create(publicationUri = action.publicationUri),
            )
            is Action.TogglePublicationSubscription.Unsubscribe -> Writable.RecordDeletion(
                recordUri = action.subscriptionUri,
            )
        }
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.DeleteRecord>.launchDeleteRecordMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { Writable.RecordDeletion(recordUri = it.recordUri) },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

private fun Action.ToggleViewerState.toConnectionWritable(): Writable.Connection =
    Writable.Connection(
        when (val following = this.following) {
            null -> Profile.Connection.Follow(
                signedInProfileId = signedInProfileId,
                profileId = viewedProfileId,
                followedBy = followedBy,
            )
            else -> Profile.Connection.Unfollow(
                signedInProfileId = signedInProfileId,
                profileId = viewedProfileId,
                followUri = following,
                followedBy = followedBy,
            )
        },
    )

context(productionScope: CoroutineScope)
private fun Flow<Action.ToggleViewerState>.launchToggleViewerStateMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { it.toConnectionWritable() },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdatePreferences>.launchFeedGeneratorStatusMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { Writable.TimelineUpdate(it.update) },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

private suspend fun CoroutineScope.profileScreenStateHolder(
    tab: ProfileTab,
    profileId: ProfileId,
    recordRepository: RecordRepository,
    timelineRepository: TimelineRepository,
): ProfileScreenStateHolders? = when (tab) {
    is ProfileTab.Bluesky.Posts -> ProfileScreenStateHolders.Timeline(
        timelineStateHolder(
            initialItems = TimelineItem.LoadingItems,
            refreshOnStart = true,
            timeline = timelineRepository.timeline(
                TimelineRequest.OfProfile(
                    profileHandleOrDid = profileId,
                    type = tab.profileTimelineType,
                ),
            )
                .first(),
            startNumColumns = 1,
            timelineRepository = timelineRepository,
        ),
    )
    is ProfileTab.Bluesky.FeedGenerators.FeedGenerator -> ProfileScreenStateHolders.Timeline(
        timelineStateHolder(
            initialItems = TimelineItem.LoadingItems,
            refreshOnStart = true,
            timeline = timelineRepository.timeline(
                TimelineRequest.OfFeed.WithUri(
                    uri = tab.uri,
                ),
            )
                .first(),
            startNumColumns = 1,
            timelineRepository = timelineRepository,
        ),
    )
    ProfileTab.Bluesky.FeedGenerators.All -> ProfileScreenStateHolders.Records.Feeds(
        mutator = recordStateHolder(
            profileId = profileId,
            stringResource = tab.stringResource,
            itemId = FeedGenerator::cid,
            cursorListLoader = recordRepository::feedGenerators,
        ),
    )
    ProfileTab.Bluesky.StarterPacks -> ProfileScreenStateHolders.Records.StarterPacks(
        mutator = recordStateHolder(
            profileId = profileId,
            stringResource = tab.stringResource,
            itemId = StarterPack::cid,
            cursorListLoader = recordRepository::starterPacks,
        ),
    )
    ProfileTab.Bluesky.Lists.All -> ProfileScreenStateHolders.Records.Lists(
        mutator = recordStateHolder(
            profileId = profileId,
            stringResource = tab.stringResource,
            itemId = FeedList::cid,
            cursorListLoader = recordRepository::lists,
        ),
    )
    ProfileTab.StandardSite.Documents -> Documents(
        mutator = recordStateHolder(
            profileId = profileId,
            stringResource = tab.stringResource,
            itemId = StandardDocument::uri,
            cursorListLoader = recordRepository::authorDocuments,
        ),
    )
    ProfileTab.StandardSite.Publications -> ProfileScreenStateHolders.Records.Publications(
        mutator = recordStateHolder(
            profileId = profileId,
            stringResource = tab.stringResource,
            itemId = StandardPublication::uri,
            cursorListLoader = recordRepository::authorPublications,
        ),
    )
}

private fun CoroutineScope.labelerSettingsStateHolder(
    profileId: ProfileId,
    writeQueue: WriteQueue,
    timelineRepository: TimelineRepository,
    recordRepository: RecordRepository,
): ProfileScreenStateHolders.LabelerSettings =
    ProfileScreenStateHolders.LabelerSettings(
        mutator = actionStateFlowMutator(
            initialState = ProfileScreenStateHolders.LabelerSettings.Settings(),
            actionTransform = { actions ->
                actions.mapLatestToManyMutations { action ->
                    writeQueue.enqueue(
                        Writable.TimelineUpdate(
                            Timeline.Update.OfContentLabel.LabelVisibilityChange(
                                value = action.definition.identifier,
                                labelCreatorId = profileId,
                                visibility = action.visibility,
                            ),
                        ),
                    )
                }
            },
            inputs = listOf(
                recordRepository.subscribedLabelers.mapToMutation { labelers ->
                    copy(subscribed = labelers.any { it.creator.did == profileId })
                },
                combine(
                    flow = timelineRepository.preferences
                        .map { it.contentLabelPreferences }
                        .distinctUntilChanged(),
                    flow2 = recordRepository.embeddableRecord(
                        uri = profileId.asSelfLabelerUri(),
                    )
                        .filterIsInstance<Labeler>()
                        .distinctUntilChanged(),
                    transform = ::Pair,
                ).mapToMutation { (contentLabelPreferences, labeler) ->
                    val visibilityMap = contentLabelPreferences.associateBy(
                        keySelector = ContentLabelPreference::label,
                        valueTransform = ContentLabelPreference::visibility,
                    )
                    copy(
                        labelSettings = labeler.definitions.map { definition ->
                            ProfileScreenStateHolders.LabelerSettings.LabelSetting(
                                definition = definition,
                                visibility = visibilityMap[definition.identifier]
                                    ?: definition.defaultSetting,
                            )
                        },
                    )
                },
            ),
        ),
    )

private fun ProfileTab.shouldShow(
    isSignedIn: Boolean,
    isSignedInProfile: Boolean,
    isLabeler: Boolean,
): Boolean = when (this) {
    ProfileTab.Bluesky.Posts.Standard -> true
    ProfileTab.Bluesky.Posts.Replies -> isSignedIn
    ProfileTab.Bluesky.Posts.Likes -> isSignedInProfile && !isLabeler
    ProfileTab.Bluesky.Posts.Media,
    ProfileTab.Bluesky.Posts.Videos,
    is ProfileTab.Bluesky.FeedGenerators,
    is ProfileTab.Bluesky.Lists,
    ProfileTab.Bluesky.StarterPacks,
    ProfileTab.StandardSite.Documents,
    ProfileTab.StandardSite.Publications,
    -> !isLabeler
}
