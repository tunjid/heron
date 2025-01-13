package com.tunjid.heron

import com.tunjid.heron.data.database.getDatabaseBuilder
import com.tunjid.heron.data.di.DataModule
import com.tunjid.heron.scaffold.scaffold.AppState
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

fun createAppState(): AppState =
    createAppState { appScope ->
        DataModule(
            appScope = appScope,
            savedStatePath = savedStatePath(),
            savedStateFileSystem = FileSystem.SYSTEM,
            databaseBuilder = getDatabaseBuilder(),
        )
    }

private fun savedStatePath(): Path = File(
    System.getProperty("java.io.tmpdir"),
    "tunji-heron-saved-state-21.ser"
).toOkioPath()