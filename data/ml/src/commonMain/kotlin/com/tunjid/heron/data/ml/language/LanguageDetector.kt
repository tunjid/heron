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
 * The display name of the language identified by [languageTag] (a BCP-47 tag such as `"de"`,
 * `"en-US"`, or `"zh-Hant"`) rendered in the locale identified by [inLocaleTag] — e.g. tag `"de"`
 * in locale `"fr"` yields `"allemand"`. Falls back to [languageTag] when the platform cannot
 * resolve a name.
 */
expect fun languageDisplayName(
    languageTag: String,
    inLocaleTag: String,
): String

/**
 * The English display name of [languageTag] — e.g. `"German"`, `"English"`, `"Chinese"`. Used where
 * the name feeds an English-language model prompt rather than the UI, which should localise names
 * to the reader via [languageDisplayName].
 */
fun englishDisplayName(
    languageTag: String,
): String = languageDisplayName(
    languageTag = languageTag,
    inLocaleTag = "en",
)

expect fun isoLanguageTags(): List<String>

/**
 * A [Comparator] that orders strings by the collation rules of [inLocaleTag], so localised names
 * sort the way a reader of that language expects — accents and locale-specific letter order handled
 * — rather than by raw code point.
 */
expect fun localeCollator(
    inLocaleTag: String,
): Comparator<String>
