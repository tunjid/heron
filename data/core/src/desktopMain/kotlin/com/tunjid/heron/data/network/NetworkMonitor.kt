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

//import io.ktor.client.HttpClient
//import io.ktor.client.request.get
//import io.ktor.client.statement.readBytes
//import io.ktor.client.statement.readRawBytes
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.channels.BufferOverflow
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.SharingStarted
//import kotlinx.coroutines.flow.buffer
//import kotlinx.coroutines.flow.distinctUntilChanged
//import kotlinx.coroutines.flow.flow
//import kotlinx.coroutines.flow.map
//import kotlinx.coroutines.flow.stateIn
//import java.io.BufferedReader
//import java.io.ByteArrayInputStream
//
//private const val NetworkPollIntervalMillis = 10_000L
//private const val NetworkMonitorWhileSubscribedMillis = 2_000L
//
///**
// * Polls for a valid internet connection. Not efficient, but eh
// */
//actual class NetworkMonitor(scope: CoroutineScope) {
//    actual val isConnected: Flow<Boolean> = flow {
//        var a = 0
//        while (true) {
//            emit(++a)
//            delay(NetworkPollIntervalMillis)
//        }
//    }
//        .buffer(
//            capacity = 1,
//            onBufferOverflow = BufferOverflow.DROP_OLDEST
//        )
//        .map {
//            exponentialBackoff(
//                initialDelay = 1_000,
//                maxDelay = 20_000,
//                default = false,
//            ) {
//                HttpClient()
//                    .use {
//                        ByteArrayInputStream(it.get("https://www.google.com").readRawBytes())
//                    }
//                    .buffered()
//                    .use {
//                        it.bufferedReader().use(BufferedReader::readText)
//                    }
//                true
//            }
//        }
//        .distinctUntilChanged()
//        .stateIn(
//            scope = scope,
//            started = SharingStarted.WhileSubscribed(NetworkMonitorWhileSubscribedMillis),
//            initialValue = false
//        )
//}

