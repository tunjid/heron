package com.tunjid.heron

import com.tunjid.heron.compose.di.ComposeComponent
import com.tunjid.heron.compose.di.ComposeNavigationComponent
import com.tunjid.heron.compose.di.create
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.data.di.DataModule
import com.tunjid.heron.data.di.create
import com.tunjid.heron.di.AppComponent
import com.tunjid.heron.di.AppNavigationComponent
import com.tunjid.heron.di.allRouteMatchers
import com.tunjid.heron.di.create
import com.tunjid.heron.feed.di.FeedComponent
import com.tunjid.heron.feed.di.FeedNavigationComponent
import com.tunjid.heron.feed.di.create
import com.tunjid.heron.gallery.di.GalleryComponent
import com.tunjid.heron.gallery.di.GalleryNavigationComponent
import com.tunjid.heron.gallery.di.create
import com.tunjid.heron.home.di.HomeComponent
import com.tunjid.heron.home.di.HomeNavigationComponent
import com.tunjid.heron.home.di.create
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.messages.di.MessagesComponent
import com.tunjid.heron.messages.di.MessagesNavigationComponent
import com.tunjid.heron.messages.di.create
import com.tunjid.heron.notifications.di.NotificationsComponent
import com.tunjid.heron.notifications.di.NotificationsNavigationComponent
import com.tunjid.heron.notifications.di.create
import com.tunjid.heron.postdetail.di.PostDetailComponent
import com.tunjid.heron.postdetail.di.PostDetailNavigationComponent
import com.tunjid.heron.postdetail.di.create
import com.tunjid.heron.profile.di.ProfileComponent
import com.tunjid.heron.profile.di.ProfileNavigationComponent
import com.tunjid.heron.profile.di.create
import com.tunjid.heron.profiles.di.ProfilesComponent
import com.tunjid.heron.profiles.di.ProfilesNavigationComponent
import com.tunjid.heron.profiles.di.create
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.di.ScaffoldModule
import com.tunjid.heron.scaffold.di.create
import com.tunjid.heron.scaffold.scaffold.AppState
import com.tunjid.heron.search.di.SearchComponent
import com.tunjid.heron.search.di.SearchNavigationComponent
import com.tunjid.heron.search.di.create
import com.tunjid.heron.signin.di.SignInComponent
import com.tunjid.heron.signin.di.SignInNavigationComponent
import com.tunjid.heron.signin.di.create
import com.tunjid.heron.splash.di.SplashComponent
import com.tunjid.heron.splash.di.SplashNavigationComponent
import com.tunjid.heron.splash.di.create
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

    val navigationComponent = AppNavigationComponent.create(
        signInNavigationComponent = SignInNavigationComponent.create(),
        composeNavigationComponent = ComposeNavigationComponent.create(),
        feedNavigationComponent = FeedNavigationComponent.create(),
        galleryNavigationComponent = GalleryNavigationComponent.create(),
        homeNavigationComponent = HomeNavigationComponent.create(),
        messagesNavigationComponent = MessagesNavigationComponent.create(),
        notificationsNavigationComponent = NotificationsNavigationComponent.create(),
        postDetailNavigationComponent = PostDetailNavigationComponent.create(),
        profileNavigationComponent = ProfileNavigationComponent.create(),
        profilesNavigationComponent = ProfilesNavigationComponent.create(),
        searchNavigationComponent = SearchNavigationComponent.create(),
        splashNavigationComponent = SplashNavigationComponent.create(),
    )

    val dataComponent = DataComponent.create(
        dataModule(appScope)
    )

    val scaffoldComponent = ScaffoldComponent.create(
        module = ScaffoldModule(
            videoPlayerController = videoPlayerController(appScope),
            routeMatchers = navigationComponent.allRouteMatchers
        ),
        dataComponent = dataComponent,
    )

    val appComponent = AppComponent.create(
        dataComponent = dataComponent,
        scaffoldComponent = scaffoldComponent,
        signInComponent = SignInComponent.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        composeComponent = ComposeComponent.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        feedComponent = FeedComponent.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        galleryComponent = GalleryComponent.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        homeComponent = HomeComponent.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        messagesComponent = MessagesComponent.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        notificationsComponent = NotificationsComponent.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        postDetailComponent = PostDetailComponent.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        profileComponent = ProfileComponent.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        profilesComponent = ProfilesComponent.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        searchComponent = SearchComponent.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        splashComponent = SplashComponent.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
    )

    return appComponent.appState
}