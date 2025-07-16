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
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.data.di.DataModule
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
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.di.ScaffoldModule
import com.tunjid.heron.scaffold.scaffold.AppState
import com.tunjid.heron.search.di.SearchComponent
import com.tunjid.heron.search.di.SearchNavigationComponent
import com.tunjid.heron.signin.di.SignInComponent
import com.tunjid.heron.signin.di.SignInNavigationComponent
import com.tunjid.heron.splash.di.SplashComponent
import com.tunjid.heron.splash.di.SplashNavigationComponent
import dev.zacsweers.metro.createGraph
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
    dataModule: (appScope: CoroutineScope) -> DataModule,
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

    val dataComponent = DataComponent(
        module = dataModule(appScope)
    )

    val scaffoldComponent = ScaffoldComponent(
        module = ScaffoldModule(
            videoPlayerController = videoPlayerController(appScope),
            routeMatchers = navigationComponent.allRouteMatchers
        ),
        dataComponent = dataComponent,
    )

    val appComponent = createGraphFactory<AppComponent.Factory>().create(
        dataComponent = dataComponent,
        scaffoldComponent = scaffoldComponent,
        signInComponent = SignInComponent(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        composeComponent = ComposeComponent(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        feedComponent = FeedComponent(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        galleryComponent = GalleryComponent(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        homeComponent = HomeComponent(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        messagesComponent = MessagesComponent(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        notificationsComponent = NotificationsComponent(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        postDetailComponent = PostDetailComponent(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        profileComponent = ProfileComponent(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        profileAvatarComponent = ProfileAvatarComponent(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        profilesComponent = ProfilesComponent(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        searchComponent = SearchComponent(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        splashComponent = SplashComponent(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
    )

    return appComponent.appState
}