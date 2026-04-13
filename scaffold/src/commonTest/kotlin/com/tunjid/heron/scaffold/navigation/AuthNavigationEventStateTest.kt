package com.tunjid.heron.scaffold.navigation

import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.strings.routeOf
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthNavigationEventStateTest {

    private val profileA = ProfileId("did:plc:user-a")
    private val profileB = ProfileId("did:plc:user-b")

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

    private val savedNavigation = SavedState.Navigation(
        activeNav = 0,
        backStacks = listOf(listOf("/auth")),
    )

    private val deepSignedInNav = MultiStackNav(
        name = "test",
        stacks = listOf(
            StackNav(
                name = AppStack.Home.stackName,
                children = listOf(
                    routeOf("/home"),
                    routeOf("/profile/alice.bsky.social"),
                    routeOf("/profile/alice.bsky.social/post/3abc123"),
                ),
            ),
            StackNav(
                name = AppStack.Search.stackName,
                children = listOf(
                    routeOf("/search"),
                    routeOf("/profile/bob.bsky.social"),
                ),
            ),
            StackNav(
                name = AppStack.Messages.stackName,
                children = listOf(routeOf("/messages")),
            ),
            StackNav(
                name = AppStack.Notifications.stackName,
                children = listOf(routeOf("/notifications")),
            ),
        ),
        currentIndex = 1,
    )

    private val deepSavedNavigation = SavedState.Navigation(
        activeNav = 1,
        backStacks = listOf(
            listOf("/home", "/profile/alice.bsky.social", "/profile/alice.bsky.social/post/3abc123"),
            listOf("/search", "/profile/bob.bsky.social"),
            listOf("/messages"),
            listOf("/notifications"),
        ),
    )

    // region No auth → Guest

    @Test
    fun noAuth_to_guest_triggers_signIn_on_auth_stack() {
        val state = AuthNavigationEventState(
            profileId = null,
            isGuest = false,
        )

        // Transition to guest
        state.process(
            AuthNavigationDigest(
                profileId = null,
                isGuest = true,
                navigation = savedNavigation,
            ),
        )

        // Apply mutation to auth stack nav - should navigate to signed-in nav
        val result = state.navigationMutation().invoke(authStackNav)
        assertEquals(
            expected = 4,
            actual = result.stacks.size,
        )
        assertEquals(
            expected = AppStack.Home.stackName,
            actual = result.stacks[0].name,
        )
    }

    @Test
    fun noAuth_to_guest_on_non_auth_stack_keeps_current() {
        val state = AuthNavigationEventState(
            profileId = null,
            isGuest = false,
        )

        // Transition to guest
        state.process(
            AuthNavigationDigest(
                profileId = null,
                isGuest = true,
                navigation = savedNavigation,
            ),
        )

        // Already on non-auth stack — freshSignIn true but not on auth stack
        val result = state.navigationMutation().invoke(signedInNav)
        assertEquals(
            expected = signedInNav,
            actual = result,
        )
    }

    @Test
    fun noAuth_to_guest_on_deep_non_auth_stack_keeps_current() {
        val state = AuthNavigationEventState(
            profileId = null,
            isGuest = false,
        )

        state.process(
            AuthNavigationDigest(
                profileId = null,
                isGuest = true,
                navigation = deepSavedNavigation,
            ),
        )

        // Deep signed-in nav with back stacks should be preserved as-is
        val result = state.navigationMutation().invoke(deepSignedInNav)
        assertEquals(
            expected = deepSignedInNav,
            actual = result,
        )
    }

    // endregion

    // region No auth → Authenticated

    @Test
    fun noAuth_to_authenticated_triggers_signIn_on_auth_stack() {
        val state = AuthNavigationEventState(
            profileId = null,
            isGuest = false,
        )

        state.process(
            AuthNavigationDigest(
                profileId = profileA,
                isGuest = false,
                navigation = savedNavigation,
            ),
        )

        val result = state.navigationMutation().invoke(authStackNav)
        // Should navigate to signed-in nav (4 stacks)
        assertEquals(
            expected = 4,
            actual = result.stacks.size,
        )
        assertEquals(
            expected = AppStack.Home.stackName,
            actual = result.stacks[0].name,
        )
    }

    @Test
    fun noAuth_to_authenticated_on_non_auth_stack_keeps_current() {
        val state = AuthNavigationEventState(
            profileId = null,
            isGuest = false,
        )

        state.process(
            AuthNavigationDigest(
                profileId = profileA,
                isGuest = false,
                navigation = savedNavigation,
            ),
        )

        // Already on signed-in nav (not auth stack) — freshSignIn true but not on auth stack
        val result = state.navigationMutation().invoke(signedInNav)
        assertEquals(
            expected = signedInNav,
            actual = result,
        )
    }

    @Test
    fun noAuth_to_authenticated_on_deep_non_auth_stack_keeps_current() {
        val state = AuthNavigationEventState(
            profileId = null,
            isGuest = false,
        )

        state.process(
            AuthNavigationDigest(
                profileId = profileA,
                isGuest = false,
                navigation = deepSavedNavigation,
            ),
        )

        // Deep signed-in nav with back stacks should be preserved as-is
        val result = state.navigationMutation().invoke(deepSignedInNav)
        assertEquals(
            expected = deepSignedInNav,
            actual = result,
        )
    }

    // endregion

    // region Guest → Authenticated

    @Test
    fun guest_to_authenticated_triggers_signIn_on_auth_stack() {
        val state = AuthNavigationEventState(
            profileId = null,
            isGuest = true,
        )

        // Transition from guest to authenticated
        state.process(
            AuthNavigationDigest(
                profileId = profileA,
                isGuest = false,
                navigation = savedNavigation,
            ),
        )

        val result = state.navigationMutation().invoke(authStackNav)
        assertEquals(
            expected = 4,
            actual = result.stacks.size,
        )
        assertEquals(
            expected = AppStack.Home.stackName,
            actual = result.stacks[0].name,
        )
    }

    // endregion

    // region Authenticated → different Authenticated (session switch)

    @Test
    fun session_switch_resets_to_signedIn_navigation() {
        val state = AuthNavigationEventState(
            profileId = profileA,
            isGuest = false,
        )

        val navigation = SavedState.Navigation(
            activeNav = 0,
            backStacks = listOf(listOf("/home"), listOf("/search")),
        )
        state.process(
            AuthNavigationDigest(
                profileId = profileB,
                isGuest = false,
                navigation = navigation,
            ),
        )

        val result = state.navigationMutation().invoke(signedInNav)
        assertEquals(
            expected = 4,
            actual = result.stacks.size,
        )
        assertEquals(
            expected = AppStack.Home.stackName,
            actual = result.stacks[0].name,
        )
    }

    // endregion

    // region Authenticated → No auth (sign out)

    @Test
    fun signOut_from_non_auth_stack_forces_signedOut_navigation() {
        val state = AuthNavigationEventState(
            profileId = profileA,
            isGuest = false,
        )

        state.process(
            AuthNavigationDigest(
                profileId = null,
                isGuest = false,
                navigation = savedNavigation,
            ),
        )

        // On signed-in nav (not auth stack) with no profile → should force sign out
        val result = state.navigationMutation().invoke(signedInNav)
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
    fun signOut_while_on_auth_stack_keeps_auth_stack() {
        val state = AuthNavigationEventState(
            profileId = profileA,
            isGuest = false,
        )

        state.process(
            AuthNavigationDigest(
                profileId = null,
                isGuest = false,
                navigation = savedNavigation,
            ),
        )

        // Already on auth stack — should keep it
        val result = state.navigationMutation().invoke(authStackNav)
        assertEquals(
            expected = authStackNav,
            actual = result,
        )
    }

    // endregion

    // region Guest stays guest

    @Test
    fun guest_staying_guest_keeps_current_navigation() {
        val state = AuthNavigationEventState(
            profileId = null,
            isGuest = true,
        )

        state.process(
            AuthNavigationDigest(
                profileId = null,
                isGuest = true,
                navigation = savedNavigation,
            ),
        )

        val result = state.navigationMutation().invoke(authStackNav)
        assertEquals(
            expected = authStackNav,
            actual = result,
        )
    }

    @Test
    fun guest_staying_guest_with_deep_navigation_keeps_current() {
        val state = AuthNavigationEventState(
            profileId = null,
            isGuest = true,
        )

        state.process(
            AuthNavigationDigest(
                profileId = null,
                isGuest = true,
                navigation = deepSavedNavigation,
            ),
        )

        val result = state.navigationMutation().invoke(deepSignedInNav)
        assertEquals(
            expected = deepSignedInNav,
            actual = result,
        )
    }

    // endregion

    // region Steady state - authenticated stays authenticated

    @Test
    fun authenticated_staying_authenticated_keeps_current_navigation() {
        val state = AuthNavigationEventState(
            profileId = profileA,
            isGuest = false,
        )

        state.process(
            AuthNavigationDigest(
                profileId = profileA,
                isGuest = false,
                navigation = savedNavigation,
            ),
        )

        val result = state.navigationMutation().invoke(signedInNav)
        assertEquals(
            expected = signedInNav,
            actual = result,
        )
    }

    @Test
    fun authenticated_staying_authenticated_with_deep_navigation_keeps_current() {
        val state = AuthNavigationEventState(
            profileId = profileA,
            isGuest = false,
        )

        state.process(
            AuthNavigationDigest(
                profileId = profileA,
                isGuest = false,
                navigation = deepSavedNavigation,
            ),
        )

        val result = state.navigationMutation().invoke(deepSignedInNav)
        assertEquals(
            expected = deepSignedInNav,
            actual = result,
        )
    }

    // endregion
}
