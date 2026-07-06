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

package com.tunjid.heron.data.ml.language

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import platform.NaturalLanguage.NLLanguageRecognizer

/**
 * [LanguageDetector] backed by Apple's on-device `NLLanguageRecognizer` from the
 * Natural Language framework. `dominantLanguage` yields a BCP-47 language code
 * (e.g. `"en"`, `"zh-Hant"`) or `null` when the recognizer cannot decide.
 */
internal class NaturalLanguageDetector(
    private val ioDispatcher: CoroutineDispatcher,
) : LanguageDetector {

    override suspend fun detectLanguageTag(text: String): String? {
        if (text.isBlank()) return null
        return withContext(ioDispatcher) {
            // A fresh recognizer per call keeps detection free of shared mutable state.
            NLLanguageRecognizer()
                .apply { processString(text) }
                .dominantLanguage
        }
    }
}

fun createLanguageDetector(
    ioDispatcher: CoroutineDispatcher,
): LanguageDetector = NaturalLanguageDetector(
    ioDispatcher = ioDispatcher,
)
