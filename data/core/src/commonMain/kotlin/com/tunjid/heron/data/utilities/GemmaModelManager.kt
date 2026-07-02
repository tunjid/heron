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

package com.tunjid.heron.data.utilities

import com.tunjid.heron.data.ml.model.GemmaModel
import com.tunjid.heron.data.ml.model.LoadedModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.HashingSink
import okio.Path
import okio.buffer

/**
 * Downloads, stores, verifies and reports on on-device [GemmaModel] files.
 * Model files are large (multiple GB) and fetched on demand — see the managed
 * download design in the plan.
 */
interface GemmaModelManager {
    /** Observable status for [model]. */
    fun status(model: GemmaModel): StateFlow<ModelStatus>

    /** Returns a ready [LoadedModel], downloading and verifying first if needed. */
    suspend fun ensure(model: GemmaModel): LoadedModel

    /** Cold flow that performs the download and emits progress. */
    fun download(model: GemmaModel): Flow<DownloadProgress>

    /** Removes the downloaded file for [model]. */
    suspend fun delete(model: GemmaModel)
}

sealed interface ModelStatus {
    data object NotDownloaded : ModelStatus
    data class Downloading(val progress: DownloadProgress) : ModelStatus
    data object Verifying : ModelStatus
    data class Ready(val model: LoadedModel) : ModelStatus
    data class Failed(val message: String) : ModelStatus
}

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
) {
    /** Download completion in `[0, 1]`, or `0` when the total size is unknown. */
    val fraction: Float
        get() = if (totalBytes <= 0L) 0f else bytesDownloaded.toFloat() / totalBytes
}

/** Supplies a bearer token for gated model hosts (e.g. Hugging Face license gating). */
interface AuthTokenProvider {
    suspend fun bearerToken(): String?

    object None : AuthTokenProvider {
        override suspend fun bearerToken(): String? = null
    }
}

class DefaultGemmaModelManager(
    private val httpClient: HttpClient,
    private val fileSystem: FileSystem,
    private val modelsDirectory: Path,
    private val ioDispatcher: CoroutineDispatcher,
    private val authTokenProvider: AuthTokenProvider = AuthTokenProvider.None,
) : GemmaModelManager {

    // Keyed by modelId. Lazily created; see note in statusFlow.
    private val statuses = mutableMapOf<String, MutableStateFlow<ModelStatus>>()

    override fun status(model: GemmaModel): StateFlow<ModelStatus> =
        statusFlow(model).asStateFlow()

    override fun download(model: GemmaModel): Flow<DownloadProgress> = flow {
        performDownload(model) { emit(it) }
    }.flowOn(ioDispatcher)

    override suspend fun ensure(model: GemmaModel): LoadedModel = withContext(ioDispatcher) {
        val destination = modelPath(model)
        if (fileSystem.exists(destination)) {
            LoadedModel(model, destination)
                .also { statusFlow(model).value = ModelStatus.Ready(it) }
        } else {
            performDownload(model) {}
            LoadedModel(model, destination)
        }
    }

    override suspend fun delete(model: GemmaModel): Unit = withContext(ioDispatcher) {
        fileSystem.delete(modelPath(model), mustExist = false)
        statusFlow(model).value = ModelStatus.NotDownloaded
    }

    private suspend fun performDownload(
        model: GemmaModel,
        emitProgress: suspend (DownloadProgress) -> Unit,
    ) {
        val status = statusFlow(model)
        val destination = modelPath(model)
        val partial = modelsDirectory / (model.modelFile + PartialSuffix)
        try {
            fileSystem.createDirectories(modelsDirectory)
            val token = authTokenProvider.bearerToken()

            httpClient.prepareGet(model.downloadUrl) {
                if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
            }.execute { response ->
                if (response.status != HttpStatusCode.OK) {
                    error("Download failed for ${model.name}: HTTP ${response.status.value}")
                }
                val channel: ByteReadChannel = response.body()
                val total = response.contentLength() ?: model.sizeInBytes
                var downloaded = 0L

                val hashingSink = HashingSink.sha256(fileSystem.sink(partial))
                val sink = hashingSink.buffer()
                try {
                    val buffer = ByteArray(DownloadBufferSize)
                    while (true) {
                        val read = channel.readAvailable(buffer)
                        if (read <= 0) break
                        sink.write(buffer, 0, read)
                        downloaded += read
                        val progress = DownloadProgress(downloaded, total)
                        status.value = ModelStatus.Downloading(progress)
                        emitProgress(progress)
                    }
                } finally {
                    sink.close()
                }

                status.value = ModelStatus.Verifying
                val expected = model.sha256
                if (expected != null && !expected.equals(hashingSink.hash.hex(), ignoreCase = true)) {
                    fileSystem.delete(partial, mustExist = false)
                    error("Checksum mismatch for ${model.name}")
                }
                fileSystem.atomicMove(partial, destination)
            }
            status.value = ModelStatus.Ready(LoadedModel(model, destination))
        } catch (throwable: Throwable) {
            fileSystem.delete(partial, mustExist = false)
            status.value = ModelStatus.Failed(throwable.message ?: "Download failed")
            throw throwable
        }
    }

    private fun modelPath(model: GemmaModel): Path =
        modelsDirectory / model.modelFile

    // NOTE: creation is not concurrency-hardened; adequate for the current single
    // consumer. Revisit with a lock if the manager is driven from multiple threads.
    private fun statusFlow(model: GemmaModel): MutableStateFlow<ModelStatus> =
        statuses.getOrPut(model.modelId) {
            MutableStateFlow(
                if (fileSystem.exists(modelPath(model))) {
                    ModelStatus.Ready(LoadedModel(model, modelPath(model)))
                } else {
                    ModelStatus.NotDownloaded
                },
            )
        }

    private companion object {
        const val DownloadBufferSize = 64 * 1024
        const val PartialSuffix = ".part"
    }
}
