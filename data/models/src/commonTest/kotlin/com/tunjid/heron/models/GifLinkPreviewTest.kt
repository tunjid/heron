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

import com.tunjid.heron.data.core.models.Gif
import com.tunjid.heron.data.core.models.asLinkPreview
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import kotlin.test.Test
import kotlin.test.assertEquals

class GifLinkPreviewTest {

    @Test
    fun klipyGif_embedsDimensionsAndVideoSlugs() {
        val preview = klipyGif().asLinkPreview()

        assertEquals(
            expected = "https://static.klipy.com/ii/abc123/def456/gif-slug.gif?hh=270&ww=480&mp4=mp4-slug&webm=webm-slug",
            actual = preview.embed.uri.uri,
        )
        assertEquals(
            expected = ImageUri("https://static.klipy.com/ii/abc123/def456/preview.jpg"),
            actual = preview.embed.thumb,
        )
    }

    @Test
    fun klipyGif_withoutVideoEncodings_embedsDimensionsOnly() {
        val preview = klipyGif()
            .copy(
                mp4 = null,
                webm = null,
            )
            .asLinkPreview()

        assertEquals(
            expected = "https://static.klipy.com/ii/abc123/def456/gif-slug.gif?hh=270&ww=480",
            actual = preview.embed.uri.uri,
        )
    }

    @Test
    fun nonKlipyGif_omitsVideoSlugs() {
        val preview = klipyGif()
            .copy(
                gif = Gif.Media(
                    uri = GenericUri("https://media.giphy.com/media/xyz/giphy.gif"),
                    width = 480,
                    height = 270,
                ),
            )
            .asLinkPreview()

        assertEquals(
            expected = "https://media.giphy.com/media/xyz/giphy.gif?hh=270&ww=480",
            actual = preview.embed.uri.uri,
        )
    }

    @Test
    fun defaultAltText_usesProviderDescription() {
        val preview = klipyGif().asLinkPreview()

        assertEquals(
            expected = "A raccoon saying oh goodness",
            actual = preview.embed.title,
        )
        assertEquals(
            expected = "ALT: A raccoon saying oh goodness",
            actual = preview.embed.description,
        )
    }

    @Test
    fun userAltText_takesPrecedence() {
        val preview = klipyGif().asLinkPreview(
            altText = "  Me, every morning  ",
        )

        assertEquals(
            expected = "Me, every morning",
            actual = preview.embed.title,
        )
        assertEquals(
            expected = "Alt: Me, every morning",
            actual = preview.embed.description,
        )
    }

    private fun klipyGif() = Gif(
        id = "123",
        title = "Oh goodness",
        description = "A raccoon saying oh goodness",
        gif = Gif.Media(
            uri = GenericUri("https://static.klipy.com/ii/abc123/def456/gif-slug.gif"),
            width = 480,
            height = 270,
        ),
        preview = Gif.Media(
            uri = GenericUri("https://static.klipy.com/ii/abc123/def456/preview.jpg"),
            width = 480,
            height = 270,
        ),
        tinyGif = null,
        mp4 = Gif.Media(
            uri = GenericUri("https://static.klipy.com/ii/abc123/def456/mp4-slug.mp4"),
            width = 480,
            height = 270,
        ),
        webm = Gif.Media(
            uri = GenericUri("https://static.klipy.com/ii/abc123/def456/webm-slug.webm"),
            width = 480,
            height = 270,
        ),
    )
}
