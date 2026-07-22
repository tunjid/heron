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

import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.ml.engine.InferenceSource
import com.tunjid.heron.data.tasks.TaskStatus
import kotlinx.coroutines.flow.Flow

interface InferenceModelManager {

    val source: Flow<InferenceSource>

    val models: List<InferenceModel>

    fun status(
        model: InferenceModel,
    ): Flow<ModelStatus>

    suspend fun enqueueDownload(
        model: InferenceModel.External,
    ): Outcome

    suspend fun cancelDownload(
        model: InferenceModel.External,
    )

    suspend fun delete(
        model: InferenceModel.External,
    )
}

sealed interface ModelStatus {
    /**
     * A model ready to load: either a downloaded file ([LoadedModel.FileBacked]) or the platform
     * system model ([LoadedModel.System]).
     */
    data class Available(
        val loadedModel: LoadedModel,
    ) : ModelStatus

    /** A downloadable model not yet on disk (download not started, running, or failed). */
    data class Pending(
        val taskStatus: TaskStatus,
    ) : ModelStatus

    /** A platform system model present on this device but not usable right now. */
    data class Unavailable(
        val reason: PlatformUnavailableReason,
    ) : ModelStatus
}

/** Why a platform system model is present but not currently usable. */
enum class PlatformUnavailableReason {
    /** Apple Intelligence is turned off in Settings. */
    AppleIntelligenceDisabled,

    /** The system is still downloading/preparing the model. */
    ModelDownloading,
}
