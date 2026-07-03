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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Downloads, stores, verifies and reports on on-device [InferenceModel] files.
 * Model files are large (multiple GB) and fetched on demand.
 */
interface InferenceModelManager {
    /** Observable status for [model]. */
    fun status(model: InferenceModel): StateFlow<ModelStatus>

    /** Returns a ready [LoadedModel], downloading and verifying first if needed. */
    suspend fun ensure(model: InferenceModel): LoadedModel

    /** Cold flow that performs the download and emits progress. */
    fun download(model: InferenceModel): Flow<DownloadProgress>

    /** Removes the downloaded file for [model]. */
    suspend fun delete(model: InferenceModel)
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
