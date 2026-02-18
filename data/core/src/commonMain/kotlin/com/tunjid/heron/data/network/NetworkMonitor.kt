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

import com.tunjid.heron.data.di.AppMainScope
import com.tunjid.heron.data.di.IODispatcher
import dev.jordond.connectivity.Connectivity
import dev.zacsweers.metro.Inject
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus

/** An interface that reports if there's a network connection */
interface NetworkMonitor {
    val isConnected: Flow<Boolean>
}

internal class ConnectivityNetworkMonitor
@Inject
constructor(
    @AppMainScope appMainScope: CoroutineScope,
    @IODispatcher ioDispatcher: CoroutineDispatcher,
    private val connectivity: Connectivity,
) : NetworkMonitor {
    override val isConnected: Flow<Boolean> =
        flow {
                connectivity.start()
                try {
                    emitAll(connectivity.statusUpdates.map { it.isConnected })
                } finally {
                    connectivity.stop()
                }
            }
            .stateIn(
                scope = appMainScope + ioDispatcher,
                initialValue = true,
                // TODO: Can this be WhileSubscribed?
                //  The backing library isn't thread safe for start / stop.
                started = SharingStarted.Lazily,
            )
}

class NetworkConnectionException(val url: Url, cause: Throwable) :
    Exception("Network error attempting to reach $url", cause)

internal expect fun Throwable.isNetworkConnectionError(): Boolean
