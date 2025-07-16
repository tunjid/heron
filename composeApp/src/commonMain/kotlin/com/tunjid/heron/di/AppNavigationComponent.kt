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

package com.tunjid.heron.di

import com.tunjid.heron.compose.di.ComposeNavigationComponent
import com.tunjid.heron.feed.di.FeedNavigationComponent
import com.tunjid.heron.gallery.di.GalleryNavigationComponent
import com.tunjid.heron.home.di.HomeNavigationComponent
import com.tunjid.heron.messages.di.MessagesNavigationComponent
import com.tunjid.heron.notifications.di.NotificationsNavigationComponent
import com.tunjid.heron.postdetail.di.PostDetailNavigationComponent
import com.tunjid.heron.profile.avatar.di.ProfileAvatarNavigationComponent
import com.tunjid.heron.profile.di.ProfileNavigationComponent
import com.tunjid.heron.profiles.di.ProfilesNavigationComponent
import com.tunjid.heron.search.di.SearchNavigationComponent
import com.tunjid.heron.signin.di.SignInNavigationComponent
import com.tunjid.heron.splash.di.SplashNavigationComponent
import com.tunjid.treenav.strings.RouteMatcher
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes

@DependencyGraph(
    scope = AppScope::class
)
interface AppNavigationComponent {

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Includes signInNavigationComponent: SignInNavigationComponent,
            @Includes composeNavigationComponent: ComposeNavigationComponent,
            @Includes feedNavigationComponent: FeedNavigationComponent,
            @Includes galleryNavigationComponent: GalleryNavigationComponent,
            @Includes homeNavigationComponent: HomeNavigationComponent,
            @Includes messagesNavigationComponent: MessagesNavigationComponent,
            @Includes notificationsNavigationComponent: NotificationsNavigationComponent,
            @Includes postDetailNavigationComponent: PostDetailNavigationComponent,
            @Includes profileNavigationComponent: ProfileNavigationComponent,
            @Includes profileAvatarNavigationComponent: ProfileAvatarNavigationComponent,
            @Includes profilesNavigationComponent: ProfilesNavigationComponent,
            @Includes searchNavigationComponent: SearchNavigationComponent,
            @Includes splashNavigationComponent: SplashNavigationComponent,
        ): AppNavigationComponent
    }

    val routeMatcherMap: Map<String, RouteMatcher>
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