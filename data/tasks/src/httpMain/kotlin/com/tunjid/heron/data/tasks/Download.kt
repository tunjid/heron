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

package com.tunjid.heron.data.tasks

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import okio.HashingSource
import okio.Path
import okio.Source
import okio.blackholeSink
import okio.buffer
import okio.use

/**
 * Streams [request] to [destination] using this [HttpClient], resuming from an on-disk `.part` file
 * (via an HTTP `Range` header) when one is present and verifying the SHA-256 when
 * [Task.Download.sha256] is set.
 *
 * Shared by the JVM-family schedulers (Android + desktop). Each platform supplies [onProgress] so it
 * can surface progress its own way — a WorkManager / UIDT notification on Android, a `StateFlow` on
 * desktop.
 *
 * File I/O goes through the scheduler's [FileManager][com.tunjid.heron.data.files.FileManager] so that
 * the terminal move — which makes the finished file appear — is observable via its mutation signal;
 * streaming uses the exposed [okio.FileSystem] directly.
 */
suspend fun BackgroundTaskScheduler.download(
    request: Task.Download,
    destination: Path,
    authHeader: String?,
    onProgress: suspend (Progress) -> Unit,
) {
    val directory = requireNotNull(destination.parent)
    val partial = directory / (destination.name + PartialSuffix)
    fileManager.createDirectories(directory)

    if (fileManager.exists(destination)) {
        val destinationSize = fileManager.metadataSize(destination) ?: 0L
        if (destinationSize == request.sizeInBytes) {
            when (request.sha256?.lowercase()) {
                null,
                hash(fileManager.fileSystem.source(destination)),
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
            if (authHeader != null) header(HttpHeaders.Authorization, authHeader)
            if (existing > 0L) header(HttpHeaders.Range, "bytes=$existing-")
        }.execute { response ->
            val resumed = response.status == HttpStatusCode.PartialContent
            if (!resumed && response.status != HttpStatusCode.OK) {
                error("Download failed for ${destination.name}: HTTP ${response.status.value}")
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
                resumed -> fileManager.fileSystem.appendingSink(partial)
                else -> fileManager.fileSystem.sink(partial)
            }.buffer()
            var downloaded = startFrom
            sink.use {
                val buffer = ByteArray(DownloadBufferSize)
                // Multi-GB files download in tens of thousands of chunks; only surface a new progress
                // value when the whole-number percent changes.
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
        val actual = hash(fileManager.fileSystem.source(partial))
        if (!expected.equals(actual, ignoreCase = true)) {
            fileManager.delete(partial)
            error("Checksum mismatch for ${destination.name}")
        }
    }
    fileManager.delete(destination)
    fileManager.atomicMove(
        source = partial,
        target = destination,
    )
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

private const val DownloadBufferSize = 64 * 1024
private const val PartialSuffix = ".part"
