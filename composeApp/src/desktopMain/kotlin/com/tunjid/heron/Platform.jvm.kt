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
import com.tunjid.heron.data.logging.JvmLogger
import com.tunjid.heron.images.imageLoader
import com.tunjid.heron.media.video.StubVideoPlayerController
import com.tunjid.heron.scaffold.notifications.NoOpNotifier
import com.tunjid.heron.scaffold.scaffold.AppState
import dev.jordond.connectivity.Connectivity
import java.io.File
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

fun createAppState(): AppState =
    createAppState(
        imageLoader = ::imageLoader,
        notifier = {
            NoOpNotifier
        },
        logger = {
            JvmLogger()
        },
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

private fun savedStatePath(): Path = File(
    System.getProperty("java.io.tmpdir"),
    "tunji-heron-saved-state-21.ser",
).toOkioPath()
