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

import com.tunjid.heron.data.repository.SavedStateRepository
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
import sh.christian.ozone.BlueskyApi
import sh.christian.ozone.XrpcBlueskyApi

interface NetworkService {
    val api: BlueskyApi
}

@Inject
class KtorNetworkService(
    private val json: Json,
    savedStateRepository: SavedStateRepository,
) : NetworkService {
    override val api = XrpcBlueskyApi(
        HttpClient {
            expectSuccess = true

            install(DefaultRequest) {
                url.takeFrom("https://bsky.social")
            }

            install(ContentNegotiation) {
                json(
                    json = json,
                    contentType = ContentType.Application.Json
                )
            }

            install(AuthPlugin) {
                this.networkErrorConverter = {
                    json.decodeFromString(it)
                }
                this.readAuth = {
                    savedStateRepository.savedState.first().auth
                }
                this.saveAuth = {
                    savedStateRepository.updateState {
                        copy(auth = it)
                    }
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
        }
    )
}
