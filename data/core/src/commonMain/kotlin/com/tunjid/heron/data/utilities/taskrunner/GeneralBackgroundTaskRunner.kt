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

package com.tunjid.heron.data.utilities.taskrunner

import com.tunjid.heron.data.files.FileManager
import com.tunjid.heron.data.files.asSystemFile
import com.tunjid.heron.data.files.path
import com.tunjid.heron.data.tasks.BackgroundTaskRunner
import com.tunjid.heron.data.tasks.Progress
import com.tunjid.heron.data.tasks.Task
import com.tunjid.heron.data.tasks.TaskId
import com.tunjid.heron.data.tasks.TaskStore
import com.tunjid.heron.data.utilities.writequeue.WriteProgress
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.HashingSource
import okio.Source
import okio.blackholeSink
import okio.buffer
import okio.use

@Inject
internal class GeneralBackgroundTaskRunner(
    private val taskStore: TaskStore,
    private val httpClient: HttpClient,
    private val fileManager: FileManager,
    private val writeQueue: WriteQueue,
) : BackgroundTaskRunner {

    override suspend fun run(
        id: TaskId,
        onProgress: suspend (Progress) -> Unit,
    ): Result<Unit> {
        val task = taskStore.pending
            .first()
            .firstOrNull { it.id == id }
            ?: return Result.failure(
                IllegalStateException("No pending task for ${id.value}"),
            )
        return try {
            when (task) {
                is Task.Download -> {
                    onProgress(
                        Progress(
                            completedBytes = 0L,
                            totalBytes = task.sizeInBytes,
                        ),
                    )
                    download(
                        request = task,
                        authHeader = null, // TODO: resolve a gated-host bearer token at run time.
                        onProgress = onProgress,
                    )
                }
                is Task.Write -> processWrite(
                    task = task,
                    onProgress = onProgress,
                )
            }
            taskStore.remove(id)
            Result.success(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            taskStore.markFailed(
                id = id,
                reason = throwable.message,
            )
            Result.failure(throwable)
        }
    }

    /**
     * Streams [request] to its on-disk destination, resuming from an on-disk `.part` file (via an HTTP
     * `Range` header) when one is present and verifying the SHA-256 when [Task.Download.sha256] is set.
     * The transfer overrides the shared client's short request timeout for its own request only.
     */
    private suspend fun download(
        request: Task.Download,
        authHeader: String?,
        onProgress: suspend (Progress) -> Unit,
    ) {
        val destination = request.destination
        // Unwrap once for the path arithmetic below (parent directory + ".part" sibling).
        val destinationPath = destination.path
        val directory = requireNotNull(destinationPath.parent)
        val partial = (directory / (destinationPath.name + PartialSuffix)).asSystemFile()
        fileManager.createDirectories(directory.asSystemFile())

        if (fileManager.exists(destination)) {
            val destinationSize = fileManager.metadataSize(destination) ?: 0L
            if (destinationSize == request.sizeInBytes) {
                when (request.sha256?.lowercase()) {
                    null,
                    hash(fileManager.fileSystem.source(destination.path)),
                    -> return onProgress(
                        Progress(
                            completedBytes = request.sizeInBytes,
                            totalBytes = request.sizeInBytes,
                        ),
                    )
                    else
                    -> fileManager.delete(destination)
                }
            }
        }

        val existing = fileManager.metadataSize(partial) ?: 0L

        if (existing < request.sizeInBytes) {
            onProgress(Progress(existing, request.sizeInBytes))

            httpClient.prepareGet(request.sourceUrl) {
                // The shared client's default request timeout is seconds; a transfer needs hours.
                timeout { requestTimeoutMillis = DownloadRequestTimeout.inWholeMilliseconds }
                if (authHeader != null) header(HttpHeaders.Authorization, authHeader)
                if (existing > 0L) header(HttpHeaders.Range, "bytes=$existing-")
            }.execute { response ->
                val resumed = response.status == HttpStatusCode.PartialContent
                if (!resumed && response.status != HttpStatusCode.OK) {
                    error("Download failed for ${destinationPath.name}: HTTP ${response.status.value}")
                }
                // Server ignored our Range: start the partial file over.
                if (!resumed && existing > 0L) fileManager.delete(partial)

                val startFrom = if (resumed) existing else 0L
                val remaining = response.contentLength()
                val total = when {
                    remaining == null -> request.sizeInBytes
                    resumed -> startFrom + remaining
                    else -> remaining
                }

                val channel = response.bodyAsChannel()
                val sink = when {
                    resumed -> fileManager.fileSystem.appendingSink(partial.path)
                    else -> fileManager.fileSystem.sink(partial.path)
                }.buffer()
                var downloaded = startFrom
                sink.use {
                    val buffer = ByteArray(DownloadBufferSize)
                    // Multi-GB files download in tens of thousands of chunks; only surface a new
                    // progress value when the whole-number percent changes.
                    var lastPercent = -1
                    var lastReportedBytes = startFrom
                    while (true) {
                        val read = channel.readAvailable(buffer)
                        if (read <= 0) break
                        it.write(buffer, 0, read)
                        downloaded += read
                        val percent = if (total > 0L) (downloaded * 100 / total).toInt() else -1
                        val shouldReport = if (total > 0L) {
                            percent != lastPercent
                        } else {
                            downloaded - lastReportedBytes >= 1024 * 1024
                        }
                        if (shouldReport) {
                            lastPercent = percent
                            lastReportedBytes = downloaded
                            onProgress(Progress(downloaded, total))
                        }
                    }
                }
            }
        } else {
            onProgress(Progress(request.sizeInBytes, request.sizeInBytes))
        }

        val expected = request.sha256
        if (expected != null) {
            // Streaming hash can't survive a resume, so hash the finished file once.
            val actual = hash(fileManager.fileSystem.source(partial.path))
            if (!expected.equals(actual, ignoreCase = true)) {
                fileManager.delete(partial)
                error("Checksum mismatch for ${destinationPath.name}")
            }
        }
        fileManager.delete(destination)
        fileManager.atomicMove(
            source = partial,
            target = destination,
        )
    }

    private suspend fun processWrite(
        task: Task.Write,
        onProgress: suspend (Progress) -> Unit,
    ) = coroutineScope {
        val writeProgress = WriteProgress()
        val reporting = launch {
            writeProgress.updates.collect(onProgress)
        }
        try {
            withContext(writeProgress) {
                writeQueue.processInBackgroundOrThrow(
                    task = task,
                )
            }
        } finally {
            reporting.cancel()
        }
    }
}

private fun hash(
    source: Source,
): String = source.use {
    HashingSource.sha256(it)
        .use { hashing ->
            hashing.buffer().readAll(blackholeSink())
            hashing.hash.hex()
        }
        .lowercase()
}

private val DownloadRequestTimeout = 2.hours
private const val DownloadBufferSize = 64 * 1024
private const val PartialSuffix = ".part"
