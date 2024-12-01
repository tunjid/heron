package com.tunjid.heron

import android.content.Context
import android.os.Build
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.data.di.DataModule
import com.tunjid.heron.data.di.create
import com.tunjid.heron.di.AppComponent
import com.tunjid.heron.di.AppNavigationComponent
import com.tunjid.heron.di.create
import com.tunjid.heron.navigation.di.NavigationComponent
import com.tunjid.heron.navigation.di.NavigationModule
import com.tunjid.heron.navigation.di.create
import com.tunjid.heron.signin.di.SignInNavigationComponent
import com.tunjid.heron.signin.di.SignInScreenHolderComponent
import com.tunjid.heron.signin.di.create
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

    AppNavigationComponent::class.create(
        signInNavigationComponent = SignInNavigationComponent::class.create()
    )

    val dataComponent = DataComponent::class.create(
        DataModule(
            appScope = appScope,
            savedStatePath = context.savedStatePath(),
            savedStateFileSystem = FileSystem.SYSTEM,
        )
    )

    val navigationComponent = NavigationComponent::class.create(
        module = NavigationModule(
            routeMatchers = listOf()
        ),
        dataComponent = dataComponent,
    )

    val appComponent = AppComponent::class.create(
        dataComponent = dataComponent,
        navigationComponent = navigationComponent,
        signInComponent = SignInScreenHolderComponent::class.create(
            navigationComponent = navigationComponent,
            dataComponent = dataComponent,
        ),
    )

    return appComponent.appState
}

private fun Context.savedStatePath(): Path =
    filesDir.resolve("savedState").absolutePath.toPath()