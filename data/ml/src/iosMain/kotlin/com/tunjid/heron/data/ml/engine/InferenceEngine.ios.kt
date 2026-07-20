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
import platform.Foundation.NSProcessInfo

/**
 * [InferenceEngine] that adapts the Swift-implemented [IosInferenceBridge] callbacks into
 * the coroutine API. Inference happens in Swift/LiteRTLM; this only marshals. The [state]
 * machine and idempotent [load] live in [BaseInferenceEngine].
 */
internal class IosInferenceEngine(
    private val bridge: IosInferenceBridge,
    private val ioDispatcher: CoroutineDispatcher,
) : BaseInferenceEngine() {

    override suspend fun onLoad(
        model: LoadedModel,
    ) = suspendCancellableCoroutine { continuation ->
        bridge.load(
            modelPath = model.file.relativePath,
            maxTokens = model.model.maxTokens,
            backend = backendFor(),
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
 * Builds the iOS engine from the Swift-provided [bridge]. Call from the app entry
 * point (`createAppState`) with the bridge Swift hands in, then inject the result —
 * no global state. Exposed to Swift as `InferenceEngine_iosKt.createInferenceEngine`.
 */
fun createInferenceEngine(
    bridge: IosInferenceBridge,
    ioDispatcher: CoroutineDispatcher,
): InferenceEngine = IosInferenceEngine(
    bridge = bridge,
    ioDispatcher = ioDispatcher,
)

internal actual fun backendFor(): InferenceBackend =
    if (isSimulator()) InferenceBackend.Cpu else InferenceBackend.Gpu

private fun isSimulator(): Boolean =
    NSProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"] != null
