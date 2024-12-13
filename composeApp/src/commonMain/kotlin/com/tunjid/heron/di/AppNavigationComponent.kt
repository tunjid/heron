/*
 * Copyright 2021 Google LLC
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

package com.tunjid.heron.di

import com.tunjid.heron.home.di.HomeNavigationComponent
import com.tunjid.heron.postdetail.di.PostDetailNavigationComponent
import com.tunjid.heron.profile.di.ProfileNavigationComponent
import com.tunjid.heron.signin.di.SignInNavigationComponent
import com.tunjid.treenav.strings.RouteMatcher
import me.tatarka.inject.annotations.Component

//@SingletonScope
@Component
abstract class AppNavigationComponent(
    @Component val signInNavigationComponent: SignInNavigationComponent,
    @Component val homeNavigationComponent: HomeNavigationComponent,
    @Component val postDetailNavigationComponent: PostDetailNavigationComponent,
    @Component val profileNavigationComponent: ProfileNavigationComponent,
) {
    internal abstract val routeMatcherMap: Map<String, RouteMatcher>
}

val AppNavigationComponent.allRouteMatchers
    get() = routeMatcherMap
        .toList()
        .sortedWith(routeMatchingComparator())
        .map(Pair<String, RouteMatcher>::second)

private fun routeMatchingComparator() =
    compareBy<Pair<String, RouteMatcher>>(
        // Order by number of path segments firs
        { (key) -> key.split("/").size },
        // Match more specific segments first, route params should be matched later
        { (key) -> -key.split("/").filter { it.startsWith("{") }.size },
        // Finally sort alphabetically
        { (key) -> key }
    ).reversed()