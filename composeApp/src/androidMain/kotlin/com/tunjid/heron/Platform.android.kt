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

import android.content.Context
import android.os.Build
import com.tunjid.heron.data.database.getDatabaseBuilder
import com.tunjid.heron.data.di.DataBindingArgs
import com.tunjid.heron.media.video.ExoplayerController
import com.tunjid.heron.scaffold.scaffold.AppState
import kotlinx.coroutines.Dispatchers
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

fun createAppState(context: Context): AppState =
    createAppState(
        videoPlayerController = { appScope ->
            ExoplayerController(
                context = context,
                scope = appScope,
                diffingDispatcher = Dispatchers.Default,
            )
        },
        args = { appScope ->
            DataBindingArgs(
                appScope = appScope,
                savedStatePath = context.savedStatePath(),
                savedStateFileSystem = FileSystem.SYSTEM,
                databaseBuilder = getDatabaseBuilder(context),
            )
        },
    )

private fun Context.savedStatePath(): Path =
    filesDir.resolve("savedState").absolutePath.toPath()