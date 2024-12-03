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


package com.tunjid.heron.scaffold.navigation

import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.SavedStateRepository
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.mutationOf
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.strings.RouteParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

interface NavigationStateHolder : ActionStateMutator<NavigationMutation, StateFlow<MultiStackNav>>
typealias NavigationMutation = NavigationContext.() -> MultiStackNav

/**
 * An action that causes mutations to navigation
 */
interface NavigationAction {
    val navigationMutation: NavigationMutation
}

private val SignedOutNavigationState = MultiStackNav(
    name = "emptyMultiStack",
    stacks = listOf(
        StackNav(
            name = "authStack",
            children = listOf(routeOf("/auth"))
        )
    )
)

@Inject
class PersistedNavigationStateHolder(
    appScope: CoroutineScope,
    savedStateRepository: SavedStateRepository,
    routeParser: RouteParser,
) : NavigationStateHolder,
    ActionStateMutator<NavigationMutation, StateFlow<MultiStackNav>> by appScope.actionStateFlowMutator(
        initialState = SignedOutNavigationState,
        started = SharingStarted.Eagerly,
        actionTransform = { navMutations ->
            flow {
                // Restore saved nav from disk first
                val savedNavigationState = savedStateRepository.savedState
                    .map { it.navigation }
                    .first { it.backStacks.isNotEmpty() }
                val multiStackNav = routeParser.parseMultiStackNav(savedNavigationState)

                emit { multiStackNav }
                emitAll(
                    navMutations.map { navMutation ->
                        mutationOf {
                            navMutation(
                                ImmutableNavigationContext(
                                    state = this,
                                    routeParser = routeParser
                                )
                            )
                        }
                    }
                )
            }
        },
    )

/**
 * A helper function for generic state producers to consume navigation actions
 */
fun <Action : NavigationAction, State> Flow<Action>.consumeNavigationActions(
    navigationMutationConsumer: (NavigationMutation) -> Unit
) = flatMapLatest { action ->
    navigationMutationConsumer(action.navigationMutation)
    emptyFlow<Mutation<State>>()
}

private fun RouteParser.parseMultiStackNav(savedState: SavedState.Navigation) =
    savedState.backStacks
        .fold(
            initial = MultiStackNav(name = "AppNav"),
            operation = { multiStackNav, routesForStack ->
                multiStackNav.copy(
                    stacks = multiStackNav.stacks +
                            routesForStack.fold(
                                initial = StackNav(
                                    name = routesForStack.firstOrNull() ?: "Unknown"
                                ),
                                operation = innerFold@{ stackNav, route ->
                                    val resolvedRoute =
                                        parse(pathAndQueries = route) ?: unknownRoute()
                                    stackNav.copy(
                                        children = stackNav.children + resolvedRoute
                                    )
                                }
                            )
                )
            }
        )
        .copy(
            currentIndex = savedState.activeNav
        )