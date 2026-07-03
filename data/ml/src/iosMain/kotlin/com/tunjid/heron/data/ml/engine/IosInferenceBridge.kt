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

/**
 * The iOS inference contract implemented in Swift on top of the `LiteRTLM` Swift
 * package (Kotlin/Native cannot call the Swift-only SDK directly). It is kept
 * deliberately Swift-friendly: only primitives and completion closures cross the
 * boundary — no `Flow` or suspend functions. [IosInferenceEngine] adapts it back into
 * the coroutine-based [InferenceEngine]. A Swift implementation is handed to Kotlin at
 * startup via `createAppState`, which builds the engine with [createInferenceEngine].
 */
interface IosInferenceBridge {
    /** Loads the model at [modelPath]; invokes exactly one of [onReady] / [onError]. */
    fun load(
        modelPath: String,
        maxTokens: Int,
        onReady: () -> Unit,
        onError: (message: String) -> Unit,
    )

    /**
     * Streams a response to [prompt]. Emits each chunk via [onToken], then exactly
     * one of [onComplete] / [onError]. Must stop if [cancel] is called.
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

    /** Releases the underlying engine. The bridge stays usable and can [load] again. */
    fun reset()
}
