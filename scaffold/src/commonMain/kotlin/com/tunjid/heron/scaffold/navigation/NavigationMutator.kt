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

import com.tunjid.heron.data.repository.EmptySavedState
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.SavedStateRepository
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

interface NavigationStateHolder : ActionStateMutator<NavigationMutation, StateFlow<MultiStackNav>>
typealias NavigationMutation = NavigationContext.() -> MultiStackNav

/**
 * An action that causes mutations to navigation
 */
interface NavigationAction {
    val navigationMutation: NavigationMutation
}

private val InitialNavigationState = MultiStackNav(
    name = "splash-app",
    // TODO: Make this a splash screen
    stacks = listOf(
        StackNav(
            name = "splashStack",
            children = listOf(routeOf("/auth"))
        )
    )
)

private val SignedOutNavigationState = MultiStackNav(
    name = "signed-out-app",
    stacks = listOf(
        StackNav(
            name = "authStack",
            children = listOf(routeOf("/auth"))
        )
    )
)

private val SignedInNavigationState = MultiStackNav(
    name = "signed-in-app",
    stacks = listOf(
        StackNav(
            name = "homeStack",
            children = listOf(routeOf("/home"))
        ),
        StackNav(
            name = "searchStack",
            children = listOf(routeOf("/search"))
        ),
        StackNav(
            name = "messagesStack",
            children = listOf(routeOf("/messages"))
        ),
        StackNav(
            name = "notificationsStack",
            children = listOf(routeOf("/notifications"))
        ),
        StackNav(
            name = "homeStack",
            children = listOf(routeOf("/me"))
        ),
    )
)

@Inject
class PersistedNavigationStateHolder(
    appScope: CoroutineScope,
    savedStateRepository: SavedStateRepository,
    routeParser: RouteParser,
) : NavigationStateHolder,
    ActionStateMutator<NavigationMutation, StateFlow<MultiStackNav>> by appScope.actionStateFlowMutator(
        initialState = InitialNavigationState,
        started = SharingStarted.Eagerly,
        inputs = listOf(
            savedStateRepository.forceSignOutMutations()
        ),
        actionTransform = { navActions ->
            flow {
                // Restore saved nav from disk first
                val savedState = savedStateRepository.savedState
                    // Wait for a non empty saved state to be read
                    .first { it != EmptySavedState }

                val multiStackNav = routeParser.parseMultiStackNav(savedState)

                emit { multiStackNav }

                emitAll(
                    navActions.mapToMutation { navMutation ->
                        navMutation(
                            ImmutableNavigationContext(
                                state = this,
                                routeParser = routeParser
                            )
                        )
                    }
                )
            }
        },
        stateTransform = { navigationStateFlow ->
            // Save each new navigation state in parallel
            navigationStateFlow.onEach { navigationState ->
                appScope.persistNavigationState(
                    navigationState = navigationState,
                    savedStateRepository = savedStateRepository
                )
            }
        }
    )

@Suppress("UnusedReceiverParameter")
fun NavigationContext.resetAuthNavigation(): MultiStackNav =
    SignedInNavigationState

/**
 * A helper function for generic state producers to consume navigation actions
 */
fun <Action : NavigationAction, State> Flow<Action>.consumeNavigationActions(
    navigationMutationConsumer: (NavigationMutation) -> Unit
) = flatMapLatest { action ->
    navigationMutationConsumer(action.navigationMutation)
    emptyFlow<Mutation<State>>()
}

private fun SavedStateRepository.forceSignOutMutations(): Flow<Mutation<MultiStackNav>> =
    savedState
        // No auth token and is displaying main navigation
        .filter { it.auth == null && it != EmptySavedState }
        .mapToMutation { _ ->
            SignedOutNavigationState
        }

private fun CoroutineScope.persistNavigationState(
    navigationState: MultiStackNav,
    savedStateRepository: SavedStateRepository,
) = launch {
    if (navigationState != InitialNavigationState) savedStateRepository.updateState {
        this.copy(navigation = navigationState.toSavedState())
    }
}

private val SavedState.isSignedIn get() = auth != null

private fun RouteParser.parseMultiStackNav(savedState: SavedState) =
    savedState.navigation.backStacks
        .foldIndexed(
            initial = MultiStackNav(
                name = if (savedState.isSignedIn) SignedInNavigationState.name
                else SignedOutNavigationState.name
            ),
            operation = { index, multiStackNav, routesForStack ->
                multiStackNav.copy(
                    stacks = multiStackNav.stacks +
                            routesForStack.fold(
                                initial = StackNav(
                                    name = when {
                                        savedState.isSignedIn -> SignedInNavigationState
                                        else -> SignedOutNavigationState
                                    }.stacks.getOrNull(index)?.name ?: "Unknown"
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
            currentIndex = savedState.navigation.activeNav
        )

private fun MultiStackNav.toSavedState() = SavedState.Navigation(
    activeNav = currentIndex,
    backStacks = stacks.fold(listOf()) { listOfLists, stackNav ->
        listOfLists.plus(
            element = stackNav.children
                .filterIsInstance<Route>()
                .fold(listOf()) { stackList, route ->
                    stackList + route.routeParams.pathAndQueries
                }
        )
    },
)