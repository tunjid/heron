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

import dev.jordond.connectivity.Connectivity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * An interface that reports if there's a network connection
 */
interface NetworkMonitor {
    val isConnected: Flow<Boolean>
}

internal class ConnectivityNetworkMonitor(
    @Named("AppScope")
    appScope: CoroutineScope,
    private val connectivity: Connectivity,
) : NetworkMonitor {
    override val isConnected: Flow<Boolean> = flow {
        connectivity.start()
        try {
            emitAll(
                connectivity.statusUpdates.map { status ->
                    when (status) {
                        is Connectivity.Status.Connected -> true
                        Connectivity.Status.Disconnected -> false
                    }
                }
            )
        } finally {
            connectivity.stop()
        }
    }
        .stateIn(
            scope = appScope,
            initialValue = true,
            started = SharingStarted.WhileSubscribed(3000),
        )
}
