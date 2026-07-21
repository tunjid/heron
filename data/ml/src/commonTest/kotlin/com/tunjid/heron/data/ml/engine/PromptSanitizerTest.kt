/*
 *    Copyright 2026 Adetunji Dahunsi
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

package com.tunjid.heron.data.ml.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PromptSanitizerTest {

    @Test
    fun leaves_plain_bmp_text_including_curly_quotes_and_dashes_unchanged() {
        // Curly apostrophe (U+2019) and em dash (U+2014) fill the vibe prompt; they encode
        // identically in modified and standard UTF-8, so they must survive untouched.
        val text = "Google’s relationship with the web is ending — traffic is down -40%."
        assertEquals(text, stripModifiedUtf8Hazards(text))
    }

    @Test
    fun strips_the_supplementary_character_that_crashed_the_desktop_parser() {
        // The exact failure: a post ending in emoji U+1F914. It is a valid surrogate pair, but the
        // desktop JNI's modified-UTF-8 conversion turns it into ill-formed standard UTF-8.
        val postText = "Some are now arguing that the AI boom is different. " +
            charArrayOf('\uD83E', '\uDD14').concatToString() // U+1F914
        assertEquals(
            "Some are now arguing that the AI boom is different. ",
            stripModifiedUtf8Hazards(postText),
        )
    }

    @Test
    fun strips_all_surrogates_and_nul_and_leaves_no_hazards() {
        val hazardous = charArrayOf(
            'a',
            '\uD83D', '\uDE00', // emoji U+1F600
            Char(0), // NUL
            '\uD800', // lone high surrogate
            'b',
            '\uD83D', '\uDD25', // emoji U+1F525
            '\uD83C', '\uDF0D', // emoji U+1F30D
        ).concatToString()

        val sanitized = stripModifiedUtf8Hazards(hazardous)

        assertEquals("ab", sanitized)
        assertFalse(sanitized.any { it.isSurrogate() }, "no surrogate code unit may remain")
        assertFalse(sanitized.any { it.code == 0 }, "no NUL may remain")
    }
}
