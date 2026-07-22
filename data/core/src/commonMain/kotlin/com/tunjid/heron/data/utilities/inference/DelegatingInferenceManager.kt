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

package com.tunjid.heron.data.utilities.inference

import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.ml.engine.InferenceSource
import com.tunjid.heron.data.ml.model.InferenceModel
import com.tunjid.heron.data.ml.model.InferenceModelManager
import com.tunjid.heron.data.ml.model.ModelStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Routes [InferenceModelManager] calls to the backing manager that owns the request. When a
 * [platformManager] is present (a device with an OS-provided system model, e.g. iOS Foundation
 * Models) it defines the device's [source] and [models]; otherwise the [downloadableManager]
 * (the LiteRT-LM download catalog) does. Per-model calls route by the model's variant, so the two
 * kinds coexist without either knowing about the other.
 */
internal class DelegatingInferenceManager(
    private val platformManager: InferenceModelManager?,
    private val downloadableManager: InferenceModelManager?,
) : InferenceModelManager {

    // A platform system model, when the device has one, defines what inference is available; the
    // downloadable catalog is the fallback. A platform (e.g. iOS Foundation Models) with no download
    // catalog omits the [downloadableManager]; a device with neither has no on-device inference.
    private val primary: InferenceModelManager?
        get() = platformManager ?: downloadableManager

    override val source: Flow<InferenceSource> = primary?.source ?: flowOf(InferenceSource.None)

    override val models: List<InferenceModel> = primary?.models.orEmpty()

    override fun status(
        model: InferenceModel,
    ): Flow<ModelStatus> = managerFor(model).status(model)

    override suspend fun enqueueDownload(
        model: InferenceModel.External,
    ): Outcome = managerFor(model).enqueueDownload(model)

    override suspend fun cancelDownload(
        model: InferenceModel.External,
    ) = managerFor(model).cancelDownload(model)

    override suspend fun delete(
        model: InferenceModel.External,
    ) = managerFor(model).delete(model)

    private fun managerFor(
        model: InferenceModel,
    ): InferenceModelManager = when (model) {
        is InferenceModel.Platform -> requireNotNull(platformManager) {
            "Platform model $model requires a platform inference manager"
        }
        is InferenceModel.External -> requireNotNull(downloadableManager) {
            "Downloadable model $model requires a downloadable inference manager"
        }
    }
}
