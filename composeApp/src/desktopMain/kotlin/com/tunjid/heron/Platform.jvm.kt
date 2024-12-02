package com.tunjid.heron

import com.tunjid.heron.data.di.DataModule
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.di.AppComponent
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.di.ScaffoldModule
import com.tunjid.heron.signin.di.SignInScreenHolderComponent
import com.tunjid.heron.signin.di.create
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File
import com.tunjid.heron.scaffold.di.create
import com.tunjid.heron.data.di.create
import com.tunjid.heron.di.AppNavigationComponent
import com.tunjid.heron.di.create
import com.tunjid.heron.scaffold.app.AppState
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

    val scaffoldComponent = ScaffoldComponent::class.create(
        module = ScaffoldModule(
            routeMatchers = listOf()
        ),
        dataComponent = dataComponent,
    )

    val appComponent = AppComponent::class.create(
        dataComponent = dataComponent,
        scaffoldComponent = scaffoldComponent,
        signInComponent = SignInScreenHolderComponent::class.create(
            scaffoldComponent = scaffoldComponent,
            dataComponent = dataComponent,
        ),
    )

    return appComponent.appState
}

private fun savedStatePath(): Path = File(
    System.getProperty("java.io.tmpdir"),
    "tunji-heron-saved-state-9.ser"
).toOkioPath()