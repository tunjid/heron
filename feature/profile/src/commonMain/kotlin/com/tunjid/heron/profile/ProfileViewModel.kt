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

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.ContentLabelPreference
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileTab
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StandardDocument
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
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.TimelineRequest
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.repository.recentConversations
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.profile.di.profileHandleOrId
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.tiling.tilingMutations
import com.tunjid.heron.timeline.state.timelineStateHolder
import com.tunjid.heron.timeline.utilities.enqueueMutations
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.distinctBy
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.routeString
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import heron.feature.profile.generated.resources.Res
import heron.feature.profile.generated.resources.feeds
import heron.feature.profile.generated.resources.lists
import heron.feature.profile.generated.resources.starter_packs
import heron.feature.profile.generated.resources.writing
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import org.jetbrains.compose.resources.StringResource

internal typealias ProfileStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualProfileViewModel
}

@AssistedInject
class ActualProfileViewModel(
    authRepository: AuthRepository,
    recordRepository: RecordRepository,
    messageRepository: MessageRepository,
    profileRepository: ProfileRepository,
    timelineRepository: TimelineRepository,
    userDataRepository: UserDataRepository,
    writeQueue: WriteQueue,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    ProfileStateHolder by scope.actionStateFlowMutator(
        initialState = State(route),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        inputs = listOf(
            commonFollowerMutations(
                profileId = route.profileHandleOrId,
                profileRepository = profileRepository,
            ),
            profileRelationshipMutations(
                profileId = route.profileHandleOrId,
                profileRepository = profileRepository,
            ),
            feedGeneratorUrisToStatusMutations(
                timelineRepository = timelineRepository,
            ),
            subscribedLabelerMutations(
                recordRepository = recordRepository,
            ),
            loadPreferencesMutations(
                userDataRepository = userDataRepository,
            ),
        ),
        actionTransform = transform@{ actions ->
            merge(
                loadProfileMutations(
                    currentState = { state() },
                    profileId = route.profileHandleOrId,
                    scope = scope,
                    writeQueue = writeQueue,
                    authRepository = authRepository,
                    recordRepository = recordRepository,
                    profileRepository = profileRepository,
                    timelineRepository = timelineRepository,
                ),
                actions.toMutationStream(
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.UpdatePageWithUpdates -> action.flow.pageWithUpdateMutations()
                        is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                            writeQueue = writeQueue,
                        )
                        is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations()

                        is Action.ToggleViewerState -> action.flow.toggleViewerStateMutations(
                            writeQueue = writeQueue,
                        )

                        is Action.UpdatePreferences -> action.flow.feedGeneratorStatusMutations(
                            writeQueue = writeQueue,
                        )

                        is Action.Navigate -> action.flow.consumeNavigationActions(
                            navigationMutationConsumer = navActions,
                        )
                        is Action.BioLinkClicked -> action.flow.mapToManyMutations {
                            when (val target = action.target) {
                                is LinkTarget.Navigable -> navActions {
                                    routeString(target.path, queryParams = emptyMap())
                                        .toRoute
                                        .let(navState::push)
                                }
                                else -> Unit
                            }
                        }
                        is Action.UpdateMutedWord -> action.flow.updateMutedWordMutations(
                            writeQueue = writeQueue,
                        )
                        is Action.Block -> action.flow.blockAccountMutations(
                            writeQueue = writeQueue,
                        )
                        is Action.Mute -> action.flow.muteAccountMutations(
                            writeQueue = writeQueue,
                        )
                        is Action.PageChanged -> action.flow.pageChangeMutations()
                        is Action.UpdateRecentConversations -> action.flow.recentConversationMutations(
                            messageRepository = messageRepository,
                        )
                        is Action.UpdateRecentLists -> action.flow.recentListsMutations(
                            recordRepository = recordRepository,
                        )
                        is Action.TogglePublicationSubscription -> action.flow.togglePublicationSubscriptionMutations(
                            writeQueue = writeQueue,
                        )
                        is Action.DeleteRecord -> action.flow.deleteRecordMutations(
                            writeQueue = writeQueue,
                        )
                        is Action.UpdateLiveStatus -> action.flow.liveStatusMutations(
                            writeQueue = writeQueue,
                        )
                    }
                },
            )
        },
    )

fun Flow<Action.UpdateRecentConversations>.recentConversationMutations(
    messageRepository: MessageRepository,
): Flow<Mutation<State>> =
    flatMapLatest {
        messageRepository.recentConversations()
            .mapToMutation { conversations ->
                copy(recentConversations = conversations)
            }
    }

fun Flow<Action.UpdateRecentLists>.recentListsMutations(
    recordRepository: RecordRepository,
): Flow<Mutation<State>> =
    flatMapLatest {
        recordRepository.recentLists
            .mapToMutation { lists ->
                copy(recentLists = lists)
            }
    }

private fun loadPreferencesMutations(
    userDataRepository: UserDataRepository,
): Flow<Mutation<State>> =
    userDataRepository.preferences
        .mapToMutation {
            copy(preferences = it)
        }

fun subscribedLabelerMutations(
    recordRepository: RecordRepository,
): Flow<Mutation<State>> =
    recordRepository.subscribedLabelers
        .mapToMutation { labelers ->
            copy(subscribedLabelers = labelers)
        }

private fun commonFollowerMutations(
    profileId: Id.Profile,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> =
    profileRepository.commonFollowers(
        otherProfileId = profileId,
        limit = 6,
    ).mapToMutation {
        copy(commonFollowers = it)
    }

private fun loadProfileMutations(
    currentState: suspend () -> State,
    profileId: Id.Profile,
    scope: CoroutineScope,
    writeQueue: WriteQueue,
    authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    timelineRepository: TimelineRepository,
    recordRepository: RecordRepository,
): Flow<Mutation<State>> {
    val sharedProfile = profileRepository.profile(profileId)
        .shareIn(
            scope = scope,
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
            scope = scope,
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            replay = 1,
        )

    return merge(
        sharedProfile
            .mapToMutation { profile ->
                copy(profile = profile)
            },
        sharedSignInState
            .mapToMutation { (signedInProfileId, isSignedInProfile) ->
                copy(
                    signedInProfileId = signedInProfileId,
                    isSignedInProfile = isSignedInProfile,
                )
            },
        combine(
            flow = sharedSignInState,
            flow2 = profileRepository.tabs(profileId),
            flow3 = sharedProfile
                .map { it.did to it.isLabeler }
                .distinctUntilChanged(),
            transform = ::Triple,
        )
            .mapLatestToManyMutations { (signedInProfileIdToIsSignedInProfile, tabs, profileIdToIsLabeler) ->
                val (signedInProfileId, isSignedInProfile) = signedInProfileIdToIsSignedInProfile
                val isSignedIn = signedInProfileId != null
                val (profileId, isLabeler) = profileIdToIsLabeler
                val stateHolders = currentState().stateHolders
                val tabsToHolders = stateHolders
                    .associateBy(ProfileScreenStateHolders::tab)

                val deferredHolders: List<Deferred<ProfileScreenStateHolders?>> = coroutineScope {
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
                                tabsToHolders[tab] ?: scope.profileScreenStateHolder(
                                    tab = tab,
                                    profileId = profileId,
                                    recordRepository = recordRepository,
                                    timelineRepository = timelineRepository,
                                )
                            }
                        }
                }

                val holders = deferredHolders.awaitAll()
                    .filterNotNull()
                    .toMutableList()

                if (isLabeler) holders.add(
                    index = 0,
                    element = stateHolders
                        .filterIsInstance<ProfileScreenStateHolders.LabelerSettings>()
                        .firstOrNull() ?: scope.labelerSettingsStateHolder(
                        profileId = profileId,
                        writeQueue = writeQueue,
                        timelineRepository = timelineRepository,
                        recordRepository = recordRepository,
                    ),
                )
                emit {
                    copy(stateHolders = holders)
                }
            },
    )
}

private fun profileRelationshipMutations(
    profileId: Id.Profile,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> =
    profileRepository.profileRelationships(setOf(profileId)).mapToMutation {
        copy(viewerState = it.firstOrNull())
    }

private fun feedGeneratorUrisToStatusMutations(
    timelineRepository: TimelineRepository,
): Flow<Mutation<State>> =
    timelineRepository.preferences
        .distinctUntilChangedBy { it.timelinePreferences }
        .mapToMutation { preferences ->
            copy(
                timelineRecordUrisToPinnedStatus = preferences.timelinePreferences
                    .associateBy(
                        keySelector = TimelinePreference::timelineRecordUri,
                        valueTransform = TimelinePreference::pinned,
                    ),
            )
        }

private fun Flow<Action.UpdatePageWithUpdates>.pageWithUpdateMutations(): Flow<Mutation<State>> =
    mapToMutation { (sourceId, hasUpdates) ->
        if (sourceIdsToHasUpdates[sourceId] == hasUpdates) this
        else copy(sourceIdsToHasUpdates = sourceIdsToHasUpdates + (sourceId to hasUpdates))
    }

private fun Flow<Action.UpdateMutedWord>.updateMutedWordMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = this.enqueueMutations(
    writeQueue,
    toWritable = {
        Writable.TimelineUpdate(
            Timeline.Update.OfMutedWord.ReplaceAll(
                mutedWordPreferences = it.mutedWordPreference,
            ),
        )
    },
) { _, memo ->
    if (memo != null) emit { copy(messages = messages + memo) }
}

private fun Flow<Action.Block>.blockAccountMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = this.enqueueMutations(
    writeQueue,
    toWritable = { action ->
        Writable.Restriction(
            when (action) {
                is Action.Block.Add -> Profile.Restriction.Block.Add(
                    signedInProfileId = action.signedInProfileId,
                    profileId = action.profileId,
                )
                is Action.Block.Remove -> Profile.Restriction.Block.Remove(
                    signedInProfileId = action.signedInProfileId,
                    profileId = action.profileId,
                    blockUri = action.blockUri,
                )
            },
        )
    },
) { _, memo ->
    if (memo != null) emit { copy(messages = messages + memo) }
}

private fun Flow<Action.Mute>.muteAccountMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = this.enqueueMutations(
    writeQueue,
    toWritable = { action ->
        Writable.Restriction(
            when (action) {
                is Action.Mute.Add -> Profile.Restriction.Mute.Add(
                    signedInProfileId = action.signedInProfileId,
                    profileId = action.profileId,
                )
                is Action.Mute.Remove -> Profile.Restriction.Mute.Remove(
                    signedInProfileId = action.signedInProfileId,
                    profileId = action.profileId,
                )
            },
        )
    },
) { _, memo ->
    if (memo != null) emit { copy(messages = messages + memo) }
}

private fun Flow<Action.SendPostInteraction>.postInteractionMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    this.enqueueMutations(
        writeQueue,
        toWritable = { Writable.Interaction(it.interaction) },
    ) { _, memo ->
        if (memo != null) emit { copy(messages = messages + memo) }
    }

private fun Flow<Action.UpdateLiveStatus>.liveStatusMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = enqueueMutations(
    writeQueue,
    toWritable = { action ->
        Writable.StatusUpdate(
            when (action) {
                is Action.UpdateLiveStatus.GoLive -> Profile.StatusUpdate.GoLive(
                    signedInProfileId = action.signedInProfileId,
                    streamUrl = action.streamUrl,
                    durationMinutes = action.duration,
                )
                is Action.UpdateLiveStatus.EndLive -> Profile.StatusUpdate.EndLive(
                    signedInProfileId = action.signedInProfileId,
                )
            },
        )
    },
) { _, memo ->
    if (memo != null) emit { copy(messages = messages + memo) }
}

private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(messages = messages - action.message)
    }

private fun Flow<Action.TogglePublicationSubscription>.togglePublicationSubscriptionMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = this.enqueueMutations(
    writeQueue,
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
) { _, memo ->
    if (memo != null) emit { copy(messages = messages + memo) }
}

private fun Flow<Action.DeleteRecord>.deleteRecordMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = this.enqueueMutations(
    writeQueue,
    toWritable = { Writable.RecordDeletion(recordUri = it.recordUri) },
) { _, memo ->
    if (memo != null) emit { copy(messages = messages + memo) }
}

private fun Flow<Action.ToggleViewerState>.toggleViewerStateMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    this.enqueueMutations(
        writeQueue,
        toWritable = { action ->
            Writable.Connection(
                when (val following = action.following) {
                    null -> Profile.Connection.Follow(
                        signedInProfileId = action.signedInProfileId,
                        profileId = action.viewedProfileId,
                        followedBy = action.followedBy,
                    )

                    else -> Profile.Connection.Unfollow(
                        signedInProfileId = action.signedInProfileId,
                        profileId = action.viewedProfileId,
                        followUri = following,
                        followedBy = action.followedBy,
                    )
                },
            )
        },
    ) { _, memo ->
        if (memo != null) emit { copy(messages = messages + memo) }
    }

private fun Flow<Action.UpdatePreferences>.feedGeneratorStatusMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    this.enqueueMutations(
        writeQueue,
        toWritable = { Writable.TimelineUpdate(it.update) },
    ) { _, memo ->
        if (memo != null) emit { copy(messages = messages + memo) }
    }

private fun Flow<Action.PageChanged>.pageChangeMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(currentPage = action.page)
    }

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
            initialState = recordTilingState(
                profileId = profileId,
                stringResource = Res.string.feeds,
            ),
            itemId = FeedGenerator::cid,
            cursorListLoader = recordRepository::feedGenerators,
        ),
    )
    ProfileTab.Bluesky.StarterPacks -> ProfileScreenStateHolders.Records.StarterPacks(
        mutator = recordStateHolder(
            initialState = recordTilingState(
                profileId = profileId,
                stringResource = Res.string.starter_packs,
            ),
            itemId = StarterPack::cid,
            cursorListLoader = recordRepository::starterPacks,
        ),
    )
    ProfileTab.Bluesky.Lists.All -> ProfileScreenStateHolders.Records.Lists(
        mutator = recordStateHolder(
            initialState = recordTilingState(
                profileId = profileId,
                stringResource = Res.string.lists,
            ),
            itemId = FeedList::cid,
            cursorListLoader = recordRepository::lists,
        ),
    )
    ProfileTab.StandardSite.Documents -> ProfileScreenStateHolders.Records.Documents(
        mutator = recordStateHolder(
            initialState = recordTilingState(
                profileId = profileId,
                stringResource = Res.string.writing,
            ),
            itemId = StandardDocument::uri,
            cursorListLoader = recordRepository::authorDocuments,
        ),
    )
    // TODO: Publications to be added in a future PR
    ProfileTab.StandardSite.Publications -> null
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

private fun defaultQueryData() = CursorQuery.Data(
    page = 0,
    cursorAnchor = Clock.System.now(),
    limit = 15,
)

private fun <T : Record> CoroutineScope.recordStateHolder(
    initialState: RecordState<T>,
    itemId: (T) -> Any,
    cursorListLoader: (ProfilesQuery, Cursor) -> Flow<CursorList<T>>,
): RecordStateHolder<T> = actionStateFlowMutator(
    initialState = initialState,
    actionTransform = transform@{ actions ->
        actions.toMutationStream {
            type().flow
                .tilingMutations(
                    currentState = { state() },
                    updateQueryData = { copy(data = it) },
                    refreshQuery = { copy(data = data.reset()) },
                    cursorListLoader = cursorListLoader,
                    onNewItems = { items ->
                        items.distinctBy(itemId)
                    },
                    onTilingDataUpdated = { copy(tilingData = it) },
                )
        }
    },
)

private fun <T : Record> recordTilingState(
    profileId: ProfileId,
    stringResource: StringResource,
): RecordState<T> =
    RecordState(
        stringResource = stringResource,
        tilingData = TilingState.Data(
            currentQuery = ProfilesQuery(
                profileId = profileId,
                data = defaultQueryData(),
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
    ProfileTab.Bluesky.FeedGenerators,
    ProfileTab.Bluesky.Lists.All,
    ProfileTab.Bluesky.StarterPacks,
    ProfileTab.StandardSite.Documents,
    ProfileTab.StandardSite.Publications,
    -> !isLabeler
}
