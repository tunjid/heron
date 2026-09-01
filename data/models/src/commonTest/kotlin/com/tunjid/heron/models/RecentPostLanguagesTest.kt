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

package com.tunjid.heron.models

import com.tunjid.heron.data.core.models.PostLanguageSelection
import com.tunjid.heron.data.core.models.withMostRecentPostLanguage
import kotlin.test.Test
import kotlin.test.assertEquals

class RecentPostLanguagesTest {

    @Test
    fun promotesANewSelectionToTheFront() {
        assertEquals(
            expected = listOf(lang("de"), lang("en"), lang("cs")),
            actual = listOf(lang("en"), lang("cs"))
                .withMostRecentPostLanguage(selection = lang("de")),
        )
    }

    @Test
    fun movesAnExistingSelectionToTheFrontWithoutDuplicating() {
        assertEquals(
            expected = listOf(lang("cs"), lang("en"), lang("de")),
            actual = listOf(lang("en"), lang("cs"), lang("de"))
                .withMostRecentPostLanguage(selection = lang("cs")),
        )
    }

    @Test
    fun capsAtSixDroppingTheOldest() {
        val full = listOf("en", "cs", "de", "fr", "es", "pt").map(::lang)
        assertEquals(
            expected = listOf("ja", "en", "cs", "de", "fr", "es").map(::lang),
            actual = full.withMostRecentPostLanguage(selection = lang("ja")),
        )
    }

    @Test
    fun ignoresAnEmptySelection() {
        val history = listOf(lang("en"), lang("cs"))
        assertEquals(
            expected = history,
            actual = history.withMostRecentPostLanguage(
                selection = PostLanguageSelection(tags = emptyList()),
            ),
        )
    }

    @Test
    fun treatsDifferentlyOrderedCombosAsDistinct() {
        val enThenCs = PostLanguageSelection(tags = listOf("en", "cs"))
        val csThenEn = PostLanguageSelection(tags = listOf("cs", "en"))
        assertEquals(
            expected = listOf(csThenEn, enThenCs),
            actual = listOf(enThenCs).withMostRecentPostLanguage(selection = csThenEn),
        )
    }
}

private fun lang(
    tag: String,
): PostLanguageSelection =
    PostLanguageSelection(tags = listOf(tag))
