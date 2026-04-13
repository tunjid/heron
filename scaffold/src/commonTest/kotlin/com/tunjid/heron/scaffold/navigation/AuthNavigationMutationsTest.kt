package com.tunjid.heron.scaffold.navigation

import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.scaffold.navigation.fakes.FakeAuthRepository
import com.tunjid.heron.scaffold.navigation.fakes.FakeUserDataRepository
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.strings.routeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AuthNavigationMutationsTest {

    private val profileA = ProfileId("did:plc:user-a")

    private val authStackNav = MultiStackNav(
        name = "test",
        stacks = listOf(
            StackNav(
                name = AppStack.Auth.stackName,
                children = listOf(routeOf("/auth")),
            ),
        ),
    )

    private val signedInNav = MultiStackNav(
        name = "test",
        stacks = listOf(
            StackNav(
                name = AppStack.Home.stackName,
                children = listOf(routeOf("/home")),
            ),
        ),
    )

    private val validNavigation = SavedState.Navigation(
        activeNav = 0,
        backStacks = listOf(listOf("/auth")),
    )

    @Test
    fun noAuth_on_auth_stack_keeps_current_navigation() =
        runTest(UnconfinedTestDispatcher()) {
            val authRepo = FakeAuthRepository()
            val userDataRepo = FakeUserDataRepository()

            authRepo.guestState.value = false
            authRepo.signedInUserState.value = null
            userDataRepo.navigationState.value = validNavigation

            val result = async {
                authNavigationMutations(
                    initialProfileId = null,
                    initialIsGuest = false,
                    authRepository = authRepo,
                    userDataRepository = userDataRepo,
                )
                    .runningFold(authStackNav) { nav, mutation -> mutation(nav) }
                    .drop(1) // skip runningFold initial
                    .first { it == authStackNav }
            }

            assertEquals(
                expected = authStackNav,
                actual = result.await(),
            )
        }

    @Test
    fun noAuth_to_guest_navigates_to_signedIn() =
        runTest(UnconfinedTestDispatcher()) {
            val authRepo = FakeAuthRepository()
            val userDataRepo = FakeUserDataRepository()

            authRepo.guestState.value = false
            authRepo.signedInUserState.value = null
            userDataRepo.navigationState.value = validNavigation

            val result = async {
                authNavigationMutations(
                    initialProfileId = null,
                    initialIsGuest = false,
                    authRepository = authRepo,
                    userDataRepository = userDataRepo,
                )
                    .runningFold(authStackNav) { nav, mutation -> mutation(nav) }
                    .drop(1)
                    .first { it.stacks.size == 4 }
            }

            // Transition to guest
            authRepo.guestState.value = true

            val nav = result.await()
            assertEquals(
                expected = 4,
                actual = nav.stacks.size,
            )
            assertEquals(
                expected = AppStack.Home.stackName,
                actual = nav.stacks[0].name,
            )
        }

    @Test
    fun guest_to_authenticated_navigates_to_signedIn() =
        runTest(UnconfinedTestDispatcher()) {
            val authRepo = FakeAuthRepository()
            val userDataRepo = FakeUserDataRepository()

            authRepo.guestState.value = true
            authRepo.signedInUserState.value = null
            userDataRepo.navigationState.value = validNavigation

            val result = async {
                authNavigationMutations(
                    initialProfileId = null,
                    initialIsGuest = true,
                    authRepository = authRepo,
                    userDataRepository = userDataRepo,
                )
                    .runningFold(authStackNav) { nav, mutation -> mutation(nav) }
                    .drop(1)
                    .first { it.stacks.size == 4 }
            }

            // Guest becomes authenticated
            authRepo.guestState.value = false
            authRepo.signedInUserState.value = stubProfile(
                did = profileA,
                handle = ProfileHandle("alice.bsky.social"),
            )

            val nav = result.await()
            assertEquals(
                expected = 4,
                actual = nav.stacks.size,
            )
            assertEquals(
                expected = AppStack.Home.stackName,
                actual = nav.stacks[0].name,
            )
        }

    @Test
    fun signOut_forces_signedOut_navigation() =
        runTest(UnconfinedTestDispatcher()) {
            val authRepo = FakeAuthRepository()
            val userDataRepo = FakeUserDataRepository()

            authRepo.guestState.value = false
            authRepo.signedInUserState.value = stubProfile(
                did = profileA,
                handle = ProfileHandle("alice.bsky.social"),
            )
            userDataRepo.navigationState.value = validNavigation

            val result = async {
                authNavigationMutations(
                    initialProfileId = profileA,
                    initialIsGuest = false,
                    authRepository = authRepo,
                    userDataRepository = userDataRepo,
                )
                    .runningFold(signedInNav) { nav, mutation -> mutation(nav) }
                    .drop(1)
                    .first { it.stacks[0].name == AppStack.Auth.stackName }
            }

            // Sign out
            authRepo.signedInUserState.value = null

            val nav = result.await()
            assertEquals(
                expected = 1,
                actual = nav.stacks.size,
            )
            assertEquals(
                expected = AppStack.Auth.stackName,
                actual = nav.stacks[0].name,
            )
        }
}
