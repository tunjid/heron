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

package com.tunjid.heron.ui.scaffold.navigation

import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.di.AppMainScope
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.EmptyNavigation
import com.tunjid.heron.data.repository.InitialNavigation
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.ui.UiTokens
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.current
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParser
import dev.zacsweers.metro.Inject
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch

interface NavigationStateHolder : ActionSuspendingStateMutator<NavigationMutation, NavigationState>
typealias NavigationMutation = NavigationContext.() -> MultiStackNav

@Inject
class PersistedNavigationStateHolder(
    @AppMainScope
    appMainScope: CoroutineScope,
    userDataRepository: UserDataRepository,
    authRepository: AuthRepository,
    routeParser: RouteParser,
) : NavigationStateHolder,
    ActionSuspendingStateMutator<NavigationMutation, NavigationState> by appMainScope.actionSuspendingStateMutator(
        state = NavigationState.Immutable().toSnapshotMutable(),
        started = SharingStarted.Eagerly,
        producer = { state, navActions ->
            val startTime = TimeSource.Monotonic.markNow()

            // Restore saved nav from disk first
            val savedNavigation = userDataRepository.navigation
                // Wait for a non empty saved state to be read
                .first { it != InitialNavigation }

            val signedInProfile = authRepository.signedInUser.first()
            val isGuest = authRepository.isGuest.first()

            val multiStackNav = when {
                savedNavigation == EmptyNavigation -> NavigationState.SignedOut
                signedInProfile == null && !isGuest -> NavigationState.SignedOut
                isGuest -> routeParser.parseMultiStackNav(
                    navigation = savedNavigation,
                    isSignedIn = false,
                    isGuest = true,
                )
                else -> routeParser.parseMultiStackNav(
                    navigation = savedNavigation,
                    isSignedIn = true,
                ).let {
                    val wasInOauthFlow = it.current?.id?.contains(NavigationState.OAuthUrlPathSegment) == true
                    if (wasInOauthFlow) NavigationState.SignedOut else it
                }
            }

            val elapsed = startTime.elapsedNow()
            if (elapsed < UiTokens.splashScreenDuration) delay(UiTokens.splashScreenDuration - elapsed)

            state.multiStackNav = multiStackNav

            merge(
                navActions.mapToMutation { navMutation ->
                    navMutation(
                        ImmutableNavigationContext(
                            state = this,
                            routeParser = routeParser,
                        ),
                    )
                },
                authNavigationMutations(
                    initialProfileId = signedInProfile?.did,
                    initialIsGuest = isGuest,
                    authRepository = authRepository,
                    userDataRepository = userDataRepository,
                ),
            )
                .launchedCollect { navigationAction ->
                    state.multiStackNav = navigationAction(state.multiStackNav)

                    // Fire and forget, do not slow down the collector,
                    // navigation needs to be immediate.
                    appMainScope.launchPersistNavigationState(
                        navigationState = state.multiStackNav,
                        userDataRepository = userDataRepository,
                    )
                }
        },
    )

/**
 * A helper function for generic state producers to consume navigation actions
 */
fun <Action : NavigationAction, State> Flow<Action>.consumeNavigationActions(
    navigationMutationConsumer: (NavigationMutation) -> Unit,
) = flatMapLatest { action ->
    navigationMutationConsumer(action.navigationMutation)
    emptyFlow<Mutation<State>>()
}

internal fun authNavigationMutations(
    initialProfileId: ProfileId?,
    initialIsGuest: Boolean,
    authRepository: AuthRepository,
    userDataRepository: UserDataRepository,
): Flow<Mutation<MultiStackNav>> =
    combine(
        authRepository.signedInUser
            .map { it?.did }
            .distinctUntilChanged(),
        authRepository.isGuest,
        userDataRepository.navigation,
        ::AuthNavigationDigest,
    )
        .filter { it.navigation != EmptyNavigation }
        .scan(
            initial = AuthNavigationEventState(
                profileId = initialProfileId,
                isGuest = initialIsGuest,
            ),
            operation = AuthNavigationEventState::process,
        )
        .map(
            transform = AuthNavigationEventState::navigationMutation,
        )

private fun CoroutineScope.launchPersistNavigationState(
    navigationState: MultiStackNav,
    userDataRepository: UserDataRepository,
) = launch {
    if (navigationState != NavigationState.Initial) userDataRepository.persistNavigationState(
        navigation = navigationState.toSavedState(),
    )
}

internal fun RouteParser.parseMultiStackNav(
    navigation: SavedState.Navigation,
    isSignedIn: Boolean,
    isGuest: Boolean = false,
): MultiStackNav {
    val templateNav = when {
        isSignedIn || isGuest -> NavigationState.SignedIn
        else -> NavigationState.SignedOut
    }
    val restored = navigation.backStacks
        .foldIndexed(
            initial = MultiStackNav(
                name = templateNav.name,
            ),
            operation = { index, multiStackNav, routesForStack ->
                multiStackNav.copy(
                    stacks = multiStackNav.stacks +
                        routesForStack.fold(
                            initial = StackNav(
                                name = templateNav.stacks.getOrNull(index)?.name ?: "Unknown",
                            ),
                            operation = innerFold@{ stackNav, route ->
                                val resolvedRoute =
                                    parse(pathAndQueries = route) ?: unknownRoute()
                                stackNav.copy(
                                    children = stackNav.children + resolvedRoute,
                                )
                            },
                        ),
                )
            },
        )
        .copy(
            currentIndex = navigation.activeNav,
        )

    // Don't put a signed in or guest user on the sign in screen
    return if (restored.current?.id == AppStack.Auth.rootRoute.id) NavigationState.SignedIn
    else restored
}

private fun MultiStackNav.toSavedState() = SavedState.Navigation(
    activeNav = currentIndex,
    backStacks = stacks.fold(listOf()) { listOfLists, stackNav ->
        listOfLists.plus(
            element = stackNav.children
                .filterIsInstance<Route>()
                .fold(listOf()) { stackList, route ->
                    stackList + route.routeParams.pathAndQueries
                },
        )
    },
)

internal val MultiStackNav.isShowingSplashScreen
    get() = this == NavigationState.Initial

internal data class AuthNavigationDigest(
    val profileId: ProfileId?,
    val isGuest: Boolean,
    val navigation: SavedState.Navigation,
)

internal class AuthNavigationEventState(
    private var profileId: ProfileId?,
    private var isGuest: Boolean,
) {

    private var navigationSavedState: SavedState.Navigation = SavedState.Navigation()
    private val events = mutableSetOf<Event>()

    fun process(
        digest: AuthNavigationDigest,
    ): AuthNavigationEventState {
        val (currentProfileId, currentIsGuest, currentNavigation) = digest
        when {
            // Guest → Authenticated: treat as sign-in
            isGuest && !currentIsGuest && currentProfileId != null -> {
                events.add(Event.SignIn)
            }
            // Not guest → Guest: treat as sign-in so guest navigates off auth screen
            !isGuest && currentIsGuest -> {
                events.add(Event.SignIn)
            }
            // No auth, not guest → Authenticated: treat as sign-in
            profileId == null && !isGuest && currentProfileId != null -> {
                events.add(Event.SignIn)
            }
            // Authenticated → different Authenticated: session switch
            profileId != null && currentProfileId != null && currentProfileId != profileId -> {
                events.add(Event.SessionSwitch(currentNavigation.hashCode()))
            }
        }

        profileId = currentProfileId
        isGuest = currentIsGuest
        navigationSavedState = currentNavigation

        return this
    }

    fun navigationMutation(): Mutation<MultiStackNav> = {
        val isSignedIn = profileId != null
        val isOnAuthStack = stacks[currentIndex].name == AppStack.Auth.stackName

        val freshSignIn = events.remove(
            element = Event.SignIn,
        )
        // Don't compute the hash if not necessary
        val sessionSwitched = if (events.isNotEmpty()) events.remove(
            element = Event.SessionSwitch(navigationSavedState.hashCode()),
        ) else false

        // Wipe after processing
        events.clear()

        when {
            // Session switch or a fresh sign-in on the auth stack, reset to signed-in navigation
            sessionSwitched || (freshSignIn && isOnAuthStack) -> NavigationState.SignedIn
            // If signed in, guest, or already on the auth stack, keep navigation as is
            isSignedIn || isOnAuthStack || isGuest -> this
            // Otherwise, the user is not signed in and not on the auth stack, so force sign out
            else -> NavigationState.SignedOut
        }
    }

    private sealed interface Event {
        data object SignIn : Event
        data class SessionSwitch(
            val navigationHash: Int,
        ) : Event
    }
}
