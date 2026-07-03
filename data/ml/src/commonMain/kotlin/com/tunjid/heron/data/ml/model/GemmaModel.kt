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

package com.tunjid.heron.data.ml.model

import com.tunjid.heron.data.ml.engine.GenerationParams
import kotlinx.serialization.Serializable

/**
 * The internal [InferenceModel] implementation: a Gemma `.litertlm` file. Carries
 * the download and inference details that are deliberately kept off the public
 * [InferenceModel] surface. Shape mirrors an AI Edge Gallery `model_allowlist.json`
 * entry so a remote allowlist can populate it later.
 */
@Serializable
internal data class GemmaModel(
    val name: String,
    val modelId: String,
    val modelFile: String,
    val downloadUrl: String,
    override val sizeInBytes: Long,
    val estimatedPeakMemoryInBytes: Long,
    val version: String,
    val defaultConfig: GenerationParams = GenerationParams(),
    val sha256: String? = null,
) : InferenceModel

/**
 * Narrows an [InferenceModel] to its concrete [GemmaModel]. Safe because
 * [InferenceModel] is sealed and [GemmaModel] is its only implementation.
 */
internal fun InferenceModel.asGemma(): GemmaModel = this as GemmaModel
