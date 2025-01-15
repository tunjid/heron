package com.tunjid.heron

import com.tunjid.heron.data.database.getDatabaseBuilder
import com.tunjid.heron.data.di.DataModule
import com.tunjid.heron.media.video.NoOpVideoPlayerController
import com.tunjid.heron.scaffold.scaffold.AppState
import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

fun createAppState(): AppState =
    createAppState(
        videoPlayerController = {
            NoOpVideoPlayerController
        },
        dataModule = { appScope ->
            DataModule(
                appScope = appScope,
                savedStatePath = savedStatePath(),
                savedStateFileSystem = FileSystem.SYSTEM,
                databaseBuilder = getDatabaseBuilder(),
            )
        },
    )

@OptIn(ExperimentalForeignApi::class)
private fun savedStatePath(): Path {
    val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return (requireNotNull(documentDirectory).path + "/heron").toPath()
}