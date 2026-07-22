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
import com.tunjid.heron.data.ml.engine.FoundationModelsAvailability
import com.tunjid.heron.data.ml.engine.FoundationModelsBridge
import com.tunjid.heron.data.ml.engine.InferenceSource
import com.tunjid.heron.data.ml.engine.platformInferenceCapability
import com.tunjid.heron.data.ml.model.AppleFoundationModel
import com.tunjid.heron.data.ml.model.InferenceModel
import com.tunjid.heron.data.ml.model.InferenceModelManager
import com.tunjid.heron.data.ml.model.LoadedModel
import com.tunjid.heron.data.ml.model.ModelStatus
import com.tunjid.heron.data.ml.model.PlatformUnavailableReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * iOS [InferenceModelManager] over Apple Foundation Models. There is no download catalog — a single
 * system model whose readiness is the OS-reported [FoundationModelsAvailability]. [source]
 * combines the [platformInferenceCapability] OS gate with that availability, so an ineligible device
 * resolves to [InferenceSource.None] (inference UI hidden). Availability is pushed from the
 * bridge (refreshed on app foreground), so toggling Apple Intelligence in Settings is reflected live.
 */
class FoundationModelsManager(
    bridge: FoundationModelsBridge,
) : InferenceModelManager {

    // null until the first availability check completes.
    private val availability = MutableStateFlow<FoundationModelsAvailability?>(null)

    override val source: Flow<InferenceSource> =
        availability.map(FoundationModelsAvailability?::toCapability)

    override val models: List<InferenceModel> = listOf(AppleFoundationModel)

    init {
        // The bridge is the availability source: observe it and pull an initial value. Subsequent
        // re-checks are driven from the app foreground (the AppDelegate refreshes the bridge).
        bridge.setAvailabilityListener { availability.value = it }
        bridge.refreshAvailability()
    }

    override fun status(
        model: InferenceModel,
    ): Flow<ModelStatus> = availability.map { it.toStatus() }

    // No download lifecycle for a system model.
    override suspend fun enqueueDownload(model: InferenceModel.External): Outcome = Outcome.Success

    override suspend fun cancelDownload(model: InferenceModel.External) = Unit

    override suspend fun delete(model: InferenceModel.External) = Unit
}

private fun FoundationModelsAvailability?.toCapability(): InferenceSource = when {
    // OS gate (iOS < 26) short-circuits before we ever consult the model.
    platformInferenceCapability() != InferenceSource.Platform -> InferenceSource.None
    // Not checked yet, or the hardware isn't Apple-Intelligence eligible: no inference at all.
    this == null || this == FoundationModelsAvailability.DeviceNotEligible -> InferenceSource.None
    else -> InferenceSource.Platform
}

private fun FoundationModelsAvailability?.toStatus(): ModelStatus = when (this) {
    FoundationModelsAvailability.Available ->
        ModelStatus.Available(LoadedModel.System(AppleFoundationModel))
    FoundationModelsAvailability.AppleIntelligenceNotEnabled ->
        ModelStatus.Unavailable(PlatformUnavailableReason.AppleIntelligenceDisabled)
    FoundationModelsAvailability.ModelNotReady ->
        ModelStatus.Unavailable(PlatformUnavailableReason.ModelDownloading)
    // Ineligible / not-yet-checked won't be shown (capability is None), but status must be total.
    FoundationModelsAvailability.DeviceNotEligible, null ->
        ModelStatus.Unavailable(PlatformUnavailableReason.AppleIntelligenceDisabled)
}
