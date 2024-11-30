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

import com.tunjid.heron.data.network.ApiUrl
import com.tunjid.heron.data.network.BaseUrl
import com.tunjid.heron.data.network.KtorNetworkService
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.di.SingletonScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

class DataModule(

)


@SingletonScope
@Component
abstract class InjectedDataComponent(
    private val module: DataModule
) {

    @Provides
    internal fun appUrl(): BaseUrl = ApiUrl

    @Provides
    internal fun provideAppJson() = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    @Provides
    internal fun provideAppProtoBuff() = ProtoBuf {
    }


    internal val KtorNetworkService.bind: NetworkService
        @SingletonScope
        @Provides get() = this

}