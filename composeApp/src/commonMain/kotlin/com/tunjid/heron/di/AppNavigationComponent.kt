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
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.KmpComponentCreate

@KmpComponentCreate
expect fun AppNavigationComponent.Companion.create(
    signInNavigationComponent: SignInNavigationComponent,
    composeNavigationComponent: ComposeNavigationComponent,
    feedNavigationComponent: FeedNavigationComponent,
    galleryNavigationComponent: GalleryNavigationComponent,
    homeNavigationComponent: HomeNavigationComponent,
    messagesNavigationComponent: MessagesNavigationComponent,
    notificationsNavigationComponent: NotificationsNavigationComponent,
    postDetailNavigationComponent: PostDetailNavigationComponent,
    profileNavigationComponent: ProfileNavigationComponent,
    profileAvatarNavigationComponent: ProfileAvatarNavigationComponent,
    profilesNavigationComponent: ProfilesNavigationComponent,
    searchNavigationComponent: SearchNavigationComponent,
    splashNavigationComponent: SplashNavigationComponent,
): AppNavigationComponent

//@SingletonScope
@Component
abstract class AppNavigationComponent(
    @Component val signInNavigationComponent: SignInNavigationComponent,
    @Component val composeNavigationComponent: ComposeNavigationComponent,
    @Component val feedNavigationComponent: FeedNavigationComponent,
    @Component val galleryNavigationComponent: GalleryNavigationComponent,
    @Component val homeNavigationComponent: HomeNavigationComponent,
    @Component val messagesNavigationComponent: MessagesNavigationComponent,
    @Component val notificationsNavigationComponent: NotificationsNavigationComponent,
    @Component val postDetailNavigationComponent: PostDetailNavigationComponent,
    @Component val profileNavigationComponent: ProfileNavigationComponent,
    @Component val profileAvatarNavigationComponent: ProfileAvatarNavigationComponent,
    @Component val profilesNavigationComponent: ProfilesNavigationComponent,
    @Component val searchNavigationComponent: SearchNavigationComponent,
    @Component val splashNavigationComponent: SplashNavigationComponent,
) {
    internal abstract val routeMatcherMap: Map<String, RouteMatcher>

    companion object
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