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

import com.tunjid.heron.data.ml.model.InferenceModel
import com.tunjid.heron.data.ml.model.LoadedModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath

private val ModelA = LoadedModel(
    model = InferenceModel.Gemma31B,
    path = "model-a".toPath(),
)
private val ModelB = LoadedModel(
    model = InferenceModel.Gemma4E2B,
    path = "model-b".toPath(),
)

/** [BaseInferenceEngine] with programmable [onLoad] behavior for exercising the state machine. */
private class FakeInferenceEngine(
    private val onLoadBehavior: suspend (LoadedModel) -> Unit = {},
) : BaseInferenceEngine() {

    var loadCount = 0
        private set
    var resetCount = 0
        private set

    override suspend fun onLoad(model: LoadedModel) {
        loadCount++
        onLoadBehavior(model)
    }

    override suspend fun onReset() {
        resetCount++
    }

    override fun generate(
        prompt: String,
        params: GenerationParams,
    ): Flow<String> = emptyFlow()
}

class BaseInferenceEngineTest {

    @Test
    fun loadingSameReadyModelIsANoOp() = runTest {
        val engine = FakeInferenceEngine()

        engine.load(ModelA)
        engine.load(ModelA)

        assertEquals(1, engine.loadCount)
        assertEquals(EngineState.Ready(ModelA), engine.state.value)
    }

    @Test
    fun loadingADifferentModelReloads() = runTest {
        val engine = FakeInferenceEngine()

        engine.load(ModelA)
        engine.load(ModelB)

        assertEquals(2, engine.loadCount)
        assertEquals(EngineState.Ready(ModelB), engine.state.value)
    }

    @Test
    fun loadRetriesAfterAnError() = runTest {
        var shouldFail = true
        val engine = FakeInferenceEngine(
            onLoadBehavior = { if (shouldFail) error("boom") },
        )

        assertFailsWith<IllegalStateException> { engine.load(ModelA) }
        assertIs<EngineState.Error>(engine.state.value)
        assertEquals(1, engine.loadCount)

        // Same model, but the engine is errored — not skipped, it retries.
        shouldFail = false
        engine.load(ModelA)

        assertEquals(2, engine.loadCount)
        assertEquals(EngineState.Ready(ModelA), engine.state.value)
    }

    @Test
    fun cancelledLoadResetsToUninitialized() = runTest {
        val gate = CompletableDeferred<Unit>()
        val engine = FakeInferenceEngine(onLoadBehavior = { gate.await() })

        val job = launch { engine.load(ModelA) }
        advanceUntilIdle()
        assertEquals(EngineState.Loading(ModelA), engine.state.value)

        job.cancel()
        advanceUntilIdle()

        // Must not strand the engine at Loading with nothing actually loaded.
        assertEquals(EngineState.Uninitialized, engine.state.value)
    }

    @Test
    fun concurrentDuplicateLoadsRunOnce() = runTest {
        val gate = CompletableDeferred<Unit>()
        val engine = FakeInferenceEngine(onLoadBehavior = { gate.await() })

        val first = launch { engine.load(ModelA) }
        val second = launch { engine.load(ModelA) }
        advanceUntilIdle()
        // First holds the lock mid-load; second is queued on the mutex.
        assertEquals(EngineState.Loading(ModelA), engine.state.value)

        gate.complete(Unit)
        advanceUntilIdle()
        first.join()
        second.join()

        // The second caller observes Ready and repeats no work.
        assertEquals(1, engine.loadCount)
        assertEquals(EngineState.Ready(ModelA), engine.state.value)
    }
}
