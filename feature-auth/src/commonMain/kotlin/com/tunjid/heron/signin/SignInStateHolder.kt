/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// See YouTrack: KTIJ-18375
@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.tunjid.heron.signin


import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.scaffold.navigation.NavigationContext
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.mutator.mutationOf
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.canPop
import com.tunjid.treenav.pop
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.switch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias SignInStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Inject
class SignInStateHolderCreator(
    private val creator: (scope: CoroutineScope, route: Route) -> ActualSignInStateHolder
) : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route
    ): ActualSignInStateHolder = creator.invoke(scope, route)
}

@Inject
class ActualSignInStateHolder(
    authRepository: AuthRepository,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER")
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope), SignInStateHolder by scope.actionStateFlowMutator(
    initialState = State(),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
        authRepository.isSignedIn.map { mutationOf { copy(isSignedIn = it) } },
    ),
    actionTransform = { actions ->
        actions.toMutationStream {
            when (val action = type()) {
                is Action.FieldChanged -> action.flow.formEditMutations()
                is Action.MessageConsumed -> action.flow.messageConsumptionMutations()
                is Action.Submit -> action.flow.submissionMutations(
                    authRepository = authRepository,
                    navActions = navActions
                )

                is Action.Navigate -> action.flow.consumeNavigationActions(
                    navigationMutationConsumer = navActions
                )
            }
        }
    }
)

private fun Flow<Action.FieldChanged>.formEditMutations(): Flow<Mutation<State>> =
    mapToMutation { (updatedField) ->
        copy(fields = fields.update(updatedField))
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
    navActions: (NavigationMutation) -> Unit
): Flow<Mutation<State>> =
    debounce(200)
        .mapLatestToManyMutations { (request) ->
            emit { copy(isSubmitting = true) }
            when (val exception =
                authRepository.createSession(request = request).exceptionOrNull()) {
                null -> emit {
                    copy(messages = exception?.message?.let(messages::plus) ?: messages)
                }

                else -> navActions(NavigationContext::resetNav)
            }
            emit { copy(isSubmitting = false) }
        }

private fun NavigationContext.resetNav(): MultiStackNav {
    var newNav = navState
    for (i in 0.until(navState.stacks.size)) {
        newNav = newNav.switch(i)
        while (newNav.canGoUp) newNav = newNav.pop()
    }
    return newNav.switch(0)
}

private val MultiStackNav.canGoUp get() = stacks.getOrNull(currentIndex)?.canPop == true
