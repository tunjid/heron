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

package com.tunjid.heron.data.ml.engine

import com.tunjid.heron.data.ml.model.LoadedModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * On-device LLM inference engine. A single instance wraps one loaded model and
 * streams generated tokens. The concrete model/runtime (e.g. Gemma via LiteRT-LM)
 * is an implementation detail of the platform factories.
 */
interface InferenceEngine {
    val state: StateFlow<EngineState>

    /** Loads [model] into the engine, transitioning [state] to [EngineState.Ready] on success. */
    suspend fun load(
        model: LoadedModel,
    )

    /** Streams the model's response to [prompt] token-by-token. Requires a prior [load]. */
    fun generate(
        prompt: String,
        params: GenerationParams = GenerationParams(),
    ): Flow<String>

    /**
     * Releases the loaded model and returns the engine to
     * [EngineState.Uninitialized]. The engine remains usable; it can be
     * [load]ed again afterwards.
     */
    suspend fun reset()
}

sealed interface EngineState {
    data object Uninitialized : EngineState
    data class Loading(
        val model: LoadedModel,
    ) : EngineState
    data class Ready(
        val model: LoadedModel,
    ) : EngineState
    data class Error(
        val model: LoadedModel,
        val message: String,
        val cause: Throwable? = null,
    ) : EngineState
}

/** Decoding parameters. Defaults mirror the AI Edge Gallery `defaultConfig` for Gemma. */
@Serializable
data class GenerationParams(
    val temperature: Float = 1.0f,
    val topK: Int = 64,
    val topP: Float = 0.95f,
    val maxTokens: Int = 4096,
)

/**
 * Hardware backend an [InferenceEngine] runs a model on. Kept a plain enum because the
 * value crosses the Kotlin/Native boundary into the Swift inference bridge on iOS.
 */
enum class InferenceBackend {
    Cpu,
    Gpu,
}

/**
 * Device models — matched case-insensitively as substrings of the reported device model —
 * whose GPU delegate is unreliable for LiteRT-LM and must fall back to CPU. Mirrors the
 * Google AI Edge Gallery heuristic, which force-disables the GPU on the Pixel 10.
 */
internal val CpuOnlyDeviceModels: List<String> = listOf(
    "pixel 10",
)

/**
 * The backend policy: GPU by default, CPU when [deviceModel] matches an entry in
 * [CpuOnlyDeviceModels]. Extracted from [preferredBackend] so it is unit-testable without a
 * real device. A null [deviceModel] (unknown device) defaults to GPU.
 */
internal fun backendFor(
    deviceModel: String?,
): InferenceBackend {
    val normalized = deviceModel?.lowercase() ?: return InferenceBackend.Gpu
    return when {
        CpuOnlyDeviceModels.any { normalized.contains(it) } -> InferenceBackend.Cpu
        else -> InferenceBackend.Gpu
    }
}

/** The [InferenceBackend] to run [model] on, resolved for the current device and platform. */
internal expect fun preferredBackend(
    model: LoadedModel,
): InferenceBackend
