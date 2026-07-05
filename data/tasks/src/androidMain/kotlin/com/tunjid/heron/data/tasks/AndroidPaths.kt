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

package com.tunjid.heron.data.tasks

import android.content.Context
import okio.Path
import okio.Path.Companion.toOkioPath

/**
 * The on-device models directory, matching where `EntryPoint.android.kt` points the inference
 * engine. Computed from [Context] so an OS-instantiated worker/job can resolve it without DI.
 */
internal fun Context.modelsDirectory(): Path =
    noBackupFilesDir.resolve("models").toOkioPath()
