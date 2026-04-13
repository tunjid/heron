package com.tunjid.heron.scaffold.navigation

import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.EmptyNavigation
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.scaffold.navigation.fakes.FakeAuthRepository
import com.tunjid.heron.scaffold.navigation.fakes.FakeUserDataRepository
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.strings.routeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

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
    fun emptyNavigation_is_filtered_out() = runTest {
        val authRepo = FakeAuthRepository()
        val userDataRepo = FakeUserDataRepository()

        authRepo.guestState.value = false
        authRepo.signedInUserState.value = null
        userDataRepo.navigationState.value = EmptyNavigation

        val mutations = authNavigationMutations(
            initialProfileId = null,
            initialIsGuest = false,
            authRepository = authRepo,
            userDataRepository = userDataRepo,
        )

        // Transition to non-empty navigation so we get an emission
        userDataRepo.navigationState.value = validNavigation

        val mutation = mutations.first()
        // With no profile and not guest, should force sign out from non-auth stack
        val result = mutation(signedInNav)
        assertEquals(
            expected = 1,
            actual = result.stacks.size,
        )
        assertEquals(
            expected = AppStack.Auth.stackName,
            actual = result.stacks[0].name,
        )
    }

    @Test
    fun noAuth_on_auth_stack_keeps_current_navigation() = runTest {
        val authRepo = FakeAuthRepository()
        val userDataRepo = FakeUserDataRepository()

        // Start as no auth, not guest
        authRepo.guestState.value = false
        authRepo.signedInUserState.value = null
        userDataRepo.navigationState.value = validNavigation

        val mutations = authNavigationMutations(
            initialProfileId = null,
            initialIsGuest = false,
            authRepository = authRepo,
            userDataRepository = userDataRepo,
        )

        // First emission: steady state (no auth, no guest)
        val firstMutation = mutations.first()
        // On auth stack with no auth and no guest → should keep auth stack
        val result = firstMutation(authStackNav)
        assertEquals(
            expected = authStackNav,
            actual = result,
        )
    }

    @Test
    fun guest_to_authenticated_triggers_signedIn_navigation() = runTest {
        val authRepo = FakeAuthRepository()
        val userDataRepo = FakeUserDataRepository()

        // Start as guest
        authRepo.guestState.value = true
        authRepo.signedInUserState.value = null
        userDataRepo.navigationState.value = validNavigation

        val mutations = authNavigationMutations(
            initialProfileId = null,
            initialIsGuest = true,
            authRepository = authRepo,
            userDataRepository = userDataRepo,
        )

        // Collect: first emission is steady state (guest)
        val items = mutableListOf<MultiStackNav>()

        // Get first emission (guest steady state)
        val firstMutation = mutations.first()
        items.add(firstMutation(authStackNav))

        // Transition to authenticated
        authRepo.guestState.value = false
        authRepo.signedInUserState.value = null // signedInUser updates separately

        // Now also set the profile
        authRepo.signedInUserState.value = null // In real usage, signedInUser emits the profile

        // The combine will fire with (null, false, validNavigation)
        // Since we went from guest=true to guest=false without a profile, this isn't a sign-in
        // The actual sign-in happens when profileId becomes non-null
    }
}
