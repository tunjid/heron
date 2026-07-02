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
 * A downloadable on-device model. Shape mirrors an entry in the AI Edge Gallery
 * `model_allowlist.json` so a remote allowlist can populate it later.
 */
@Serializable
data class GemmaModel(
    /** Display name, e.g. "Gemma-4-E2B-it-int4". */
    val name: String,
    /** Hugging Face repo id the file is hosted under. */
    val modelId: String,
    /** File name on disk once downloaded, e.g. "gemma-4-E2B-it-int4.litertlm". */
    val modelFile: String,
    /** Absolute URL to fetch the model file from (Hugging Face `resolve` URL). */
    val downloadUrl: String,
    val sizeInBytes: Long,
    /** Estimated peak runtime memory; used to gate availability against device RAM. */
    val estimatedPeakMemoryInBytes: Long,
    val version: String,
    /** Forward-compat runtime tag; v1 ships LiteRT-LM only. */
    val runtime: Runtime = Runtime.LiteRtLm,
    val defaultConfig: GenerationParams = GenerationParams(),
    /** Optional hex SHA-256 for integrity verification after download. */
    val sha256: String? = null,
) {
    enum class Runtime {
        LiteRtLm,
        MediaPipe,
    }

    companion object {
        // NOTE: exact modelId / modelFile / downloadUrl / sizeInBytes / sha256 values
        // are placeholders pending confirmation of the published Gemma 4 `.litertlm`
        // artifacts (see plan open item 2). The shape is correct; the constants are not
        // yet load-bearing because no engine consumes them until the SDK is wired.
        val Gemma4E2B = GemmaModel(
            name = "Gemma-4-E2B-it",
            modelId = "google/gemma-4-E2B-it-litert",
            modelFile = "gemma-4-E2B-it-int4.litertlm",
            downloadUrl = "https://huggingface.co/google/gemma-4-E2B-it-litert/resolve/main/gemma-4-E2B-it-int4.litertlm",
            sizeInBytes = 2_580_000_000L,
            estimatedPeakMemoryInBytes = 5_905_580_032L,
            version = "20250520",
        )

        val Gemma4E4B = GemmaModel(
            name = "Gemma-4-E4B-it",
            modelId = "google/gemma-4-E4B-it-litert",
            modelFile = "gemma-4-E4B-it-int4.litertlm",
            downloadUrl = "https://huggingface.co/google/gemma-4-E4B-it-litert/resolve/main/gemma-4-E4B-it-int4.litertlm",
            sizeInBytes = 3_650_000_000L,
            estimatedPeakMemoryInBytes = 9_372_000_000L,
            version = "20250520",
        )

        /** Bundled default catalog; may be superseded by a remote allowlist later. */
        val defaultCatalog = listOf(Gemma4E2B, Gemma4E4B)
    }
}
