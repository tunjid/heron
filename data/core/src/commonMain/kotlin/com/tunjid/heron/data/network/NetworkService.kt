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

package com.tunjid.heron.data.network

import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.lexicons.BlueskyApi
import com.tunjid.heron.data.lexicons.XrpcBlueskyApi
import com.tunjid.heron.data.lexicons.XrpcSerializersModule
import com.tunjid.heron.data.local.models.SessionRequest
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.signedInAuth
import com.tunjid.heron.data.utilities.runCatchingWithNetworkRetry
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import sh.christian.ozone.api.response.AtpResponse
import sh.christian.ozone.api.runtime.buildXrpcJsonConfiguration

interface NetworkService {
    val api: BlueskyApi

    suspend fun beginOauthFlowUri(
        handle: ProfileHandle,
    ): GenericUri

    suspend fun finishOauthFlow(
        request: SessionRequest.Oauth,
    )

    suspend fun <T : Any> runCatchingWithMonitoredNetworkRetry(
        times: Int = 3,
        initialDelay: Long = 100, // 0.1 second
        maxDelay: Long = 5000, // 1 second
        factor: Double = 2.0,
        block: suspend BlueskyApi.() -> AtpResponse<T>,
    ): Result<T>
}

@Inject
class KtorNetworkService(
    savedStateDataSource: SavedStateDataSource,
    private val networkMonitor: NetworkMonitor,
) : NetworkService {
    override val api = XrpcBlueskyApi(
        HttpClient {
            expectSuccess = true

            install(DefaultRequest) {
                url.takeFrom("https://bsky.social")
            }

            install(ContentNegotiation) {
                json(
                    json = BlueskyJson,
                    contentType = ContentType.Application.Json,
                )
            }

            install(AuthPlugin) {
                this.networkErrorConverter = {
                    BlueskyJson.decodeFromString(it)
                }
                this.readAuth = {
                    // Must be signed in to use
                    savedStateDataSource.signedInAuth.first()
                }
                this.saveAuth = {
                    savedStateDataSource.setAuth(
                        auth = it,
                    )
                }
            }
            install(Logging) {
                level = LogLevel.INFO
                logger = object : Logger {
                    override fun log(message: String) {
//                        println("Logger Ktor => $message")
                    }
                }
            }
        },
    )

    override suspend fun beginOauthFlowUri(handle: ProfileHandle): GenericUri {
        TODO("Not yet implemented")
    }

    override suspend fun finishOauthFlow(request: SessionRequest.Oauth) {
        TODO("Not yet implemented")
    }

    override suspend fun <T : Any> runCatchingWithMonitoredNetworkRetry(
        times: Int,
        initialDelay: Long, // 0.1 second
        maxDelay: Long, // 1 second
        factor: Double,
        block: suspend BlueskyApi.() -> AtpResponse<T>,
    ): Result<T> = networkMonitor.runCatchingWithNetworkRetry(
        times,
        initialDelay,
        maxDelay,
        factor,
        block = { block(api) },
    )
}

internal val BlueskyJson: Json = buildXrpcJsonConfiguration(XrpcSerializersModule)
