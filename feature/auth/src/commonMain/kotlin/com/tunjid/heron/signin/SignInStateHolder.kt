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

import com.tunjid.heron.data.core.models.OauthUriRequest
import com.tunjid.heron.data.core.models.Server
import com.tunjid.heron.data.core.models.SessionRequest
import com.tunjid.heron.data.core.models.normalized
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.signin.di.iss
import com.tunjid.heron.signin.oauth.OauthFlowResult
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.stateproduction.RouteViewModel
import com.tunjid.heron.ui.text.Memo
import com.tunjid.heron.ui.text.copyWithValidation
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import heron.feature.auth.generated.resources.Res
import heron.feature.auth.generated.resources.oauth_flow_failed
import heron.feature.auth.generated.resources.oauth_start_error
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn

internal interface SignInStateHolder : ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface SignInViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualSignInViewModel
}

class ActualSignInViewModel(
    mutator: ActionSuspendingStateMutator<Action, State>,
    scope: CoroutineScope,
    route: Route,
) : RouteViewModel(scope, route),
    SignInStateHolder,
    ActionSuspendingStateMutator<Action, State> by mutator {

    @AssistedInject
    constructor(
        authRepository: AuthRepository,
        navActions: (NavigationMutation) -> Unit,
        @Assisted scope: CoroutineScope,
        @Assisted route: Route,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = State().toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            producer = { state, actions ->
                launchPastSessionMutations(
                    state = state,
                    authRepository = authRepository,
                )
                launchIsSignedInMutations(
                    state = state,
                    authRepository = authRepository,
                )
                launchAuthDeeplinkMutations(
                    state = state,
                    route = route,
                    authRepository = authRepository,
                )
                actions.launchMutationsIn(
                    productionScope = this,
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.FieldChanged -> action.flow.launchFormEditMutations(
                            state = state,
                            authRepository = authRepository,
                        )
                        is Action.TogglePasswordPreference -> action.flow.launchPasswordPreferenceMutations(
                            state = state,
                        )
                        is Action.MessageConsumed -> action.flow.launchMessageConsumptionMutations(
                            state = state,
                        )
                        is Action.CreateSession -> action.flow.launchSubmissionMutations(
                            state = state,
                            authRepository = authRepository,
                        )
                        is Action.BeginOauthFlow -> action.flow.launchBeginOauthMutations(
                            state = state,
                            authRepository = authRepository,
                        )
                        is Action.OauthFlowResultAvailable -> action.flow.launchOauthFlowResultMutations(
                            state = state,
                            authRepository = authRepository,
                        )
                        is Action.OauthAvailabilityChanged -> action.flow.launchOauthAvailabilityChangedMutations(
                            state = state,
                        )
                        is Action.SetServer -> action.flow.launchSetServerMutations(
                            state = state,
                        )
                        is Action.Navigate -> action.flow.collect {
                            navActions(it.navigationMutation)
                        }
                    }
                }
            },
        ),
        scope = scope,
        route = route,
    )
}

context(productionScope: CoroutineScope)
private fun launchIsSignedInMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.isSignedIn
    .launchedCollect {
        state.isSignedIn = it
    }

context(productionScope: CoroutineScope)
private fun launchPastSessionMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.pastSessions
    .launchedCollect { pastSessions ->
        val mostRecentSession = pastSessions.firstOrNull()
        state.fields =
            if (state.fields != InitialFields || mostRecentSession == null) state.fields
            else state.fields.map { field ->
                if (field.id != Username) field
                else field.copy(value = mostRecentSession.profileHandle.id)
            }
        state.pastSessions = pastSessions
    }

context(productionScope: CoroutineScope)
private fun launchAuthDeeplinkMutations(
    state: State.SnapshotMutable,
    route: Route,
    authRepository: AuthRepository,
) = flow<Unit> {
    if (route.routeParams.queryParams.isEmpty()) return@flow
    val oauthTokenIssuer = route.iss ?: return@flow

    val pathAndQueries = route.routeParams.pathAndQueries

    createSessionMutations(
        state = state,
        request = SessionRequest.Oauth(
            callbackUri = GenericUri("$PLACEHOLDER_OAUTH_HOST$pathAndQueries"),
            server = Server(
                endpoint = oauthTokenIssuer,
                supportsOauth = true,
            ),
        ),
        authRepository = authRepository,
    )
}.launchedCollect { }

context(productionScope: CoroutineScope)
private fun Flow<Action.FieldChanged>.launchFormEditMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) {
    val shared = shareIn(
        scope = productionScope,
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        replay = 1,
    )
    shared.launchedCollect { (id, text) ->
        state.fields = state.fields.copyWithValidation(id, text)
    }
    shared.filter { it.id == Username }
        .debounce(HandleResolutionDebounce)
        .launchedCollectLatest { (_, text) ->
            state.isResolvingServer = DomainRegex.matches(text)
            if (!state.isResolvingServer) return@launchedCollectLatest
            try {
                val server = authRepository.resolveServer(ProfileHandle(text))
                    .getOrNull()
                    ?.normalized()

                state.isServerResolvedFromHandle = server != null
                state.selectedServer = server ?: state.selectedServer
                state.availableServers = when (server) {
                    null -> StartingServers
                    else -> listOf(server)
                }
            } finally {
                state.isResolvingServer = false
            }
        }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SetServer>.launchSetServerMutations(
    state: State.SnapshotMutable,
) = launchedCollect { (server) ->
    state.isServerResolvedFromHandle = false
    state.selectedServer = server
    state.availableServers = when (server) {
        // Do not accidentally duplicate a custom server with an
        // endpoint that is the same as a known server
        in Server.KnownServers -> StartingServers
        // Add the custom server as the last server
        else -> Server.KnownServers.toList() + server
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.OauthAvailabilityChanged>.launchOauthAvailabilityChangedMutations(
    state: State.SnapshotMutable,
) = launchedCollect { (isOauthAvailable) ->
    state.isOauthAvailable = isOauthAvailable
}

context(productionScope: CoroutineScope)
private fun Flow<Action.OauthFlowResultAvailable>.launchOauthFlowResultMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = launchedCollectLatest { action ->
    when (val result = action.result) {
        OauthFlowResult.Failure ->
            state.messages += Memo.Resource(Res.string.oauth_flow_failed)
        is OauthFlowResult.Success ->
            createSessionMutations(
                state = state,
                request = SessionRequest.Oauth(
                    callbackUri = result.callbackUri,
                    server = Server(
                        endpoint = result.issuer,
                        supportsOauth = true,
                    ),
                ),
                authRepository = authRepository,
            )
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.BeginOauthFlow>.launchBeginOauthMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = launchedCollectLatest {
    val result = authRepository.oauthRequestUri(
        request = OauthUriRequest(
            handle = it.handle,
            server = it.server,
        ),
    )
    result.fold(
        onSuccess = {
            state.oauthRequestUri = it
        },
        onFailure = {
            state.messages += Memo.Resource(Res.string.oauth_start_error)
        },
    )
}

/**
 * Mutations from consuming messages from the message queue
 */
context(productionScope: CoroutineScope)
private fun Flow<Action.MessageConsumed>.launchMessageConsumptionMutations(
    state: State.SnapshotMutable,
) = launchedCollect { (message) ->
    state.messages -= message
}

context(productionScope: CoroutineScope)
private fun Flow<Action.TogglePasswordPreference>.launchPasswordPreferenceMutations(
    state: State.SnapshotMutable,
) = launchedCollect {
    val preferPassword = !state.prefersPassword
    state.prefersPassword = preferPassword
    state.fields =
        if (preferPassword) state.fields
        else state.fields.map { field ->
            // Clear password when using oauth flow
            if (field.id == Password) field.copy(value = "")
            else field
        }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.CreateSession>.launchSubmissionMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = debounce(SubmissionDebounce)
    .launchedCollectLatest { (sessionRequest) ->
        createSessionMutations(
            state = state,
            request = sessionRequest,
            authRepository = authRepository,
        )
    }

private suspend fun createSessionMutations(
    state: State.SnapshotMutable,
    request: SessionRequest,
    authRepository: AuthRepository,
) {
    state.isSubmitting = true
    when (val outcome = authRepository.createSession(request)) {
        is Outcome.Success -> Unit
        is Outcome.Failure -> state.messages = state.messages.plus(
            outcome.exception.message
                ?.let(Memo::Text)
                ?: Memo.Resource(Res.string.oauth_flow_failed),
        )
            .distinct()
    }
    state.isSubmitting = false
}

private val SubmissionDebounce = 200.milliseconds
private val HandleResolutionDebounce = 500.milliseconds
private const val PLACEHOLDER_OAUTH_HOST = "https://placeholder.com"
