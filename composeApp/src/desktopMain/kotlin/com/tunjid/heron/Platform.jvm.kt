package com.tunjid.heron

import com.tunjid.heron.data.di.DataModule
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.di.AppComponent
import com.tunjid.heron.navigation.di.NavigationComponent
import com.tunjid.heron.navigation.di.NavigationModule
import com.tunjid.heron.signin.di.SignInScreenHolderComponent
import com.tunjid.heron.signin.di.create
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File
import com.tunjid.heron.navigation.di.create
import com.tunjid.heron.data.di.create
import com.tunjid.heron.di.AppNavigationComponent
import com.tunjid.heron.di.create
import com.tunjid.heron.signin.di.SignInNavigationComponent

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

fun createAppState(): AppState {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    AppNavigationComponent::class.create(
        signInNavigationComponent = SignInNavigationComponent::class.create()
    )

    val dataComponent = DataComponent::class.create(
        DataModule(
            appScope = appScope,
            savedStatePath = savedStatePath(),
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

private fun savedStatePath(): Path = File(
    System.getProperty("java.io.tmpdir"),
    "tunji-heron-saved-state-9.ser"
).toOkioPath()