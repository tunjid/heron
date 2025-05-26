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

package com.tunjid.heron.scaffold.navigation

import com.tunjid.treenav.Node
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.urlRouteMatcher
import kotlinx.serialization.Serializable

/**
 * Basic route definition
 */
private class BasicRoute(
    override val routeParams: RouteParams,
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