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

import androidx.lifecycle.ViewModel
import com.mikepenz.aboutlibraries.Libs
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import heron.feature.settings.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

internal typealias SettingsStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualSettingsViewModel
}

@AssistedInject
class ActualSettingsViewModel(
    authRepository: AuthRepository,
    userDataRepository: UserDataRepository,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER")
    @Assisted route: Route,
) : ViewModel(viewModelScope = scope),
    SettingsStateHolder by scope.actionStateFlowMutator(
        initialState = State(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        inputs = listOf(
            signedInProfileSavedStateMutations(
                userDataRepository = userDataRepository,
            ),
            loadOpenSourceLibraryMutations(),
        ),
        actionTransform = transform@{ actions ->
            actions.toMutationStream(
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations()

                    is Action.SetRefreshHomeTimelinesOnLaunch -> action.flow.homeTimelineRefreshOnLaunchMutations(
                        userDataRepository = userDataRepository,
                    )

                    is Action.SetDynamicThemingPreference -> action.flow.toggleDynamicTheming(
                        userDataRepository = userDataRepository,
                    )

                    is Action.SetCompactNavigation -> action.flow.toggleCompactNavigation(
                        userDataRepository = userDataRepository,
                    )

                    is Action.SetAutoHideBottomNavigation -> action.flow.toggleAutoHideBottomNavigation(
                        userDataRepository = userDataRepository,
                    )

                    is Action.Navigate -> action.flow.consumeNavigationActions(
                        navigationMutationConsumer = navActions,
                    )

                    Action.SignOut -> action.flow.mapToManyMutations {
                        authRepository.signOut()
                    }
                }
            }
        },
    )

fun signedInProfileSavedStateMutations(
    userDataRepository: UserDataRepository,
): Flow<Mutation<State>> =
    userDataRepository.preferences
        .mapToMutation {
            copy(signedInProfilePreferences = it)
        }

fun loadOpenSourceLibraryMutations(): Flow<Mutation<State>> = flow {
    val libs = withContext(Dispatchers.IO) {
        Libs.Builder()
            .withJson(Res.readBytes("files/aboutlibraries.json").decodeToString())
            .build()
    }
    emit { copy(openSourceLibraries = libs) }
}

private fun Flow<Action.SetRefreshHomeTimelinesOnLaunch>.homeTimelineRefreshOnLaunchMutations(
    userDataRepository: UserDataRepository,
): Flow<Mutation<State>> =
    mapToManyMutations { (refreshOnLaunch) ->
        userDataRepository.setRefreshedHomeTimelineOnLaunch(refreshOnLaunch)
    }

private fun Flow<Action.SetDynamicThemingPreference>.toggleDynamicTheming(
    userDataRepository: UserDataRepository,
): Flow<Mutation<State>> =
    mapToManyMutations { (dynamicTheming) ->
        userDataRepository.setDynamicTheming(dynamicTheming)
    }

private fun Flow<Action.SetCompactNavigation>.toggleCompactNavigation(
    userDataRepository: UserDataRepository,
): Flow<Mutation<State>> =
    mapToManyMutations { (compactNavigation) ->
        userDataRepository.setCompactNavigation(compactNavigation)
    }

private fun Flow<Action.SetAutoHideBottomNavigation>.toggleAutoHideBottomNavigation(
    userDataRepository: UserDataRepository,
): Flow<Mutation<State>> =
    mapToManyMutations { (persistentBottomNavigation) ->
        userDataRepository.setAutoHideBottomNavigation(persistentBottomNavigation)
    }

private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(messages = messages - action.message)
    }
