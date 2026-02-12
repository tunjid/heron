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

import com.tunjid.heron.data.lexicons.BlueskyApi
import com.tunjid.heron.data.lexicons.XrpcBlueskyApi
import com.tunjid.heron.data.utilities.AtProtoException
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.runCatchingWithNetworkRetry
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.currentCoroutineContext
import sh.christian.ozone.api.response.AtpResponse

internal interface NetworkService {

    /**
     * Catches network related exceptions and wraps them in a failure result.
     * If the [BlueskyApi] indicates an [AtpResponse.Failure], it is also considered a failure
     * and also returns a failure result.
     */
    suspend fun <T : Any> runCatchingWithMonitoredNetworkRetry(
        times: Int = 3,
        initialDelay: Duration = 100.milliseconds,
        maxDelay: Duration = 4.seconds,
        factor: Double = 2.0,
        block: suspend BlueskyApi.() -> AtpResponse<T>,
    ): Result<T>
}

@Inject
internal class KtorNetworkService(
    httpClient: HttpClient,
    sessionManager: SessionManager,
    private val networkMonitor: NetworkMonitor,
) : NetworkService {
    private val api = XrpcBlueskyApi(
        httpClient = httpClient.config {
            sessionManager.manage(config = this)
        },
    )

    private val validatingApi = XrpcBlueskyApi(
        httpClient = httpClient.config {
            sessionManager.manage(config = this)
        },
    )

    override suspend fun <T : Any> runCatchingWithMonitoredNetworkRetry(
        times: Int,
        initialDelay: Duration,
        maxDelay: Duration,
        factor: Double,
        block: suspend BlueskyApi.() -> AtpResponse<T>,
    ): Result<T> = networkMonitor.runCatchingWithNetworkRetry(
        times,
        initialDelay,
        maxDelay,
        factor,
        block = {
            block(
                when (currentCoroutineContext()[SessionContext.Key]) {
                    is SessionContext.Previous -> validatingApi
                    else -> api
                },
            )
        },
    ).mapCatchingUnlessCancelled { atpResponse ->
        when (atpResponse) {
            is AtpResponse.Failure -> throw AtProtoException(
                statusCode = atpResponse.statusCode.code,
                error = atpResponse.error?.error,
                message = atpResponse.error?.message,
            )

            is AtpResponse.Success -> atpResponse.response
        }
    }
}
