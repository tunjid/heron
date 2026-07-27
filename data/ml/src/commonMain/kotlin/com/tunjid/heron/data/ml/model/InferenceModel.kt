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
 * A downloadable on-device inference model. The concrete implementation (which
 * model, served by which runtime, and how it is fetched) is an internal detail;
 * consumers pick from the predefined entries such as [Gemma4E2B] / [Gemma4E4B] and
 * otherwise treat it as opaque.
 */
sealed interface InferenceModel {
    val name: String
    val maxTokens: Int
    val abilities: List<Ability>

    /** A model fetched as a file and executed by a bundled runtime (LiteRT-LM / Gemma). */
    sealed interface External : InferenceModel {
        val sizeInBytes: Long
        val minDeviceMemoryInGb: Int
        val fileName: String

        /**
         * Terms of use the user must accept before downloading, or `null` if the model imposes none.
         */
        val termsOfServiceUrl: String?
        val sha256: String?
    }

    sealed interface Platform : InferenceModel

    companion object {
        val Gemma31B: External = LiteRtLmModel(
            name = "Gemma3-1B-IT",
            modelId = "litert-community/Gemma3-1B-IT",
            info = "A variant of [google/Gemma-3-1B-IT](https://huggingface.co/google/Gemma-3-1B-IT) with 4-bit quantization ready for deployment on Android using [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM/blob/main/kotlin/README.md).",
            termsOfServiceUrl = "https://ai.google.dev/gemma/terms",
            fileName = "gemma3-1b-it-int4.litertlm",
            commitHash = "42d538a932e8d5b12e6b3b455f5572560bd60b2c",
            sizeInBytes = 584_417_280L,
            minDeviceMemoryInGb = 6,
            defaultConfig = GenerationParams(
                maxTokens = 1024,
            ),
            inputModes = setOf(
                InputMode.Text,
            ),
            abilities = listOf(
                Ability.Translation,
                Ability.Summary,
            ),
        )

        val Gemma4E2B: External = LiteRtLmModel(
            name = "Gemma-4-E2B-it",
            modelId = "litert-community/gemma-4-E2B-it-litert-lm",
            info = "A variant of Gemma 4 E2B ready for deployment on Android using [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md). It supports multi-modality input, with up to 32K context length.",
            termsOfServiceUrl = "https://ai.google.dev/gemma/terms",
            fileName = "gemma-4-E2B-it.litertlm",
            commitHash = "6e5c4f1e395deb959c494953478fa5cec4b8008f",
            sizeInBytes = 2_588_147_712L,
            minDeviceMemoryInGb = 8,
            defaultConfig = GenerationParams(
                maxTokens = 4000,
            ),
            inputModes = setOf(
                InputMode.Text,
                InputMode.Image,
                InputMode.Audio,
            ),
            abilities = listOf(
                Ability.Translation,
                Ability.Summary,
            ),
        )

        val Gemma4E4B: External = LiteRtLmModel(
            name = "Gemma-4-E4B-it",
            modelId = "litert-community/gemma-4-E4B-it-litert-lm",
            info = "A variant of Gemma 4 E4B ready for deployment on Android using [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md). It supports multi-modality input, with up to 32K context length.",
            termsOfServiceUrl = "https://ai.google.dev/gemma/terms",
            fileName = "gemma-4-E4B-it.litertlm",
            commitHash = "28299f30ee4d43294517a4ac93abd6163412f07f",
            sizeInBytes = 3_659_530_240L,
            minDeviceMemoryInGb = 12,
            defaultConfig = GenerationParams(
                maxTokens = 4000,
            ),
            inputModes = setOf(
                InputMode.Text,
                InputMode.Image,
                InputMode.Audio,
            ),
            abilities = listOf(
                Ability.Translation,
                Ability.Summary,
            ),
        )
    }

    @Serializable
    sealed class Ability {
        @Serializable
        data object Translation : Ability()

        @Serializable
        data object Summary : Ability()
    }
}
