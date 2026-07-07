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

import android.content.Context
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import android.view.textclassifier.TextLanguage
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal class TextClassifierLanguageDetector(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : LanguageDetector {

    // The system TextClassifier is process-wide and reusable, so resolve it once and cache
    // it. Null only if the platform reports no TextClassificationManager (never on a real
    // device), in which case detection degrades to null.
    private val textClassifier: TextClassifier? by lazy {
        context.getSystemService(TextClassificationManager::class.java)
            ?.textClassifier
    }

    override suspend fun detectLanguageTag(text: String): String? {
        if (text.isBlank()) return null
        return withContext(ioDispatcher) {
            val classifier = textClassifier ?: return@withContext null
            val request = TextLanguage.Request.Builder(text).build()
            classifier.detectLanguage(request)
                .takeIf { it.localeHypothesisCount > 0 }
                ?.getLocale(0)
                ?.toLanguageTag()
        }
    }
}

fun createLanguageDetector(
    context: Context,
    ioDispatcher: CoroutineDispatcher,
): LanguageDetector = TextClassifierLanguageDetector(
    context = context,
    ioDispatcher = ioDispatcher,
)

actual fun englishDisplayName(languageTag: String): String =
    Locale.forLanguageTag(languageTag)
        .getDisplayLanguage(Locale.ENGLISH)
        .ifBlank { languageTag }
