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

import com.tunjid.heron.data.core.models.ModelUrlSafeBase64
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.current
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParser
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * provides a context for navigation actions, most commonly parsing a string route to a fully
 * type route.
 */
interface NavigationContext {
    val navState: MultiStackNav
    val String.toRoute: Route
    fun Route.encodeToQueryParam(): String
}

internal class ImmutableNavigationContext(
    private val state: MultiStackNav,
    private val routeParser: RouteParser,
) : NavigationContext {
    override val navState: MultiStackNav get() = state

    override val String.toRoute: Route
        get() = routeParser.parse(this) ?: unknownRoute()

    @OptIn(ExperimentalEncodingApi::class)
    override fun Route.encodeToQueryParam(): String =
        ModelUrlSafeBase64.encode(routeParams.pathAndQueries.encodeToByteArray())
}

fun unknownRoute(path: String = "/404") = routeOf(path = path)

@OptIn(ExperimentalEncodingApi::class)
fun String.decodeRoutePathAndQueriesFromQueryParam(): String =
    ModelUrlSafeBase64.decode(this).decodeToString()


val NavigationContext.currentRoute get() = navState.current as Route
