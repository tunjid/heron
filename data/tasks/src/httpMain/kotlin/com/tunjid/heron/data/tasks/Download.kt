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
import okio.FileSystem
import okio.HashingSource
import okio.Path
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
 */
suspend fun HttpClient.download(
    request: Task.Download,
    fileSystem: FileSystem,
    destination: Path,
    authHeader: String?,
    onProgress: suspend (Progress) -> Unit,
) {
    val directory = requireNotNull(destination.parent)
    val partial = directory / (destination.name + PartialSuffix)
    fileSystem.createDirectories(directory)
    val existing = if (fileSystem.exists(partial)) fileSystem.metadata(partial).size ?: 0L else 0L

    onProgress(Progress(existing, request.sizeInBytes))

    prepareGet(request.sourceUrl) {
        if (authHeader != null) header(HttpHeaders.Authorization, authHeader)
        if (existing > 0L) header(HttpHeaders.Range, "bytes=$existing-")
    }.execute { response ->
        val resumed = response.status == HttpStatusCode.PartialContent
        if (!resumed && response.status != HttpStatusCode.OK) {
            error("Download failed for ${destination.name}: HTTP ${response.status.value}")
        }
        // Server ignored our Range: start the partial file over.
        if (!resumed && existing > 0L) fileSystem.delete(
            path = partial,
            mustExist = false,
        )

        val startFrom = if (resumed) existing else 0L
        val remaining = response.contentLength()
        val total = when {
            remaining == null -> request.sizeInBytes
            resumed -> startFrom + remaining
            else -> remaining
        }

        val channel = response.bodyAsChannel()
        val sink = when {
            resumed -> fileSystem.appendingSink(partial)
            else -> fileSystem.sink(partial)
        }.buffer()
        var downloaded = startFrom
        sink.use {
            val buffer = ByteArray(DownloadBufferSize)
            // Multi-GB files download in tens of thousands of chunks; only surface a new progress
            // value when the whole-number percent changes.
            var lastPercent = -1
            while (true) {
                val read = channel.readAvailable(buffer)
                if (read <= 0) break
                it.write(buffer, 0, read)
                downloaded += read
                val percent = if (total > 0L) (downloaded * 100 / total).toInt() else -1
                if (total <= 0L || percent != lastPercent) {
                    lastPercent = percent
                    onProgress(Progress(downloaded, total))
                }
            }
        }
    }

    val expected = request.sha256
    if (expected != null) {
        // Streaming hash can't survive a resume, so hash the finished file once.
        val actual = HashingSource.sha256(fileSystem.source(partial)).use { hashing ->
            hashing.buffer().readAll(blackholeSink())
            hashing.hash.hex()
        }
        if (!expected.equals(actual, ignoreCase = true)) {
            fileSystem.delete(partial, mustExist = false)
            error("Checksum mismatch for ${destination.name}")
        }
    }
    fileSystem.atomicMove(partial, destination)
}

private const val DownloadBufferSize = 64 * 1024
private const val PartialSuffix = ".part"
