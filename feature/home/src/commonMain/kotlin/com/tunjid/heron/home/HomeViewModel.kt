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

package com.tunjid.heron.home

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.uri
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.recentConversations
import com.tunjid.heron.data.repository.signedInProfilePreferences
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.scaffold.scaffold.duplicateWriteMessage
import com.tunjid.heron.scaffold.scaffold.failedWriteMessage
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.state.timelineStateHolder
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.take

internal typealias HomeStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualHomeViewModel
}

@Inject
class ActualHomeViewModel(
    authRepository: AuthRepository,
    messageRepository: MessageRepository,
    timelineRepository: TimelineRepository,
    savedStateDataSource: SavedStateDataSource,
    writeQueue: WriteQueue,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER")
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    HomeStateHolder by scope.actionStateFlowMutator(
        initialState = State(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        inputs = listOf(
            timelineMutations(
                scope = scope,
                timelineRepository = timelineRepository,
                savedStateDataSource = savedStateDataSource,
            ),
            loadProfileMutations(
                authRepository,
            ),
            recentConversationMutations(
                messageRepository = messageRepository,
            ),
        ),
        actionTransform = transform@{ actions ->
            actions.toMutationStream(
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.UpdatePageWithUpdates -> action.flow.pageWithUpdateMutations()
                    is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                        writeQueue = writeQueue,
                    )
                    is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations()

                    is Action.RefreshCurrentTab -> action.flow.tabRefreshMutations(
                        stateHolder = this@transform,
                    )

                    is Action.UpdateTimeline -> action.flow.saveTimelinePreferencesMutations(
                        writeQueue = writeQueue,
                    )

                    is Action.SetCurrentTab -> action.flow.setCurrentTabMutations(savedStateDataSource)
                    is Action.SetTabLayout -> action.flow.setTabLayoutMutations()
                    is Action.Navigate -> action.flow.consumeNavigationActions(
                        navigationMutationConsumer = navActions,
                    )
                }
            }
        },
    )

private fun loadProfileMutations(
    authRepository: AuthRepository,
): Flow<Mutation<State>> =
    authRepository.signedInUser.mapToMutation {
        copy(signedInProfile = it)
    }

private fun timelineMutations(
    scope: CoroutineScope,
    timelineRepository: TimelineRepository,
    savedStateDataSource: SavedStateDataSource,
): Flow<Mutation<State>> =
    combine(
        savedStateDataSource.savedState.take(1),
        timelineRepository.homeTimelines(),
        ::Pair,
    ).mapToMutation { (savedState, homeTimelines) ->
        val tabUri = currentTabUri
            ?: savedState.signedInProfilePreferences()
                ?.lastViewedHomeTimelineUri
                ?.takeIf { uri ->
                    homeTimelines.any { it.isPinned && it.uri == uri }
                }
            ?: homeTimelines.firstOrNull()?.uri

        copy(
            currentTabUri = tabUri,
            timelines = homeTimelines,
            timelineStateHolders = homeTimelines.map { timeline ->
                val holder = timelineStateHolders
                    .firstOrNull { it.state.value.timeline.sourceId == timeline.sourceId }
                    ?.mutator
                    ?: scope.timelineStateHolder(
                        refreshOnStart = savedState.signedInProfilePreferences()
                            ?.refreshHomeTimelineOnLaunch == true,
                        timeline = timeline,
                        startNumColumns = 1,
                        timelineRepository = timelineRepository,
                    )

                if (timeline.isPinned)
                    HomeScreenStateHolders.Pinned(holder)
                else
                    HomeScreenStateHolders.Saved(holder)
            },
        )
    }

fun recentConversationMutations(
    messageRepository: MessageRepository,
): Flow<Mutation<State>> =
    messageRepository.recentConversations()
        .mapToMutation { conversations ->
            copy(recentConversations = conversations)
        }

private fun Flow<Action.UpdatePageWithUpdates>.pageWithUpdateMutations(): Flow<Mutation<State>> =
    mapToMutation { (sourceId, hasUpdates) ->
        copy(sourceIdsToHasUpdates = sourceIdsToHasUpdates + (sourceId to hasUpdates))
    }

@OptIn(ExperimentalUuidApi::class)
private fun Flow<Action.UpdateTimeline>.saveTimelinePreferencesMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapLatestToManyMutations {
        when (it) {
            Action.UpdateTimeline.RequestUpdate -> emit {
                copy(timelinePreferenceSaveRequestId = Uuid.random().toHexString())
            }

            is Action.UpdateTimeline.Update -> {
                val writable = Writable.TimelineUpdate(Timeline.Update.Bulk(it.timelines))
                writeQueue.enqueue(writable)
                writeQueue.awaitDequeue(writable)
                emit {
                    copy(
                        tabLayout = when (tabLayout) {
                            TabLayout.Collapsed.All -> tabLayout
                            TabLayout.Collapsed.Selected -> tabLayout
                            TabLayout.Expanded -> TabLayout.Collapsed.Selected
                        },
                    )
                }
            }
        }
    }

private fun Flow<Action.SendPostInteraction>.postInteractionMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        when (writeQueue.enqueue(Writable.Interaction(action.interaction))) {
            WriteQueue.Status.Dropped -> emit {
                copy(messages = messages + action.interaction.failedWriteMessage())
            }
            WriteQueue.Status.Duplicate -> emit {
                copy(messages = messages + action.interaction.duplicateWriteMessage())
            }
            WriteQueue.Status.Enqueued -> Unit
        }
    }

private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(messages = messages - action.message)
    }

private fun Flow<Action.SetCurrentTab>.setCurrentTabMutations(
    savedStateDataSource: SavedStateDataSource,
): Flow<Mutation<State>> = mapLatestToManyMutations { action ->
    // Write to memory in state immediately
    emit { copy(currentTabUri = action.currentTabUri) }

    // Wait until we're sure the user has settled on this tab
    delay(1400.milliseconds)
    // Write to disk
    savedStateDataSource.setLastViewedHomeTimelineUri(action.currentTabUri)
}

private fun Flow<Action.SetTabLayout>.setTabLayoutMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(tabLayout = action.layout)
    }

private fun Flow<Action.RefreshCurrentTab>.tabRefreshMutations(
    stateHolder: SuspendingStateHolder<State>,
): Flow<Mutation<State>> =
    mapToManyMutations {
        val currentState = stateHolder.state()
        currentState.timelineStateHolders
            .firstOrNull { it.state.value.timeline.uri == currentState.currentTabUri }
            ?.accept
            ?.invoke(
                TimelineState.Action.Tile(
                    tilingAction = TilingState.Action.Refresh,
                ),
            )
    }
