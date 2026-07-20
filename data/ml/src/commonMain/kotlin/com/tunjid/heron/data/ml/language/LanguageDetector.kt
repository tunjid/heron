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

interface LanguageDetector {
    /**
     * Returns a BCP-47 language tag (e.g. `"en"`, `"pt"`, `"zh-Hant"`) for the dominant
     * language of [text], or `null` when it cannot be determined.
     */
    suspend fun detectLanguageTag(text: String): String?
}

internal object NoOpLanguageDetector : LanguageDetector {
    override suspend fun detectLanguageTag(text: String): String? = null
}

/**
 * The English display name of the language identified by [languageTag] (a BCP-47 tag such as
 * `"de"`, `"en-US"`, or `"zh-Hant"`) — e.g. `"German"`, `"English"`, `"Chinese"`. Falls back to
 * [languageTag] verbatim when the platform cannot resolve a name.
 */
expect fun englishDisplayName(languageTag: String): String
