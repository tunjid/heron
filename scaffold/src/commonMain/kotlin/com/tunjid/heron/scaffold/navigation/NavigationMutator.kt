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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Start
import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.repository.EmptySavedState
import com.tunjid.heron.data.repository.InitialSavedState
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.SavedStateRepository
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.pop
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.routeString
import heron.scaffold.generated.resources.Res
import heron.scaffold.generated.resources.auth
import heron.scaffold.generated.resources.home
import heron.scaffold.generated.resources.messages
import heron.scaffold.generated.resources.notifications
import heron.scaffold.generated.resources.profile
import heron.scaffold.generated.resources.search
import heron.scaffold.generated.resources.splash
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
import org.jetbrains.compose.resources.StringResource

interface NavigationStateHolder : ActionStateMutator<NavigationMutation, StateFlow<MultiStackNav>>
typealias NavigationMutation = NavigationContext.() -> MultiStackNav

/**
 * An action that causes mutations to navigation
 */
interface NavigationAction {
    val navigationMutation: NavigationMutation

    sealed class Common : NavigationAction {
        data object Pop : Common() {
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        }

        data class ToProfile(
            val profileId: Id,
            val profileAvatar: Uri?,
            val avatarSharedElementKey: String?,
        ) : Common() {
            override val navigationMutation: NavigationMutation = {
                routeString(
                    path = "/profile/${profileId.id}",
                    queryParams = mapOf(
                        "profileAvatar" to listOfNotNull(profileAvatar?.uri),
                        "avatarSharedElementKey" to listOfNotNull(avatarSharedElementKey),
                        "referringRoute" to currentRoute
                            .routeParams
                            .queryParams
                            .getOrElse("referringRoute", ::emptyList)
                    )
                )
                    .toRoute
                    .takeIf { it.id != currentRoute.id }
                    ?.let(navState::push)
                    ?: navState
            }
        }

        data class ToPost(
            val postUri: Uri,
            val postId: Id,
            val profileId: Id,
            val sharedElementPrefix: String,
        ) : Common() {
            override val navigationMutation: NavigationMutation = {
                navState.push(
                    routeString(
                        path = "/profile/${profileId.id}/post/${postId.id}",
                        queryParams = mapOf(
                            "postUri" to listOf(postUri.uri),
                            "sharedElementPrefix" to listOf(sharedElementPrefix),
                        )
                    ).toRoute
                )
            }
        }
    }
}

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
                    .first { it != InitialSavedState }

                val multiStackNav =
                    if (savedState == EmptySavedState) SignedOutNavigationState
                    else routeParser.parseMultiStackNav(savedState)

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

private val InitialNavigationState = MultiStackNav(
    name = "splash-app",
    stacks = listOf(
        StackNav(
            name = AppStack.Splash.stackName,
            children = listOf(routeOf("/splash"))
        )
    )
)

private val SignedOutNavigationState = MultiStackNav(
    name = "signed-out-app",
    stacks = listOf(
        StackNav(
            name = AppStack.Auth.stackName,
            children = listOf(routeOf("/auth"))
        )
    )
)

private val SignedInNavigationState = MultiStackNav(
    name = "signed-in-app",
    stacks = listOf(
        StackNav(
            name = AppStack.Home.stackName,
            children = listOf(routeOf("/home"))
        ),
        StackNav(
            name = AppStack.Search.stackName,
            children = listOf(routeOf("/search"))
        ),
        StackNav(
            name = AppStack.Messages.stackName,
            children = listOf(routeOf("/messages"))
        ),
        StackNav(
            name = AppStack.Notifications.stackName,
            children = listOf(routeOf("/notifications"))
        ),
        StackNav(
            name = AppStack.Profile.stackName,
            children = listOf(routeOf("/me"))
        ),
    )
)


internal enum class AppStack(
    val stackName: String,
    val titleRes: StringResource,
    val icon: ImageVector,
) {
    Home(
        stackName = "home-stack",
        titleRes = Res.string.home,
        icon = Icons.Rounded.Home,
    ),
    Search(
        stackName = "search-stack",
        titleRes = Res.string.search,
        icon = Icons.Rounded.Search,
    ),
    Messages(
        stackName = "messages-stack",
        titleRes = Res.string.messages,
        icon = Icons.AutoMirrored.Rounded.Message,
    ),
    Notifications(
        stackName = "notifications-stack",
        titleRes = Res.string.notifications,
        icon = Icons.Rounded.Notifications,
    ),
    Profile(
        stackName = "profile-stack",
        titleRes = Res.string.profile,
        icon = Icons.Rounded.AccountCircle,
    ),
    Auth(
        stackName = "auth-stack",
        titleRes = Res.string.auth,
        icon = Icons.Rounded.Lock,
    ),
    Splash(
        stackName = "splash-stack",
        titleRes = Res.string.splash,
        icon = Icons.Rounded.Start,
    );
}