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

package com.tunjid.heron.data.ml.di

import com.tunjid.heron.data.ml.engine.GemmaEngine
import com.tunjid.heron.data.ml.engine.createGemmaEngine
import com.tunjid.heron.data.ml.model.AuthTokenProvider
import com.tunjid.heron.data.ml.model.DefaultGemmaModelManager
import com.tunjid.heron.data.ml.model.GemmaModelManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import okio.FileSystem
import okio.Path

/**
 * Platform-supplied values for [MlBindings], constructed in the app entry point
 * the same way [com.tunjid.heron.data.di.DataBindingArgs] is.
 */
class MlBindingArgs(
    val httpClient: HttpClient,
    val fileSystem: FileSystem,
    val modelsDirectory: Path,
    val ioDispatcher: CoroutineDispatcher,
    val authTokenProvider: AuthTokenProvider = AuthTokenProvider.None,
)

@BindingContainer
class MlBindings(
    private val args: MlBindingArgs,
) {

    @SingleIn(AppScope::class)
    @Provides
    fun provideGemmaModelManager(): GemmaModelManager = DefaultGemmaModelManager(
        httpClient = args.httpClient,
        fileSystem = args.fileSystem,
        modelsDirectory = args.modelsDirectory,
        ioDispatcher = args.ioDispatcher,
        authTokenProvider = args.authTokenProvider,
    )

    @SingleIn(AppScope::class)
    @Provides
    fun provideGemmaEngine(): GemmaEngine = createGemmaEngine(args.ioDispatcher)
}
