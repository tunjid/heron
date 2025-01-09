package com.tunjid.heron.scaffold.navigation

import com.tunjid.treenav.Node
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.urlRouteMatcher
import kotlinx.serialization.Serializable

/**
 * Basic route definition
 */
@Serializable
private class BasicRoute(
    override val routeParams: SerializedRouteParams,
    override val children: List<Node> = emptyList(),
) : Route {

    constructor(
        path: String,
        pathArgs: Map<String, String> = emptyMap(),
        queryParams: Map<String, List<String>> = emptyMap(),
        children: List<Node> = emptyList(),
    ) : this(
        routeParams = RouteParams(
            pathAndQueries = path,
            pathArgs = pathArgs,
            queryParams = queryParams,
        ),
        children = children
    )

    override fun toString(): String = id
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Route) return false

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}


fun routeOf(
    params: RouteParams,
    children: List<Node> = emptyList(),
): Route = BasicRoute(
    routeParams = params,
    children = children
)

fun routeOf(
    path: String,
    pathArgs: Map<String, String> = emptyMap(),
    queryParams: Map<String, List<String>> = emptyMap(),
    children: List<Node> = listOf(),
): Route = BasicRoute(
    path = path,
    pathArgs = pathArgs,
    queryParams = queryParams,
    children = children
)

fun <T : Route> routeAndMatcher(
    routePattern: String,
    routeMapper: (RouteParams) -> T,
) = routePattern to urlRouteMatcher(
    routePattern = routePattern,
    routeMapper = routeMapper
)