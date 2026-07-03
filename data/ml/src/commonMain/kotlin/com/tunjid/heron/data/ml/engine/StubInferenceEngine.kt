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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/**
 * A placeholder [InferenceEngine] used by every platform actual until the real
 * LiteRT-LM binding is wired. It compiles and reports [EngineState.Error] with
 * [reason] rather than performing inference, so the module builds green on every
 * target while the SDK integration is pending.
 */
internal class StubInferenceEngine(
    private val reason: String,
) : InferenceEngine {

    private val _state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    override suspend fun load(model: LoadedModel) {
        _state.value = EngineState.Error(reason)
    }

    override fun generate(
        prompt: String,
        params: GenerationParams,
    ): Flow<String> = flow {
        throw UnsupportedOperationException(reason)
    }

    override suspend fun reset() {
        _state.value = EngineState.Uninitialized
    }
}
