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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The iOS inference contract, implemented in Swift on top of Apple's Foundation Models framework
 * (Kotlin/Native cannot call the Swift-only SDK directly). Deliberately Swift-friendly: only
 * primitives and completion closures cross the boundary — no `Flow` or suspend functions.
 * [FoundationModelsEngine] adapts it into the coroutine [InferenceEngine], and the iOS inference
 * manager observes availability via [setAvailabilityListener]. A Swift implementation is handed to
 * Kotlin at startup via `createAppState`.
 */
interface FoundationModelsBridge {
    /**
     * Registers the [listener] notified with the system model's [FoundationModelsAvailability] on
     * every [refreshAvailability]. The iOS inference manager observes this for its capability signal.
     */
    fun setAvailabilityListener(
        listener: (availability: FoundationModelsAvailability) -> Unit,
    )

    /**
     * Re-queries availability and notifies the registered listener. Called on app foreground (Apple
     * Intelligence may have been toggled in Settings) and once at startup.
     */
    fun refreshAvailability()

    /** Opens a session with the system model; invokes exactly one of [onReady] / [onError]. */
    fun load(
        onReady: () -> Unit,
        onError: (message: String) -> Unit,
    )

    /**
     * Streams a response to [prompt]. Emits each chunk via [onToken], then exactly one of
     * [onComplete] / [onError]. Must stop if [cancel] is called.
     */
    fun generate(
        prompt: String,
        temperature: Float,
        topK: Int,
        topP: Float,
        onToken: (chunk: String) -> Unit,
        onComplete: () -> Unit,
        onError: (message: String) -> Unit,
    )

    /** Cancels an in-flight [generate]. */
    fun cancel()

    /** Releases the current session. The bridge stays usable and can [load] again. */
    fun reset()
}

/** The Foundation Models system model's availability on this device. */
enum class FoundationModelsAvailability {
    Available,
    DeviceNotEligible,
    AppleIntelligenceNotEnabled,
    ModelNotReady,
}

/**
 * [InferenceEngine] over Apple Foundation Models via the Swift-implemented [FoundationModelsBridge].
 * There is no model file — [onLoad] just opens a session. The state machine and idempotent [load]
 * live in [BaseInferenceEngine].
 */
internal class FoundationModelsEngine(
    private val bridge: FoundationModelsBridge,
    private val ioDispatcher: CoroutineDispatcher,
) : BaseInferenceEngine() {

    override suspend fun onLoad(
        model: LoadedModel,
    ) = suspendCancellableCoroutine { continuation ->
        bridge.load(
            onReady = { if (continuation.isActive) continuation.resume(Unit) },
            onError = { message ->
                if (continuation.isActive) {
                    continuation.resumeWithException(IllegalStateException(message))
                }
            },
        )
        continuation.invokeOnCancellation {
            bridge.reset()
        }
    }

    override suspend fun onReset() {
        bridge.reset()
    }

    override fun onGenerate(
        prompt: String,
        params: GenerationParams,
    ): Flow<String> = callbackFlow {
        bridge.generate(
            prompt = prompt,
            temperature = params.temperature,
            topK = params.topK,
            topP = params.topP,
            onToken = { chunk -> trySend(chunk) },
            onComplete = { close() },
            onError = { message -> close(IllegalStateException(message)) },
        )
        awaitClose { bridge.cancel() }
    }.flowOn(ioDispatcher)
}

/**
 * Builds the iOS Foundation Models engine from the Swift-provided [bridge]. Call from the app entry
 * point (`createAppState`) with the bridge Swift hands in, then inject the result — no global state.
 */
fun createFoundationModelsEngine(
    bridge: FoundationModelsBridge,
    ioDispatcher: CoroutineDispatcher,
): InferenceEngine = FoundationModelsEngine(
    bridge = bridge,
    ioDispatcher = ioDispatcher,
)
