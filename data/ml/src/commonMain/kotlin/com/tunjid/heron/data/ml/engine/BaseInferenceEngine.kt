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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Base [InferenceEngine] that owns the [state] machine shared by every platform: it
 * serializes [load]/[reset] with [mutex], makes [load] idempotent, and normalizes error and
 * cancellation handling. Subclasses implement only the platform bits — [onLoad], [onReset],
 * and [generate].
 */
internal abstract class BaseInferenceEngine : InferenceEngine {

    /** Serializes access to the platform engine across [load]/[generate]/[reset]. */
    protected val mutex = Mutex()

    final override val state: StateFlow<EngineState>
        field = MutableStateFlow<EngineState>(EngineState.Uninitialized)

    final override suspend fun load(
        model: LoadedModel,
    ) = mutex.withLock {
        // Idempotent: loading the already-ready model repeats no work. A different model, an
        // errored engine, an uninitialized engine, or a dangling Loading (left behind by a
        // cancelled load) all fall through and (re)load. Concurrent duplicate loads are
        // serialized by [mutex]; later callers observe Ready here and no-op.
        val current = state.value
        if (current is EngineState.Ready && current.model == model) return@withLock

        state.value = EngineState.Loading(model)
        try {
            onLoad(model)
            state.value = EngineState.Ready(model)
        } catch (cancellation: CancellationException) {
            // A cancelled load leaves nothing loaded; don't strand [state] at Loading.
            state.value = EngineState.Uninitialized
            throw cancellation
        } catch (throwable: Throwable) {
            state.value = EngineState.Error(
                model = model,
                message = throwable.message ?: "Failed to load model",
                cause = throwable,
            )
            throw throwable
        }
    }

    final override suspend fun reset() = mutex.withLock {
        onReset()
        state.value = EngineState.Uninitialized
    }

    /**
     * Loads [model] into the platform engine, replacing any previously loaded model and
     * throwing on failure. Called under [mutex] with [state] already at [EngineState.Loading];
     * the caller records [EngineState.Ready] / [EngineState.Error] from the outcome.
     */
    protected abstract suspend fun onLoad(model: LoadedModel)

    /** Releases the platform engine, leaving the bridge/runtime reusable. Called under [mutex]. */
    protected abstract suspend fun onReset()
}
