package com.tunjid.heron

import android.content.Context
import android.os.Build
import com.tunjid.heron.compose.di.ComposeComponent
import com.tunjid.heron.compose.di.ComposeNavigationComponent
import com.tunjid.heron.compose.di.create
import com.tunjid.heron.data.database.getDatabaseBuilder
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
import com.tunjid.heron.home.di.HomeComponent
import com.tunjid.heron.home.di.HomeNavigationComponent
import com.tunjid.heron.home.di.create
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
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

fun createAppState(context: Context): AppState {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val navigationComponent = AppNavigationComponent::class.create(
        signInNavigationComponent = SignInNavigationComponent::class.create(),
        composeNavigationComponent = ComposeNavigationComponent::class.create(),
        feedNavigationComponent = FeedNavigationComponent::class.create(),
        homeNavigationComponent = HomeNavigationComponent::class.create(),
        messagesNavigationComponent = MessagesNavigationComponent::class.create(),
        notificationsNavigationComponent = NotificationsNavigationComponent::class.create(),
        postDetailNavigationComponent = PostDetailNavigationComponent::class.create(),
        profileNavigationComponent = ProfileNavigationComponent::class.create(),
        profilesNavigationComponent = ProfilesNavigationComponent::class.create(),
        searchNavigationComponent = SearchNavigationComponent::class.create(),
        splashNavigationComponent = SplashNavigationComponent::class.create(),
    )

    val dataComponent = DataComponent::class.create(
        DataModule(
            appScope = appScope,
            savedStatePath = context.savedStatePath(),
            savedStateFileSystem = FileSystem.SYSTEM,
            databaseBuilder = getDatabaseBuilder(context),
        )
    )

    val scaffoldComponent = ScaffoldComponent::class.create(
        module = ScaffoldModule(
            routeMatchers = navigationComponent.allRouteMatchers
        ),
        dataComponent = dataComponent,
    )

    val appComponent = AppComponent::class.create(
        dataComponent = dataComponent,
        scaffoldComponent = scaffoldComponent,
        signInComponent = SignInComponent::class.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        composeComponent = ComposeComponent::class.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        feedComponent = FeedComponent::class.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        homeComponent = HomeComponent::class.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        messagesComponent = MessagesComponent::class.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        notificationsComponent = NotificationsComponent::class.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        postDetailComponent = PostDetailComponent::class.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        profileComponent = ProfileComponent::class.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        profilesComponent = ProfilesComponent::class.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        searchComponent = SearchComponent::class.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
        splashComponent = SplashComponent::class.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
    )

    return appComponent.appState
}

private fun Context.savedStatePath(): Path =
    filesDir.resolve("savedState").absolutePath.toPath()