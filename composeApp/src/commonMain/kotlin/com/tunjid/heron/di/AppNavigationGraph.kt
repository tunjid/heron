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

import com.tunjid.heron.compose.di.ComposeNavigationBindings
import com.tunjid.heron.conversation.di.ConversationNavigationBindings
import com.tunjid.heron.feed.di.FeedNavigationBindings
import com.tunjid.heron.gallery.di.GalleryNavigationBindings
import com.tunjid.heron.home.di.HomeNavigationBindings
import com.tunjid.heron.list.di.ListNavigationBindings
import com.tunjid.heron.messages.di.MessagesNavigationBindings
import com.tunjid.heron.notifications.di.NotificationsNavigationBindings
import com.tunjid.heron.postdetail.di.PostDetailNavigationBindings
import com.tunjid.heron.posts.di.PostsNavigationBindings
import com.tunjid.heron.profile.avatar.di.ProfileAvatarNavigationBindings
import com.tunjid.heron.profile.di.ProfileNavigationBindings
import com.tunjid.heron.profiles.di.ProfilesNavigationBindings
import com.tunjid.heron.search.di.SearchNavigationBindings
import com.tunjid.heron.settings.di.SettingsNavigationBindings
import com.tunjid.heron.signin.di.SignInNavigationBindings
import com.tunjid.heron.splash.di.SplashNavigationBindings
import com.tunjid.treenav.strings.RouteMatcher
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes

@DependencyGraph(
    scope = AppScope::class,
)
interface AppNavigationGraph {

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Includes signInNavigationBindings: SignInNavigationBindings,
            @Includes composeNavigationBindings: ComposeNavigationBindings,
            @Includes conversationNavigationBindings: ConversationNavigationBindings,
            @Includes feedNavigationBindings: FeedNavigationBindings,
            @Includes galleryNavigationBindings: GalleryNavigationBindings,
            @Includes homeNavigationBindings: HomeNavigationBindings,
            @Includes listNavigationBindings: ListNavigationBindings,
            @Includes messagesNavigationBindings: MessagesNavigationBindings,
            @Includes notificationsNavigationBindings: NotificationsNavigationBindings,
            @Includes postDetailNavigationBindings: PostDetailNavigationBindings,
            @Includes postsNavigationBindings: PostsNavigationBindings,
            @Includes profileNavigationBindings: ProfileNavigationBindings,
            @Includes profileAvatarNavigationBindings: ProfileAvatarNavigationBindings,
            @Includes profilesNavigationBindings: ProfilesNavigationBindings,
            @Includes searchNavigationBindings: SearchNavigationBindings,
            @Includes splashNavigationBindings: SplashNavigationBindings,
            @Includes settingsNavigationBindings: SettingsNavigationBindings,
        ): AppNavigationGraph
    }

    val routeMatcherMap: Map<String, RouteMatcher>
}

val AppNavigationGraph.allRouteMatchers
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
        { (key) -> key },
    ).reversed()
