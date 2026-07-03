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
import com.tunjid.heron.data.ml.model.asGemma
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [InferenceEngine] that adapts the Swift-implemented [IosInferenceBridge] callbacks into
 * the coroutine API. Inference happens in Swift/LiteRTLM; this only marshals.
 */
internal class IosInferenceEngine(
    private val bridge: IosInferenceBridge,
    private val ioDispatcher: CoroutineDispatcher,
) : InferenceEngine {

    // Serializes state transitions across load/close.
    private val mutex = Mutex()

    private val _state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    override suspend fun load(model: LoadedModel) = mutex.withLock {
        _state.value = EngineState.Loading
        try {
            suspendCancellableCoroutine { continuation ->
                bridge.load(
                    modelPath = model.path.toString(),
                    maxTokens = model.model.asGemma().defaultConfig.maxTokens,
                    onReady = { if (continuation.isActive) continuation.resume(Unit) },
                    onError = { message ->
                        if (continuation.isActive) {
                            continuation.resumeWithException(IllegalStateException(message))
                        }
                    },
                )
            }
            _state.value = EngineState.Ready(model)
        } catch (throwable: Throwable) {
            _state.value = EngineState.Error(throwable.message ?: "Failed to load model", throwable)
            throw throwable
        }
    }

    override fun generate(
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

    override suspend fun close() = mutex.withLock {
        bridge.close()
        _state.value = EngineState.Uninitialized
    }
}
