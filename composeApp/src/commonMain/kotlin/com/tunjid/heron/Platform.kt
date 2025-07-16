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

package com.tunjid.heron

import com.tunjid.heron.compose.di.ComposeBindings
import com.tunjid.heron.compose.di.ComposeNavigationBindings
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.data.di.DataBindingArgs
import com.tunjid.heron.di.AppGraph
import com.tunjid.heron.di.AppNavigationGraph
import com.tunjid.heron.di.allRouteMatchers
import com.tunjid.heron.feed.di.FeedBindings
import com.tunjid.heron.feed.di.FeedNavigationBindings
import com.tunjid.heron.gallery.di.GalleryBindings
import com.tunjid.heron.gallery.di.GalleryNavigationBindings
import com.tunjid.heron.home.di.HomeBindings
import com.tunjid.heron.home.di.HomeNavigationBindings
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.messages.di.MessagesBindings
import com.tunjid.heron.messages.di.MessagesNavigationBindings
import com.tunjid.heron.notifications.di.NotificationsBindings
import com.tunjid.heron.notifications.di.NotificationsNavigationBindings
import com.tunjid.heron.postdetail.di.PostDetailBindings
import com.tunjid.heron.postdetail.di.PostDetailNavigationBindings
import com.tunjid.heron.profile.avatar.di.ProfileAvatarBindings
import com.tunjid.heron.profile.avatar.di.ProfileAvatarNavigationBindings
import com.tunjid.heron.profile.di.ProfileBindings
import com.tunjid.heron.profile.di.ProfileNavigationBindings
import com.tunjid.heron.profiles.di.ProfilesBindings
import com.tunjid.heron.profiles.di.ProfilesNavigationBindings
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.di.ScaffoldBindingArgs
import com.tunjid.heron.scaffold.scaffold.AppState
import com.tunjid.heron.search.di.SearchBindings
import com.tunjid.heron.search.di.SearchNavigationBindings
import com.tunjid.heron.signin.di.SignInBindings
import com.tunjid.heron.signin.di.SignInNavigationBindings
import com.tunjid.heron.splash.di.SplashBindings
import com.tunjid.heron.splash.di.SplashNavigationBindings
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

fun createAppState(
    videoPlayerController: (appScope: CoroutineScope) -> VideoPlayerController,
    args: (appScope: CoroutineScope) -> DataBindingArgs,
): AppState {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val navigationComponent = createGraphFactory<AppNavigationGraph.Factory>().create(
        signInNavigationBindings = SignInNavigationBindings,
        composeNavigationBindings = ComposeNavigationBindings,
        feedNavigationBindings = FeedNavigationBindings,
        galleryNavigationBindings = GalleryNavigationBindings,
        homeNavigationBindings = HomeNavigationBindings,
        messagesNavigationBindings = MessagesNavigationBindings,
        notificationsNavigationBindings = NotificationsNavigationBindings,
        postDetailNavigationBindings = PostDetailNavigationBindings,
        profileNavigationBindings = ProfileNavigationBindings,
        profileAvatarNavigationBindings = ProfileAvatarNavigationBindings,
        profilesNavigationBindings = ProfilesNavigationBindings,
        searchNavigationBindings = SearchNavigationBindings,
        splashNavigationBindings = SplashNavigationBindings,
    )

    val dataBindings = DataBindings(
        args = args(appScope)
    )

    val scaffoldBindings = ScaffoldBindings(
        args = ScaffoldBindingArgs(
            videoPlayerController = videoPlayerController(appScope),
            routeMatchers = navigationComponent.allRouteMatchers
        ),
        dataBindings = dataBindings,
    )

    val appGraph = createGraphFactory<AppGraph.Factory>().create(
        dataBindings = dataBindings,
        scaffoldBindings = scaffoldBindings,
        signInBindings = SignInBindings(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        composeBindings = ComposeBindings(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        feedBindings = FeedBindings(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        galleryBindings = GalleryBindings(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        homeBindings = HomeBindings(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        messagesBindings = MessagesBindings(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        notificationsBindings = NotificationsBindings(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        postDetailBindings = PostDetailBindings(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        profileBindings = ProfileBindings(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        profileAvatarBindings = ProfileAvatarBindings(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        profilesBindings = ProfilesBindings(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        searchBindings = SearchBindings(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        splashBindings = SplashBindings(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
    )

    return appGraph.appState
}