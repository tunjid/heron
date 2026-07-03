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

package com.tunjid.heron.data.ml.model

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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.HashingSink
import okio.Path
import okio.buffer

/**
 * [InferenceModelManager] backed by Hugging Face-hosted `.litertlm` files. Only one
 * download runs at a time, so a single mutex + single status flow is enough — no
 * per-model bookkeeping.
 */
class GemmaModelManager(
    private val httpClient: HttpClient,
    private val fileSystem: FileSystem,
    private val modelsDirectory: Path,
    private val ioDispatcher: CoroutineDispatcher,
    private val authTokenProvider: AuthTokenProvider = AuthTokenProvider.None,
) : InferenceModelManager {

    private val downloadMutex = Mutex()
    private val currentStatus = MutableStateFlow<ModelStatus>(ModelStatus.NotDownloaded)

    override fun status(
        model: InferenceModel,
    ): StateFlow<ModelStatus> =
        currentStatus.asStateFlow()

    override fun download(
        model: InferenceModel,
    ): Flow<DownloadProgress> = flow {
        downloadMutex.withLock {
            performDownload(model.asGemma()) { emit(it) }
        }
    }.flowOn(ioDispatcher)

    override suspend fun ensure(
        model: InferenceModel,
    ): LoadedModel = withContext(ioDispatcher) {
        downloadMutex.withLock {
            val gemma = model.asGemma()
            val destination = modelPath(gemma)
            if (fileSystem.exists(destination)) {
                LoadedModel(gemma, destination)
                    .also { currentStatus.value = ModelStatus.Ready(it) }
            } else {
                performDownload(gemma) {}
                LoadedModel(gemma, destination)
            }
        }
    }

    override suspend fun delete(model: InferenceModel): Unit = withContext(ioDispatcher) {
        downloadMutex.withLock {
            fileSystem.delete(modelPath(model.asGemma()), mustExist = false)
            currentStatus.value = ModelStatus.NotDownloaded
        }
    }

    private suspend fun performDownload(
        model: GemmaModel,
        emitProgress: suspend (DownloadProgress) -> Unit,
    ) {
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
                        currentStatus.value = ModelStatus.Downloading(progress)
                        emitProgress(progress)
                    }
                } finally {
                    sink.close()
                }

                currentStatus.value = ModelStatus.Verifying
                val expected = model.sha256
                if (expected != null && !expected.equals(hashingSink.hash.hex(), ignoreCase = true)) {
                    fileSystem.delete(partial, mustExist = false)
                    error("Checksum mismatch for ${model.name}")
                }
                fileSystem.atomicMove(partial, destination)
            }
            currentStatus.value = ModelStatus.Ready(LoadedModel(model, destination))
        } catch (throwable: Throwable) {
            fileSystem.delete(partial, mustExist = false)
            currentStatus.value = ModelStatus.Failed(throwable.message ?: "Download failed")
            throw throwable
        }
    }

    private fun modelPath(model: GemmaModel): Path =
        modelsDirectory / model.modelFile

    private companion object {
        const val DownloadBufferSize = 64 * 1024
        const val PartialSuffix = ".part"
    }
}
