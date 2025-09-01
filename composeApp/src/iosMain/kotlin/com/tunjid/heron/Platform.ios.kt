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

import com.tunjid.heron.data.database.getDatabaseBuilder
import com.tunjid.heron.data.di.DataBindingArgs
import com.tunjid.heron.images.imageLoader
import com.tunjid.heron.media.video.StubVideoPlayerController
import com.tunjid.heron.scaffold.scaffold.AppState
import dev.jordond.connectivity.Connectivity
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
        imageLoader = ::imageLoader,
        videoPlayerController = {
            StubVideoPlayerController
        },
        args = { appScope ->
            DataBindingArgs(
                appScope = appScope,
                connectivity = Connectivity(),
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
