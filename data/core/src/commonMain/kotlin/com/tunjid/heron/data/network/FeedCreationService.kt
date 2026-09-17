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

import com.tunjid.heron.data.core.types.AtProtoException
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.data.graze.GrazeFeed
import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.logging.loggableText
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.expiredSessionResult
import com.tunjid.heron.data.repository.inCurrentProfileSession
import com.tunjid.heron.data.utilities.asJsonContent
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import dev.zacsweers.metro.Inject
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import social.heron.graze.CreateFeedRequest
import social.heron.graze.DeleteFeedRequest
import social.heron.graze.EditFeedRequest
import social.heron.graze.FeedResult
import social.heron.graze.GetFeedQueryParams

internal interface FeedCreationService {

    suspend fun updateGrazeFeed(
        update: GrazeFeed.Update,
    ): Result<GrazeResponse>
}

/**
 * Manages Graze feed generators through the Heron AppView's XRPC methods (`social.heron.graze.*`).
 * Each call is proxied to the AppView by the user's PDS the same way reads are (see
 * [HeronProxyPaths]), so the PDS mints the service-auth (`aud` = the AppView DID) transparently and
 * the client neither talks to a bespoke endpoint nor mints its own service-auth. The AppView then
 * authenticates to Graze with its own platform credentials.
 */
@Inject
internal class GrazeFeedCreationService(
    private val networkService: NetworkService,
    private val savedStateDataSource: SavedStateDataSource,
) : FeedCreationService {

    override suspend fun updateGrazeFeed(
        update: GrazeFeed.Update,
    ): Result<GrazeResponse> = savedStateDataSource.inCurrentProfileSession { signedInProfileId ->
        if (signedInProfileId == null) return@inCurrentProfileSession expiredSessionResult()

        when (update) {
            is GrazeFeed.Update.Create -> networkService.runCatchingWithMonitoredNetworkRetry {
                createFeed(update.feed.toCreateRequest())
            }.mapCatchingUnlessCancelled(FeedResult::toCreated)

            is GrazeFeed.Update.Edit -> networkService.runCatchingWithMonitoredNetworkRetry {
                editFeed(update.feed.toEditRequest())
            }.mapCatchingUnlessCancelled(FeedResult::toEdited)

            is GrazeFeed.Update.Get -> networkService.runCatchingWithMonitoredNetworkRetry {
                getFeed(GetFeedQueryParams(rkey = update.recordKey.value))
            }.mapCatchingUnlessCancelled(FeedResult::toRead)

            is GrazeFeed.Update.Delete -> networkService.runCatchingWithMonitoredNetworkRetry {
                deleteFeed(DeleteFeedRequest(rkey = update.recordKey.value))
            }.foldToDeleted(recordKey = update.recordKey)
        }.onFailure { throwable ->
            logcat(LogPriority.DEBUG) {
                "Failed graze call for ${update::class.simpleName}: ${throwable.loggableText()}"
            }
        }
    } ?: expiredSessionResult()
}

private fun GrazeFeed.Editable.toCreateRequest() = CreateFeedRequest(
    rkey = recordKey.value,
    displayName = displayName,
    description = description,
    filter = filter.asJsonContent(Filter.Root.serializer()),
)

private fun GrazeFeed.Editable.toEditRequest() = EditFeedRequest(
    rkey = recordKey.value,
    displayName = displayName,
    description = description,
    filter = filter.asJsonContent(Filter.Root.serializer()),
)

private fun FeedResult.toCreated(): GrazeResponse = GrazeResponse.Created(
    rkey = requireOk().let { RecordKey(rkey) },
    contentMode = contentMode.orEmpty(),
)

private fun FeedResult.toEdited(): GrazeResponse = GrazeResponse.Edited(
    rkey = requireOk().let { RecordKey(rkey) },
    contentMode = contentMode.orEmpty(),
)

private fun FeedResult.toRead(): GrazeResponse = GrazeResponse.Read(
    rkey = requireOk().let { RecordKey(rkey) },
    contentMode = contentMode.orEmpty(),
    algorithm = checkNotNull(algorithm) {
        "Graze getFeed returned no algorithm manifest"
    }.decodeAs(),
)

/**
 * The AppView passes Graze's upstream HTTP status straight through, so deleting a feed that no
 * longer exists surfaces as a 404 (an [AtProtoException]) rather than a [FeedResult] body. Treat
 * that as an idempotent success, matching the prior direct-HTTP client.
 */
private fun Result<FeedResult>.foldToDeleted(
    recordKey: RecordKey,
): Result<GrazeResponse> = fold(
    onSuccess = { Result.success(GrazeResponse.Deleted(rkey = RecordKey(it.rkey))) },
    onFailure = { throwable ->
        if (throwable is AtProtoException && throwable.statusCode == HttpStatusCode.NotFound.value) {
            Result.success(GrazeResponse.Deleted(rkey = recordKey))
        } else {
            Result.failure(throwable)
        }
    },
)

private fun FeedResult.requireOk(): FeedResult = apply {
    check(ok) { "Graze call failed with status $status: $error" }
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
            val order: String?,
            val manifest: Manifest,
        )

        @Serializable
        data class Manifest(
            val filter: Filter.Root,
        )
    }
}
