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

package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.GenericUri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileLinkValidationTest {

    @Test
    fun normalize_addsHttpsToBareDomains() {
        assertEquals(
            expected = GenericUri("https://patreon.com/kat"),
            actual = normalizeProfileLinkUrl(" patreon.com/kat "),
        )
    }

    @Test
    fun normalize_lowercasesTheHost() {
        assertEquals(
            expected = normalizeProfileLinkUrl("https://ko-fi.com/kat"),
            actual = normalizeProfileLinkUrl("https://Ko-Fi.COM/kat"),
        )
    }

    @Test
    fun normalize_rejectsInputThatIsNotAWebLink() {
        assertNull(normalizeProfileLinkUrl("hello"))
        assertNull(normalizeProfileLinkUrl(""))
        assertNull(normalizeProfileLinkUrl("example.com."))
        assertNull(normalizeProfileLinkUrl("example.c0m"))
        assertNull(normalizeProfileLinkUrl("javascript:alert(1)"))
        assertNull(normalizeProfileLinkUrl("mailto:kat@ko-fi.com"))
        assertNull(normalizeProfileLinkUrl("ftp://example.com"))
        assertNull(normalizeProfileLinkUrl("https://paypal.com@example.com"))
    }

    @Test
    fun validate_flagsEmptyInput() {
        assertIs<ProfileLinkValidation.Empty>(validate("  "))
    }

    @Test
    fun validate_flagsHttpBeforeTheHostIsDone() {
        assertIs<ProfileLinkValidation.Insecure>(validate("http:"))
        assertIs<ProfileLinkValidation.Insecure>(validate("http://example.com"))
        assertIs<ProfileLinkValidation.Insecure>(validate("HTTP://example.com"))
    }

    @Test
    fun validate_leavesAdultPlatformsToLabels() {
        assertIs<ProfileLinkValidation.Valid>(validate("https://onlyfans.com/kat"))
        assertIs<ProfileLinkValidation.Valid>(validate("www.fansly.com/kat"))
    }

    @Test
    fun validate_flagsLinkHostsAsShortenersWithTheirSubdomains() {
        assertIs<ProfileLinkValidation.Shortener>(validate("https://videy.at/v/abc"))
        assertIs<ProfileLinkValidation.Shortener>(validate("https://www.onlylinks.cc/kat"))
    }

    @Test
    fun validate_doesNotFlagLookalikes() {
        assertIs<ProfileLinkValidation.Valid>(validate("https://notvidey.at/kat"))
        assertIs<ProfileLinkValidation.Valid>(validate("https://videy.at.example.net/kat"))
    }

    @Test
    fun validate_flagsShortenersButNotBrandShortLinks() {
        assertIs<ProfileLinkValidation.Shortener>(validate("https://bit.ly/abc"))
        assertIs<ProfileLinkValidation.Shortener>(validate("t.co/abc"))
        assertIs<ProfileLinkValidation.Valid>(validate("https://youtu.be/abc"))
    }

    @Test
    fun validate_flagsDuplicatesAfterNormalizing() {
        assertIs<ProfileLinkValidation.Duplicate>(
            validateProfileLinkInput(
                input = "Ko-fi.com/kat",
                existingUrls = listOf(GenericUri("https://ko-fi.com/kat")),
            ),
        )
    }

    @Test
    fun validate_returnsTheNormalizedUrl() {
        val validation = validate("ko-fi.com/kat")
        assertIs<ProfileLinkValidation.Valid>(validation)
        assertEquals(
            expected = GenericUri("https://ko-fi.com/kat"),
            actual = validation.url,
        )
    }

    @Test
    fun isShowable_rejectsInsecureAndShortenedLinks() {
        assertTrue(GenericUri("https://ko-fi.com/kat").isShowableProfileLink())
        assertFalse(GenericUri("http://ko-fi.com/kat").isShowableProfileLink())
        assertTrue(GenericUri("https://onlyfans.com/kat").isShowableProfileLink())
        assertFalse(GenericUri("https://videy.at/v/abc").isShowableProfileLink())
        assertFalse(GenericUri("https://bit.ly/abc").isShowableProfileLink())
        assertFalse(GenericUri("not a url").isShowableProfileLink())
    }

    private fun validate(
        input: String,
    ) = validateProfileLinkInput(
        input = input,
        existingUrls = emptyList(),
    )
}
