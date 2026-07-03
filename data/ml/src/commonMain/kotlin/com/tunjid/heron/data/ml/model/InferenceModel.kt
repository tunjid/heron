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

/**
 * A downloadable on-device inference model. The concrete implementation (which
 * model, served by which runtime, and how it is fetched) is an internal detail;
 * consumers pick from the predefined entries such as [Gemma4E2B] / [Gemma4E4B] and
 * otherwise treat it as opaque.
 */
sealed interface InferenceModel {
    /** Total download size in bytes. */
    val sizeInBytes: Long

    companion object {
        // NOTE: the concrete values are placeholders pending confirmation of the
        // published Gemma 4 `.litertlm` artifacts; not yet load-bearing.
        val Gemma4E2B: InferenceModel = GemmaModel(
            name = "Gemma-4-E2B-it",
            modelId = "google/gemma-4-E2B-it-litert",
            modelFile = "gemma-4-E2B-it-int4.litertlm",
            downloadUrl = "https://huggingface.co/google/gemma-4-E2B-it-litert/resolve/main/gemma-4-E2B-it-int4.litertlm",
            sizeInBytes = 2_580_000_000L,
            estimatedPeakMemoryInBytes = 5_905_580_032L,
            version = "20250520",
        )

        val Gemma4E4B: InferenceModel = GemmaModel(
            name = "Gemma-4-E4B-it",
            modelId = "google/gemma-4-E4B-it-litert",
            modelFile = "gemma-4-E4B-it-int4.litertlm",
            downloadUrl = "https://huggingface.co/google/gemma-4-E4B-it-litert/resolve/main/gemma-4-E4B-it-int4.litertlm",
            sizeInBytes = 3_650_000_000L,
            estimatedPeakMemoryInBytes = 9_372_000_000L,
            version = "20250520",
        )
    }
}
