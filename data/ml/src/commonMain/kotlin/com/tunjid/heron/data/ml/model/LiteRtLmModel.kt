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
 * The internal [InferenceModel] implementation: a `.litertlm` file.
 *
 * [url] resolves the exact [commitHash] pinned in the model repo, so the download is
 * reproducible for a given [LiteRtLmModel].
 */
@Serializable
internal data class LiteRtLmModel(
    override val name: String,
    val modelId: String,
    val info: String,
    override val learnMoreUrl: String,
    val modelFile: String,
    val url: String,
    val commitHash: String,
    override val sizeInBytes: Long,
    val minDeviceMemoryInGb: Int,
    val sha256: String? = null,
    val defaultConfig: GenerationParams = GenerationParams(),
    val inputModes: Set<InputMode>,
    override val abilities: List<InferenceModel.Ability>,
) : InferenceModel

/** An input the model can consume. [Text] is always supported; richer modes vary by model. */
@Serializable
internal sealed class InputMode {
    @Serializable
    data object Text : InputMode()

    @Serializable
    data object Image : InputMode()

    @Serializable
    data object Audio : InputMode()
}

internal fun InferenceModel.asLiteRtLmModel(): LiteRtLmModel = this as LiteRtLmModel
