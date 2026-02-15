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

import com.atproto.server.GetServiceAuthQueryParams
import com.tunjid.heron.data.InternalEndpoints
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.data.graze.GrazeDid
import com.tunjid.heron.data.graze.GrazeFeed
import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.logging.loggableText
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.expiredSessionResult
import com.tunjid.heron.data.repository.inCurrentProfileSession
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid

internal interface FeedCreationService {

    suspend fun updateGrazeFeed(
        update: GrazeFeed.Update,
    ): Result<GrazeResponse>
}

internal class GrazeFeedCreationService @Inject constructor(
    httpClient: HttpClient,
    private val networkService: NetworkService,
    private val savedStateDataSource: SavedStateDataSource,
) : FeedCreationService {

    private val feedCreationClient = httpClient.config {
        install(DefaultRequest) {
            url.takeFrom(InternalEndpoints.HeronEndpoint)
        }
        install(Logging) {
            level = LogLevel.INFO
            logger = object : Logger {
                override fun log(message: String) {
                    logcat(LogPriority.VERBOSE) { message }
                }
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15.seconds.inWholeMilliseconds
        }
    }

    override suspend fun updateGrazeFeed(
        update: GrazeFeed.Update,
    ): Result<GrazeResponse> =
        when (update) {
            is GrazeFeed.Update.Create -> performRequest<GrazeResponse.Created>(
                call = GrazeCall.Create,
                body = GrazeRequestBody.Feed(update.feed),
            )
            is GrazeFeed.Update.Delete -> performRequest<GrazeResponse.Deleted>(
                call = (GrazeCall.Delete),
                body = GrazeRequestBody.Key(
                    recordKey = update.recordKey,
                ),
            )

            is GrazeFeed.Update.Edit -> performRequest<GrazeResponse.Edited>(
                call = GrazeCall.Edit,
                body = GrazeRequestBody.Feed(update.feed),
            )
            is GrazeFeed.Update.Get -> performRequest<GrazeResponse.Read>(
                call = GrazeCall.Get,
                body = GrazeRequestBody.Key(
                    recordKey = update.recordKey,
                ),
            )
        }

    private suspend inline fun <reified T : GrazeResponse> performRequest(
        call: GrazeCall,
        body: GrazeRequestBody,
    ): Result<GrazeResponse> = savedStateDataSource.inCurrentProfileSession { signedInProfileId ->
        if (signedInProfileId == null) return@inCurrentProfileSession expiredSessionResult()

        networkService.runCatchingWithMonitoredNetworkRetry {
            getServiceAuth(
                GetServiceAuthQueryParams(
                    aud = Did(GrazeDid.id),
                    exp = Clock.System.now().epochSeconds + 30.minutes.inWholeSeconds,
                    lxm = call.lexicon,
                ),
            )
        }.mapCatchingUnlessCancelled { tokenResponse ->
            val response = feedCreationClient.post(call.path) {
                setBody(body.body())
                contentType(ContentType.Application.Json)
                bearerAuth(tokenResponse.token)
            }

            if (!response.status.isSuccess()) when (response.status) {
                HttpStatusCode.NotFound -> return@mapCatchingUnlessCancelled GrazeResponse.Deleted(
                    rkey = body.recordKey,
                )
                else -> throw Exception(response.bodyAsText())
            }
            response.body<T>()
        }
            .onFailure {
                logcat(LogPriority.DEBUG) {
                    "Failed graze call notification for ${call.path}: ${it.loggableText()}"
                }
            }
    } ?: expiredSessionResult()
}

private enum class GrazeCall(
    val path: String,
    val lexicon: Nsid,
) {
    Create(
        path = "/createGrazeFeed",
        lexicon = Nsid("com.atproto.repo.createRecord"),
    ),
    Edit(
        path = "/editGrazeFeed",
        lexicon = Nsid("com.atproto.repo.putRecord"),
    ),
    Delete(
        path = "/deleteGrazeFeed",
        lexicon = Nsid("com.atproto.repo.deleteRecord"),
    ),
    Get(
        path = "/getGrazeFeed",
        lexicon = Nsid("com.atproto.repo.getRecord"),
    ),
}

private sealed interface GrazeRequestBody {
    val recordKey: RecordKey

    fun body(): Any

    @Serializable
    data class Key(
        @SerialName("rkey")
        override val recordKey: RecordKey,
    ) : GrazeRequestBody {
        override fun body(): Any = this
    }

    data class Feed(
        val feed: GrazeFeed,
    ) : GrazeRequestBody {
        override val recordKey: RecordKey
            get() = feed.recordKey

        override fun body(): Any = feed
    }
}

@Serializable
internal sealed interface GrazeResponse {

    val rkey: RecordKey

    @Serializable
    data class Deleted(
        override val rkey: RecordKey,
    ) : GrazeResponse

    @Serializable
    data class Created(
        override val rkey: RecordKey,
        val contentMode: String,
    ) : GrazeResponse

    @Serializable
    data class Edited(
        override val rkey: RecordKey,
        val contentMode: String,
    ) : GrazeResponse

    @Serializable
    data class Read(
        override val rkey: RecordKey,
        val contentMode: String,
        val algorithm: Detail,
    ) : GrazeResponse {
        @Serializable
        data class Detail(
            val order: String,
            val manifest: Manifest,
        )

        @Serializable
        data class Manifest(
            val filter: Filter.Root,
        )
    }
}
