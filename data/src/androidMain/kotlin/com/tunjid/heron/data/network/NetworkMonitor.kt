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

package com.tunjid.heron.data.network

//import android.content.Context
//import android.net.ConnectivityManager
//import android.net.ConnectivityManager.NetworkCallback
//import android.net.Network
//import android.net.NetworkCapabilities
//import android.net.NetworkRequest.Builder
//import androidx.core.content.getSystemService
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.channels.awaitClose
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.SharingStarted
//import kotlinx.coroutines.flow.callbackFlow
//import kotlinx.coroutines.flow.stateIn
//
//actual class NetworkMonitor(
//    scope: CoroutineScope,
//    private val context: Context,
//) {
//    actual val isConnected: Flow<Boolean> = callbackFlow {
//        val callback = object : NetworkCallback() {
//            override fun onAvailable(network: Network) {
//                if (!channel.isClosedForSend) channel.trySend(true)
//            }
//
//            override fun onLost(network: Network) {
//                if (!channel.isClosedForSend) channel.trySend(false)
//            }
//        }
//
//        val connectivityManager = context.getSystemService<ConnectivityManager>()
//        connectivityManager?.registerNetworkCallback(
//            Builder()
//                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
//                .build(),
//            callback
//        )
//
//        awaitClose {
//            connectivityManager?.unregisterNetworkCallback(callback)
//        }
//    }
//        .stateIn(
//            scope = scope,
//            started = SharingStarted.WhileSubscribed(2000),
//            initialValue = false
//        )
//}
