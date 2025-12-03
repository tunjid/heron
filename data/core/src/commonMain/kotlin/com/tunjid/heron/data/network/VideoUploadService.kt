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

import app.bsky.video.GetJobStatusResponse
import com.atproto.server.GetServiceAuthQueryParams
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.files.FileManager
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.inCurrentProfileSession
import com.tunjid.heron.data.repository.signedInAuth
import com.tunjid.heron.data.utilities.Collections
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.utils.io.ByteReadChannel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.model.Blob

interface VideoUploadService {
    suspend fun uploadVideo(
        file: File,
    ): Result<Blob>
}

internal class SuspendingVideoUploadService @Inject constructor(
    httpClient: HttpClient,
    private val networkService: NetworkService,
    private val fileManager: FileManager,
    private val savedStateDataSource: SavedStateDataSource,
) : VideoUploadService {

    private val videoUploadClient = httpClient.config {
        install(DefaultRequest) {
            url.takeFrom(VideoUploadServiceEndpoint)
        }
        install(Logging) {
            level = LogLevel.INFO
            logger = object : Logger {
                override fun log(message: String) {
//                    println("Logger Ktor => $message")
                }
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 40.seconds.inWholeMilliseconds
        }
    }

    // Video upload implementation as recommended in:
    // https://docs.bsky.app/docs/tutorials/video#recommended-method
    override suspend fun uploadVideo(
        file: File,
    ): Result<Blob> = savedStateDataSource.inCurrentProfileSession { signedInProfileId ->
        if (signedInProfileId == null) throw IllegalStateException("Not signed in")

        val authToken = savedStateDataSource.signedInAuth
            .filterNotNull()
            .first()

        val serviceUrl = authToken.serviceUrl ?: throw IllegalStateException("Not signed in")

        networkService.runCatchingWithMonitoredNetworkRetry {
            getServiceAuth(
                GetServiceAuthQueryParams(
                    aud = serviceUrlDid(serviceUrl),
                    exp = Clock.System.now().epochSeconds + 30.minutes.inWholeSeconds,
                    lxm = Nsid(Collections.UploadVideo),
                ),
            )
        }.mapCatchingUnlessCancelled { tokenResponse ->
            videoUploadClient.post(UploadVideoEndpoint) {
                url {
                    parameters.append(
                        name = DidQueryParam,
                        value = authToken.authProfileId.id,
                    )
                    parameters.append(
                        name = NameQueryParam,
                        value = Clock.System.now().toEpochMilliseconds().toString(),
                    )
                }
                bearerAuth(tokenResponse.token)
                contentType(ContentType.Video.MP4)
                setBody(ByteReadChannel(fileManager.source(file)))
                headers[ContentLengthHeaderKey] = fileManager.size(file).toString()
            }
                .let {
                    if (it.status.isSuccess()) it.body<VideoUploadResponse>()
                    else throw IOException("Video upload failed with status: ${it.status}")
                }
        }.mapCatchingUnlessCancelled { uploadResponse ->
            repeat(MaxVideoUploadStatusCheckCount) {
                runCatchingUnlessCancelled {
                    videoUploadClient.get(VideoStatusEndpoint) {
                        parameter(JobIdQueryParam, uploadResponse.jobId)
                    }
                        .body<GetJobStatusResponse>()
                }
                    .getOrNull()
                    ?.jobStatus
                    ?.blob
                    ?.let { processedBlob ->
                        return@mapCatchingUnlessCancelled processedBlob
                    }
                delay(MaxVideoUploadStatusCheckPollInterval)
            }

            throw IOException("Video upload timed out")
        }
    } ?: Result.failure(IOException("Video upload failed: Invalid user session."))
}

private fun serviceUrlDid(serviceUrl: String) =
    Did("did:web:${Url(serviceUrl).host}")

@Serializable
private data class VideoUploadResponse(
    val did: String,
    val jobId: String,
    val state: String,
)

private const val VideoUploadServiceEndpoint = "https://video.bsky.app"
private const val UploadVideoEndpoint = "/xrpc/app.bsky.video.uploadVideo"
private const val VideoStatusEndpoint = "/xrpc/app.bsky.video.getJobStatus"

private const val ContentLengthHeaderKey = "Content-Length"
private const val DidQueryParam = "did"
private const val NameQueryParam = "name"
private const val JobIdQueryParam = "jobId"

private const val MaxVideoUploadStatusCheckCount = 60

private const val MaxVideoUploadStatusCheckPollInterval = 2_000L
