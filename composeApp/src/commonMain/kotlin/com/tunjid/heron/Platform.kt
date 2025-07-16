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

import com.tunjid.heron.compose.di.ComposeComponent
import com.tunjid.heron.compose.di.ComposeNavigationComponent
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.data.di.DataBindingArgs
import com.tunjid.heron.di.AppComponent
import com.tunjid.heron.di.AppNavigationComponent
import com.tunjid.heron.di.allRouteMatchers
import com.tunjid.heron.feed.di.FeedComponent
import com.tunjid.heron.feed.di.FeedNavigationComponent
import com.tunjid.heron.gallery.di.GalleryComponent
import com.tunjid.heron.gallery.di.GalleryNavigationComponent
import com.tunjid.heron.home.di.HomeComponent
import com.tunjid.heron.home.di.HomeNavigationComponent
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.messages.di.MessagesComponent
import com.tunjid.heron.messages.di.MessagesNavigationComponent
import com.tunjid.heron.notifications.di.NotificationsComponent
import com.tunjid.heron.notifications.di.NotificationsNavigationComponent
import com.tunjid.heron.postdetail.di.PostDetailComponent
import com.tunjid.heron.postdetail.di.PostDetailNavigationComponent
import com.tunjid.heron.profile.avatar.di.ProfileAvatarComponent
import com.tunjid.heron.profile.avatar.di.ProfileAvatarNavigationComponent
import com.tunjid.heron.profile.di.ProfileComponent
import com.tunjid.heron.profile.di.ProfileNavigationComponent
import com.tunjid.heron.profiles.di.ProfilesComponent
import com.tunjid.heron.profiles.di.ProfilesNavigationComponent
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.di.ScaffoldBindingArgs
import com.tunjid.heron.scaffold.scaffold.AppState
import com.tunjid.heron.search.di.SearchComponent
import com.tunjid.heron.search.di.SearchNavigationComponent
import com.tunjid.heron.signin.di.SignInComponent
import com.tunjid.heron.signin.di.SignInNavigationComponent
import com.tunjid.heron.splash.di.SplashComponent
import com.tunjid.heron.splash.di.SplashNavigationComponent
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

    val navigationComponent = createGraphFactory<AppNavigationComponent.Factory>().create(
        signInNavigationComponent = SignInNavigationComponent,
        composeNavigationComponent = ComposeNavigationComponent,
        feedNavigationComponent = FeedNavigationComponent,
        galleryNavigationComponent = GalleryNavigationComponent,
        homeNavigationComponent = HomeNavigationComponent,
        messagesNavigationComponent = MessagesNavigationComponent,
        notificationsNavigationComponent = NotificationsNavigationComponent,
        postDetailNavigationComponent = PostDetailNavigationComponent,
        profileNavigationComponent = ProfileNavigationComponent,
        profileAvatarNavigationComponent = ProfileAvatarNavigationComponent,
        profilesNavigationComponent = ProfilesNavigationComponent,
        searchNavigationComponent = SearchNavigationComponent,
        splashNavigationComponent = SplashNavigationComponent,
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

    val appComponent = createGraphFactory<AppComponent.Factory>().create(
        dataBindings = dataBindings,
        scaffoldBindings = scaffoldBindings,
        signInComponent = SignInComponent(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        composeComponent = ComposeComponent(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        feedComponent = FeedComponent(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        galleryComponent = GalleryComponent(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        homeComponent = HomeComponent(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        messagesComponent = MessagesComponent(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        notificationsComponent = NotificationsComponent(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        postDetailComponent = PostDetailComponent(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        profileComponent = ProfileComponent(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        profileAvatarComponent = ProfileAvatarComponent(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        profilesComponent = ProfilesComponent(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        searchComponent = SearchComponent(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
        splashComponent = SplashComponent(
            scaffoldBindings = scaffoldBindings,
            dataBindings = dataBindings,
        ),
    )

    return appComponent.appState
}