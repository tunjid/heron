/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.data.di

import com.tunjid.heron.data.network.KtorNetworkService
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.AuthTokenRepository
import com.tunjid.heron.data.repository.DataStoreSavedStateRepository
import com.tunjid.heron.data.repository.SavedStateRepository
import com.tunjid.heron.di.SingletonScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import okio.FileSystem
import okio.Path

class DataModule(
    val appScope: CoroutineScope,
    val savedStatePath: Path,
    val savedStateFileSystem: FileSystem,
)

@SingletonScope
@Component
abstract class DataComponent(
    private val module: DataModule
) {

    @Provides
    fun appScope(): CoroutineScope = module.appScope

    @Provides
    fun savedStatePath(): Path = module.savedStatePath

    @Provides
    fun savedStateFileSystem(): FileSystem = module.savedStateFileSystem

    @Provides
    fun provideAppJson() = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    @Provides
    fun provideAppProtoBuff() = ProtoBuf {
    }

    val KtorNetworkService.bind: NetworkService
        @SingletonScope
        @Provides get() = this

     val DataStoreSavedStateRepository.bind: SavedStateRepository
         @SingletonScope
         @Provides get() = this

    val AuthTokenRepository.bind: AuthRepository
        @SingletonScope
        @Provides get() = this
}