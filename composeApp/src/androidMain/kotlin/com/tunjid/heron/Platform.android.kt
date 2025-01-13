package com.tunjid.heron

import android.content.Context
import android.os.Build
import com.tunjid.heron.data.database.getDatabaseBuilder
import com.tunjid.heron.data.di.DataModule
import com.tunjid.heron.scaffold.scaffold.AppState
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

fun createAppState(context: Context): AppState =
    createAppState { appScope ->
        DataModule(
            appScope = appScope,
            savedStatePath = context.savedStatePath(),
            savedStateFileSystem = FileSystem.SYSTEM,
            databaseBuilder = getDatabaseBuilder(context),
        )
    }

private fun Context.savedStatePath(): Path =
    filesDir.resolve("savedState").absolutePath.toPath()