package com.tunjid.heron.scaffold.navigation

import com.tunjid.heron.data.repository.SavedState
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.routeParserFrom
import com.tunjid.treenav.strings.urlRouteMatcher
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseMultiStackNavTest {

    // Route parser with explicit matchers mirroring real app route patterns.
    // Each matcher maps matched paths back to a simple routeOf(path).
    private val routeParser = routeParserFrom(
        *listOf(
            "/home",
            "/search",
            "/messages",
            "/notifications",
            "/auth",
            "/profile/{profileHandleOrId}",
            "/profile/{profileHandleOrId}/post/{postRecordKey}",
        ).map { pattern ->
            urlRouteMatcher(
                routePattern = pattern,
                routeMapper = { routeParams: RouteParams ->
                    routeOf(routeParams.pathAndQueries)
                },
            )
        }.toTypedArray(),
    )

    // region Signed-in user

    @Test
    fun signedIn_restores_navigation_with_signedIn_stack_names() {
        val navigation = SavedState.Navigation(
            activeNav = 0,
            backStacks = listOf(
                listOf("/home"),
                listOf("/search"),
                listOf("/messages"),
                listOf("/notifications"),
            ),
        )

        val result = routeParser.parseMultiStackNav(
            navigation = navigation,
            isSignedIn = true,
        )

        assertEquals(
            expected = 4,
            actual = result.stacks.size,
        )
        assertEquals(
            expected = AppStack.Home.stackName,
            actual = result.stacks[0].name,
        )
        assertEquals(
            expected = AppStack.Search.stackName,
            actual = result.stacks[1].name,
        )
        assertEquals(
            expected = AppStack.Messages.stackName,
            actual = result.stacks[2].name,
        )
        assertEquals(
            expected = AppStack.Notifications.stackName,
            actual = result.stacks[3].name,
        )
        assertEquals(
            expected = "/home",
            actual = result.stacks[0].children.first().id,
        )
    }

    @Test
    fun signedIn_on_auth_route_redirects_to_signedIn_navigation() {
        val navigation = SavedState.Navigation(
            activeNav = 0,
            backStacks = listOf(
                listOf(AppStack.Auth.rootRoute.id),
            ),
        )

        val result = routeParser.parseMultiStackNav(
            navigation = navigation,
            isSignedIn = true,
        )

        // Should redirect to SignedInNavigationState (4 stacks)
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

    // region Signed-out user (not guest)

    @Test
    fun signedOut_restores_navigation_with_signedOut_stack_names() {
        // Push a profile route on top of /auth to avoid the auth-route redirect guard
        val navigation = SavedState.Navigation(
            activeNav = 0,
            backStacks = listOf(
                listOf("/auth", "/profile/alice.bsky.social"),
            ),
        )

        val result = routeParser.parseMultiStackNav(
            navigation = navigation,
            isSignedIn = false,
        )

        assertEquals(
            expected = 1,
            actual = result.stacks.size,
        )
        assertEquals(
            expected = AppStack.Auth.stackName,
            actual = result.stacks[0].name,
        )
        // Current route is the last pushed route, not /auth root
        assertEquals(
            expected = 2,
            actual = result.stacks[0].children.size,
        )
    }

    @Test
    fun signedOut_on_auth_root_route_redirects_to_signedIn_navigation() {
        // When the only route is /auth, the guard redirects to SignedInNavigationState
        val navigation = SavedState.Navigation(
            activeNav = 0,
            backStacks = listOf(
                listOf(AppStack.Auth.rootRoute.id),
            ),
        )

        val result = routeParser.parseMultiStackNav(
            navigation = navigation,
            isSignedIn = false,
        )

        // The guard prevents anyone from being restored to the bare auth screen
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

    // region Guest user

    @Test
    fun guest_restores_navigation_with_signedIn_stack_names() {
        val navigation = SavedState.Navigation(
            activeNav = 0,
            backStacks = listOf(
                listOf("/home"),
                listOf("/search"),
                listOf("/messages"),
                listOf("/notifications"),
            ),
        )

        val result = routeParser.parseMultiStackNav(
            navigation = navigation,
            isSignedIn = false,
            isGuest = true,
        )

        // Guest should get SignedInNavigationState template (4 stacks)
        assertEquals(
            expected = 4,
            actual = result.stacks.size,
        )
        assertEquals(
            expected = AppStack.Home.stackName,
            actual = result.stacks[0].name,
        )
        assertEquals(
            expected = AppStack.Search.stackName,
            actual = result.stacks[1].name,
        )
        assertEquals(
            expected = AppStack.Messages.stackName,
            actual = result.stacks[2].name,
        )
        assertEquals(
            expected = AppStack.Notifications.stackName,
            actual = result.stacks[3].name,
        )
    }

    @Test
    fun guest_on_auth_route_redirects_to_signedIn_navigation() {
        val navigation = SavedState.Navigation(
            activeNav = 0,
            backStacks = listOf(
                listOf(AppStack.Auth.rootRoute.id),
            ),
        )

        val result = routeParser.parseMultiStackNav(
            navigation = navigation,
            isSignedIn = false,
            isGuest = true,
        )

        // Should not leave guest on the sign-in screen
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
    fun guest_with_deep_navigation_preserves_back_stack() {
        val navigation = SavedState.Navigation(
            activeNav = 1,
            backStacks = listOf(
                listOf("/home", "/profile/alice.bsky.social/post/3abc123"),
                listOf("/search", "/profile/bob.bsky.social"),
                listOf("/messages"),
                listOf("/notifications"),
            ),
        )

        val result = routeParser.parseMultiStackNav(
            navigation = navigation,
            isSignedIn = false,
            isGuest = true,
        )

        assertEquals(
            expected = 4,
            actual = result.stacks.size,
        )
        assertEquals(
            expected = 1,
            actual = result.currentIndex,
        )
        // Home stack should have 2 entries
        assertEquals(
            expected = 2,
            actual = result.stacks[0].children.size,
        )
        // Search stack should have 2 entries
        assertEquals(
            expected = 2,
            actual = result.stacks[1].children.size,
        )
    }

    // endregion
}
