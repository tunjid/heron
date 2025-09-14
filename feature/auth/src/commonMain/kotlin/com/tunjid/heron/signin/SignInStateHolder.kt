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

package com.tunjid.heron.signin

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.local.models.SessionRequest
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationContext
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.scaffold.navigation.resetAuthNavigation
import com.tunjid.heron.scaffold.scaffold.SnackbarMessage
import com.tunjid.heron.signin.oauth.OauthFlowResult
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.mutator.mutationOf
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import heron.feature.auth.generated.resources.Res
import heron.feature.auth.generated.resources.oauth_flow_failed
import heron.feature.auth.generated.resources.oauth_start_error
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map

internal typealias SignInStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualSignInViewModel
}

@Inject
class ActualSignInViewModel(
    authRepository: AuthRepository,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER")
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    SignInStateHolder by scope.actionStateFlowMutator(
        initialState = State(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        inputs = listOf(
            authRepository.isSignedIn.map { mutationOf { copy(isSignedIn = it) } },
        ),
        actionTransform = { actions ->
            actions.toMutationStream(
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.FieldChanged -> action.flow.formEditMutations()
                    is Action.MessageConsumed -> action.flow.messageConsumptionMutations()
                    is Action.Submit -> action.flow.submissionMutations(
                        authRepository = authRepository,
                        navActions = navActions,
                    )
                    is Action.BeginOauthFlow -> action.flow.beginOauthMutations(
                        authRepository = authRepository,
                    )
                    is Action.OauthFlowResultAvailable -> action.flow.oauthFlowResultMutations(
                        authRepository = authRepository,
                    )
                    is Action.OauthAvailabilityChanged -> action.flow.oauthAvailabilityChangedMutations()
                    is Action.SetAuthMode -> action.flow.setAuthModeMutations()
                    is Action.Navigate -> action.flow.consumeNavigationActions(
                        navigationMutationConsumer = navActions,
                    )
                }
            }
        },
    )

private fun Flow<Action.FieldChanged>.formEditMutations(): Flow<Mutation<State>> =
    mapToMutation { (updatedField) ->
        copy(fields = fields.update(updatedField))
    }

private fun Flow<Action.SetAuthMode>.setAuthModeMutations(): Flow<Mutation<State>> =
    mapToMutation { (authMode) ->
        copy(authMode = authMode)
    }

private fun Flow<Action.OauthAvailabilityChanged>.oauthAvailabilityChangedMutations(): Flow<Mutation<State>> =
    mapToMutation { (isOauthAvailable) ->
        copy(
            isOauthAvailable = isOauthAvailable,
            authMode = when (authMode) {
                AuthMode.UserSelectable.Oauth ->
                    if (!isOauthAvailable) AuthMode.UserSelectable.Password
                    else authMode
                AuthMode.UserSelectable.Password -> authMode
                AuthMode.Undecided ->
                    if (isOauthAvailable) AuthMode.UserSelectable.Oauth
                    else AuthMode.UserSelectable.Password
            },
        )
    }

private fun Flow<Action.OauthFlowResultAvailable>.oauthFlowResultMutations(
    authRepository: AuthRepository,
): Flow<Mutation<State>> =
    mapLatestToManyMutations { action ->
        when (val result = action.result) {
            OauthFlowResult.Failure -> emit {
                copy(
                    messages = messages + SnackbarMessage.Resource(Res.string.oauth_flow_failed)
                )
            }
            is OauthFlowResult.Success -> {
                authRepository.createSession(
                    SessionRequest.Oauth(
                        handle = action.handle,
                        code = result.code,
                    ),
                )
            }
        }
    }

private fun Flow<Action.BeginOauthFlow>.beginOauthMutations(
    authRepository: AuthRepository,
): Flow<Mutation<State>> =
    mapLatestToManyMutations {
        val result = authRepository.oauthRequestUri(it.handle)
        result.fold(
            onSuccess = {
                emit { copy(oauthRequestUri = it) }
            },
            onFailure = {
                emit {
                    copy(messages = messages + SnackbarMessage.Resource(Res.string.oauth_start_error))
                }
            },
        )
    }

/**
 * Mutations from consuming messages from the message queue
 */
private fun Flow<Action.MessageConsumed>.messageConsumptionMutations(): Flow<Mutation<State>> =
    mapToMutation { (message) ->
        copy(messages = messages - message)
    }

private fun Flow<Action.Submit>.submissionMutations(
    authRepository: AuthRepository,
    navActions: (NavigationMutation) -> Unit,
): Flow<Mutation<State>> =
    debounce(200)
        .mapLatestToManyMutations { action ->
            emit { copy(isSubmitting = true) }
            val exception = when (action) {
                is Action.Submit.Auth -> {
                    authRepository.createSession(action.request).exceptionOrNull()
                }
                Action.Submit.GuestAuth -> {
                    authRepository.guestSignIn()
                    null
                }
            }
            when (exception) {
                null -> navActions(NavigationContext::resetAuthNavigation)
                else -> emit {
                    copy(
                        messages = exception.message
                            ?.let(SnackbarMessage::Text)
                            ?.let(messages::plus)
                            ?.distinct()
                            ?: messages,
                    )
                }
            }
            emit { copy(isSubmitting = false) }
        }
