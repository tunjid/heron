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

package com.tunjid.heron.settings

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.mikepenz.aboutlibraries.Libs
import com.tunjid.heron.data.core.models.SessionSummary
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.stateproduction.RouteStateHolder
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.switch_account_failed
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Stable
internal interface SettingsStateHolder :
    RouteStateHolder,
    ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface SettingsViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualSettingsViewModel
}

class ActualSettingsViewModel(
    mutator: ActionSuspendingStateMutator<Action, State>,
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    SettingsStateHolder,
    ActionSuspendingStateMutator<Action, State> by mutator {

    @AssistedInject
    constructor(
        authRepository: AuthRepository,
        userDataRepository: UserDataRepository,
        writeQueue: WriteQueue,
        navActions: (NavigationMutation) -> Unit,
        @Assisted scope: CoroutineScope,
        @Assisted route: Route,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = State().toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            producer = { state, actions ->
                launchSignedInProfileSavedStateMutations(
                    state = state,
                    userDataRepository = userDataRepository,
                )
                launchLoadOpenSourceLibraryMutations(
                    state = state,
                )
                launchLoadSessionSummaryMutations(
                    state = state,
                    authRepository = authRepository,
                )
                launchObserveActiveProfileMutations(
                    state = state,
                    authRepository = authRepository,
                )
                actions.launchMutationsIn(
                    productionScope = this,
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(
                            state = state,
                        )
                        is Action.UpdateSection -> action.flow.launchUpdateSectionMutations(
                            state = state,
                        )

                        is Action.SetRefreshHomeTimelinesOnLaunch -> action.flow.launchHomeTimelineRefreshOnLaunchMutations(
                            userDataRepository = userDataRepository,
                        )

                        is Action.SetAutoPlayTimelineVideos -> action.flow.launchTimelineVideoAutoPlayMutations(
                            userDataRepository = userDataRepository,
                        )

                        is Action.SetCurrentThemeOrdinal -> action.flow.launchSetCurrentThemeOrdinal(
                            userDataRepository = userDataRepository,
                        )

                        is Action.SetDarkThemeConfigOrdinal -> action.flow.launchSetDarkThemeConfigOrdinal(
                            userDataRepository = userDataRepository,
                        )

                        is Action.SetCompactNavigation -> action.flow.launchToggleCompactNavigation(
                            userDataRepository = userDataRepository,
                        )

                        is Action.SetAutoHideBottomNavigation -> action.flow.launchToggleAutoHideBottomNavigation(
                            userDataRepository = userDataRepository,
                        )

                        is Action.SetShowPostEngagementMetrics -> action.flow.launchToggleShowPostEngagementMetrics(
                            userDataRepository = userDataRepository,
                        )

                        is Action.SetShowTrendingTopics -> action.flow.launchToggleShowTrendingTopics(
                            userDataRepository = userDataRepository,
                        )

                        is Action.SetAllowAllTimelinePresentations -> action.flow.launchToggleAllowAllTimelinePresentations(
                            userDataRepository = userDataRepository,
                        )

                        is Action.Navigate -> action.flow.collect {
                            navActions(it.navigationMutation)
                        }
                        is Action.SwitchSession -> action.flow.launchHandleSwitchSessionMutations(
                            state = state,
                            authRepository = authRepository,
                        )
                        is Action.UpdateFeedPreference -> action.flow.launchUpdateFeedPreferenceMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                        is Action.UpdateThreadViewPreference -> action.flow.launchUpdateThreadPreferenceMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                        Action.SignOut -> action.flow.collect {
                            authRepository.signOut()
                        }
                    }
                }
            },
        ),
        scope = scope,
    )
}

context(productionScope: CoroutineScope)
fun launchSignedInProfileSavedStateMutations(
    state: State.SnapshotMutable,
    userDataRepository: UserDataRepository,
) = userDataRepository.preferences
    .launchedCollect {
        state.signedInProfilePreferences = it
    }

context(productionScope: CoroutineScope)
fun launchLoadOpenSourceLibraryMutations(
    state: State.SnapshotMutable,
) = flow {
    val libs = withContext(Dispatchers.IO) {
        Libs.Builder()
            .withJson(Res.readBytes("files/aboutlibraries.json").decodeToString())
            .build()
    }
    emit(libs)
}.launchedCollect {
    state.openSourceLibraries = it
}

context(productionScope: CoroutineScope)
private fun launchLoadSessionSummaryMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.pastSessions
    .launchedCollect { sessionSummaries ->
        state.pastSessions = sessionSummaries
    }

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateSection>.launchUpdateSectionMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.section = action.section
}

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateFeedPreference>.launchUpdateFeedPreferenceMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
        Writable.TimelineUpdate(
            update = Timeline.Update.OfFeedPreference.Add(
                action.feedPreference,
            ),
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateThreadViewPreference>.launchUpdateThreadPreferenceMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
        Writable.TimelineUpdate(
            update = Timeline.Update.OfThreadViewPreference.ThreadView(
                action.threadViewPreference,
            ),
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.SwitchSession>.launchHandleSwitchSessionMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = debounce(SwitchActionDebounce)
    .launchedCollectLatest {
        switchSessionMutation(
            state = state,
            authRepository = authRepository,
            sessionSummary = it.sessionSummary,
        )
    }

private suspend fun switchSessionMutation(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
    sessionSummary: SessionSummary,
) {
    state.switchPhase = AccountSwitchPhase.MORPHING
    state.switchingSession = sessionSummary

    delay(AccountSwitchPhase.MORPHING.changeDelay)

    state.switchPhase = AccountSwitchPhase.LOADING

    when (val outcome = authRepository.switchSession(sessionSummary)) {
        is Outcome.Success -> {
            state.switchPhase = AccountSwitchPhase.SUCCESS
            delay(AccountSwitchPhase.SUCCESS.changeDelay)
        }

        is Outcome.Failure -> {
            state.switchPhase = AccountSwitchPhase.IDLE
            state.switchingSession = null
            state.messages = state.messages.plus(
                outcome.exception.message?.let(Memo::Text)
                    ?: Memo.Resource(Res.string.switch_account_failed),
            ).distinct()
        }
    }
}

context(productionScope: CoroutineScope)
private fun launchObserveActiveProfileMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser
    .map { it?.did }
    .distinctUntilChanged()
    .launchedCollect { profileId ->
        state.activeProfileId = profileId
    }

context(productionScope: CoroutineScope)
private fun Flow<Action.SetRefreshHomeTimelinesOnLaunch>.launchHomeTimelineRefreshOnLaunchMutations(
    userDataRepository: UserDataRepository,
) = launchedCollect { (refreshOnLaunch) ->
    userDataRepository.setRefreshedHomeTimelineOnLaunch(refreshOnLaunch)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SetAutoPlayTimelineVideos>.launchTimelineVideoAutoPlayMutations(
    userDataRepository: UserDataRepository,
) = launchedCollect { (autoPlayTimelineVideos) ->
    userDataRepository.setAutoPlayTimelineVideos(autoPlayTimelineVideos)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SetCurrentThemeOrdinal>.launchSetCurrentThemeOrdinal(
    userDataRepository: UserDataRepository,
) = launchedCollect { (themeOrdinal) ->
    userDataRepository.setCurrentThemeOrdinal(themeOrdinal)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SetDarkThemeConfigOrdinal>.launchSetDarkThemeConfigOrdinal(
    userDataRepository: UserDataRepository,
) = launchedCollect { (darkThemeConfigOrdinal) ->
    userDataRepository.setDarkThemeConfigOrdinal(darkThemeConfigOrdinal)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SetCompactNavigation>.launchToggleCompactNavigation(
    userDataRepository: UserDataRepository,
) = launchedCollect { (compactNavigation) ->
    userDataRepository.setCompactNavigation(compactNavigation)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SetAutoHideBottomNavigation>.launchToggleAutoHideBottomNavigation(
    userDataRepository: UserDataRepository,
) = launchedCollect { (autoHideBottomNavigation) ->
    userDataRepository.setAutoHideBottomNavigation(autoHideBottomNavigation)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SetShowPostEngagementMetrics>.launchToggleShowPostEngagementMetrics(
    userDataRepository: UserDataRepository,
) = launchedCollect { (showPostEngagementMetrics) ->
    userDataRepository.setShowPostEngagementMetrics(showPostEngagementMetrics)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SetShowTrendingTopics>.launchToggleShowTrendingTopics(
    userDataRepository: UserDataRepository,
) = launchedCollect { (showTrendingTopics) ->
    userDataRepository.setShowTrendingTopics(showTrendingTopics)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SetAllowAllTimelinePresentations>.launchToggleAllowAllTimelinePresentations(
    userDataRepository: UserDataRepository,
) = launchedCollect { (allowAllTimelinePresentations) ->
    userDataRepository.setAllowAllTimelinePresentations(allowAllTimelinePresentations)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.messages -= action.message
}

private val AccountSwitchPhase.changeDelay
    get() = when (this) {
        AccountSwitchPhase.IDLE -> 0.milliseconds
        AccountSwitchPhase.MORPHING -> 180.milliseconds
        AccountSwitchPhase.SUCCESS -> 220.milliseconds
        AccountSwitchPhase.LOADING -> 0.milliseconds
    }

private val SwitchActionDebounce = 200.milliseconds
