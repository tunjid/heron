package com.tunjid.heron

import com.tunjid.heron.data.database.getDatabaseBuilder
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.data.di.DataModule
import com.tunjid.heron.data.di.create
import com.tunjid.heron.di.AppComponent
import com.tunjid.heron.di.AppNavigationComponent
import com.tunjid.heron.di.allRouteMatchers
import com.tunjid.heron.di.create
import com.tunjid.heron.home.di.HomeComponent
import com.tunjid.heron.home.di.HomeNavigationComponent
import com.tunjid.heron.home.di.create
import com.tunjid.heron.postdetail.di.PostDetailComponent
import com.tunjid.heron.postdetail.di.PostDetailNavigationComponent
import com.tunjid.heron.postdetail.di.create
import com.tunjid.heron.profile.di.ProfileComponent
import com.tunjid.heron.profile.di.ProfileNavigationComponent
import com.tunjid.heron.profile.di.create
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.di.ScaffoldModule
import com.tunjid.heron.scaffold.di.create
import com.tunjid.heron.scaffold.scaffold.AppState
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
import okio.Path.Companion.toOkioPath
import java.io.File

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

fun createAppState(): AppState {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val navigationComponent = AppNavigationComponent::class.create(
        signInNavigationComponent = SignInNavigationComponent::class.create(),
        homeNavigationComponent = HomeNavigationComponent::class.create(),
        postDetailNavigationComponent = PostDetailNavigationComponent::class.create(),
        profileNavigationComponent = ProfileNavigationComponent::class.create(),
        splashNavigationComponent = SplashNavigationComponent::class.create(),
    )

    val dataComponent = DataComponent::class.create(
        DataModule(
            appScope = appScope,
            savedStatePath = savedStatePath(),
            savedStateFileSystem = FileSystem.SYSTEM,
            databaseBuilder = getDatabaseBuilder(),
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
        homeComponent = HomeComponent::class.create(
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
        splashComponent = SplashComponent::class.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
    )

    return appComponent.appState
}

private fun savedStatePath(): Path = File(
    System.getProperty("java.io.tmpdir"),
    "tunji-heron-saved-state-20.ser"
).toOkioPath()