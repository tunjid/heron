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

import com.atproto.repo.UploadBlobResponse
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.files.FileManager
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.writequeue.WriteProgress
import com.tunjid.heron.data.utilities.writequeue.reportingUploadProgress
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import sh.christian.ozone.api.model.Blob

internal suspend fun FileManager.mediaFileUploads(
    files: List<File.Media>,
    upload: suspend FileManager.(File.Media) -> Result<Blob>,
): Result<List<MediaBlob>> {
    if (files.isEmpty()) return Result.success(emptyList())

    val writeProgress = currentCoroutineContext()[WriteProgress]
    if (writeProgress != null) files.forEach { media ->
        writeProgress.append(
            file = media,
            sizeInBytes = size(media),
            // The video service encodes videos after upload.
            processedRemotely = media is File.Media.Video,
        )
    }

    return runCatchingUnlessCancelled {
        coroutineScope {
            files.map { media ->
                async {
                    // Media rehydrated from a draft arrives without dimensions, as a draft
                    // stores only a path and its alt text. Read them back off the file so
                    // the embed still declares an aspect ratio.
                    val file = withDimensionsOrSelf(media)
                    upload(this@mediaFileUploads, file).map(file::with)
                }
            }
                .awaitAll()
                .map(Result<MediaBlob>::getOrThrow)
        }
    }
}

context(fileManager: FileManager)
internal suspend fun NetworkService.uploadFileBlob(
    file: File,
): Result<Blob> = runCatchingWithMonitoredNetworkRetry {
    fileManager.source(file).use {
        uploadBlob(ByteReadChannel(it.reportingUploadProgress(file = file)))
            .map(UploadBlobResponse::blob)
    }
}
