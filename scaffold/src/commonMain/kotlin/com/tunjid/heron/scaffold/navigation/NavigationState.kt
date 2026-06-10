package com.tunjid.heron.scaffold.navigation

import com.tunjid.heron.data.core.models.UrlEncodableModel
import com.tunjid.heron.data.core.models.toUrlEncodedBase64
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.referringRouteQueryParams
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.pop
import com.tunjid.treenav.popToRoot
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.routeString
import com.tunjid.treenav.switch

@Snapshottable
interface NavigationState {
    @SnapshotSpec
    data class Immutable(
        val multiStackNav: MultiStackNav = Initial,
    ) : NavigationState

    companion object {
        internal val Initial = MultiStackNav(
            name = "splash-app",
            stacks = listOf(
                AppStack.Splash.toStackNav(),
            ),
        )
        internal val SignedOut = MultiStackNav(
            name = "signed-out-app",
            stacks = listOf(
                AppStack.Auth.toStackNav(),
            ),
        )
        internal val SignedIn = MultiStackNav(
            name = "signed-in-app",
            stacks = listOf(
                AppStack.Home,
                AppStack.Search,
                AppStack.Messages,
                AppStack.Notifications,
            ).map(AppStack::toStackNav),
        )

        internal const val OAuthUrlPathSegment = "oauth"
    }
}

/**
 * An action that causes mutations to navigation
 */
interface NavigationAction {
    val navigationMutation: NavigationMutation

    data object Pop : NavigationAction {
        override val navigationMutation: NavigationMutation = {
            navState.pop()
        }
    }

    data object Home : NavigationAction {
        override val navigationMutation: NavigationMutation = {
            navState.switch(0).popToRoot()
        }
    }

    sealed class Destination : NavigationAction {

        internal data class ToRawUrl(
            val path: String,
            val sharedElementPrefix: String? = null,
            val avatarSharedElementKey: String? = null,
            val sharedUri: GenericUri? = null,
            val models: List<UrlEncodableModel> = emptyList(),
            val miscQueries: Map<String, List<String>> = emptyMap(),
            val referringRouteOption: ReferringRouteOption,
        ) : Destination() {
            override val navigationMutation: NavigationMutation = {
                routeString(
                    path = path,
                    queryParams = miscQueries + mapOf(
                        "model" to models.map(UrlEncodableModel::toUrlEncodedBase64),
                        "sharedElementPrefix" to listOfNotNull(sharedElementPrefix),
                        "avatarSharedElementKey" to listOfNotNull(avatarSharedElementKey),
                        "sharedUri" to listOfNotNull(sharedUri?.uri),
                        referringRouteQueryParams(referringRouteOption),
                    ),
                ).toRoute
                    .takeIf { it.id != currentRoute.id }
                    ?.let(navState::push)
                    ?: navState
            }
        }
    }

    /**
     * Definition for describing what route referred a route to a destination
     */
    sealed class ReferringRouteOption {

        /**
         * The current route is the referrer.
         */
        data object Current : ReferringRouteOption()

        /**
         * The referrer is the route that referred the current route if it exists.
         */
        data object Parent : ReferringRouteOption()

        /**
         * The parent referrer is the referrer it it exists, otherwise
         * the current route is the referrer.
         */
        data object ParentOrCurrent : ReferringRouteOption()

        companion object {
            fun NavigationContext.referringRouteQueryParams(
                option: ReferringRouteOption,
            ): Pair<String, List<String>> = ReferringRouteQueryParam to when (option) {
                Current -> listOf(
                    currentRoute.encodeToQueryParam(),
                )

                Parent ->
                    currentRoute
                        .routeParams
                        .queryParams
                        .getOrElse(
                            key = ReferringRouteQueryParam,
                            defaultValue = ::emptyList,
                        )

                ParentOrCurrent -> referringRouteQueryParams(Parent).second.ifEmpty {
                    referringRouteQueryParams(Current).second
                }
            }

            fun RouteParams.decodeReferringRoute() =
                queryParams[ReferringRouteQueryParam]?.firstOrNull()
                    ?.decodeRoutePathAndQueriesFromQueryParam()
                    ?.let(::routeOf)

            /**
             * Hydrates a route with metadata that may have been lost like path args and
             * query args.
             */
            fun RouteParser.hydrate(route: Route) = parse(route.routeParams.pathAndQueries) ?: route
        }
    }
}

private fun AppStack.toStackNav() = StackNav(
    name = stackName,
    children = listOf(rootRoute),
)

private const val ReferringRouteQueryParam = "referringRoute"
