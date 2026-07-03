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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.HashingSink
import okio.Path
import okio.buffer

/**
 * [InferenceModelManager] backed by Hugging Face-hosted `.litertlm` files. Only one
 * download runs at a time (serialized by [downloadMutex]); [active] holds the single
 * model currently being tracked, so any other model reports [ModelStatus.NotDownloaded].
 */
class GemmaModelManager(
    private val httpClient: HttpClient,
    private val fileSystem: FileSystem,
    private val modelsDirectory: Path,
    private val ioDispatcher: CoroutineDispatcher,
    private val authTokenProvider: AuthTokenProvider = AuthTokenProvider.None,
) : InferenceModelManager {

    private data class Active(val model: InferenceModel, val status: ModelStatus)

    private val downloadMutex = Mutex()
    private val active = MutableStateFlow<Active?>(null)

    override fun status(
        model: InferenceModel,
    ): Flow<ModelStatus> =
        active.map { current ->
            if (current?.model == model) current.status else ModelStatus.NotDownloaded
        }

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
                    .also { active.value = Active(gemma, ModelStatus.Ready(it)) }
            } else {
                performDownload(gemma) {}
                LoadedModel(gemma, destination)
            }
        }
    }

    override suspend fun delete(model: InferenceModel): Unit = withContext(ioDispatcher) {
        downloadMutex.withLock {
            val gemma = model.asGemma()
            fileSystem.delete(modelPath(gemma), mustExist = false)
            active.value = Active(gemma, ModelStatus.NotDownloaded)
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
                        active.value = Active(model, ModelStatus.Downloading(progress))
                        emitProgress(progress)
                    }
                } finally {
                    sink.close()
                }

                active.value = Active(model, ModelStatus.Verifying)
                val expected = model.sha256
                if (expected != null && !expected.equals(hashingSink.hash.hex(), ignoreCase = true)) {
                    fileSystem.delete(partial, mustExist = false)
                    error("Checksum mismatch for ${model.name}")
                }
                fileSystem.atomicMove(partial, destination)
            }
            active.value = Active(model, ModelStatus.Ready(LoadedModel(model, destination)))
        } catch (cancellation: CancellationException) {
            // Unsubscribed / cancelled mid-download: drop the partial file and
            // report the model as not downloaded rather than failed.
            fileSystem.delete(partial, mustExist = false)
            active.value = Active(model, ModelStatus.NotDownloaded)
            throw cancellation
        } catch (throwable: Throwable) {
            fileSystem.delete(partial, mustExist = false)
            active.value = Active(model, ModelStatus.Failed(throwable.message ?: "Download failed"))
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
