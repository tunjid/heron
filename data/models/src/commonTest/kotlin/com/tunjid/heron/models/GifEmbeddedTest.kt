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

import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.Gif
import com.tunjid.heron.data.core.models.asEmbeddedGifOrNull
import com.tunjid.heron.data.core.models.asLinkPreview
import com.tunjid.heron.data.core.models.displayUri
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GifEmbeddedTest {

    @Test
    fun klipyLinkPreview_roundTrips() {
        val gif = klipyGif()
            .asLinkPreview()
            .embed
            .asEmbeddedGifOrNull()

        assertNotNull(gif)
        assertEquals(
            expected = Gif.Embedded.Provider.Klipy,
            actual = gif.provider,
        )
        assertEquals(
            expected = ImageUri("https://k.gifs.bsky.app/ii/abc123/def456/gif-slug.gif"),
            actual = gif.playbackUri,
        )
        assertEquals(
            expected = 480L,
            actual = gif.width,
        )
        assertEquals(
            expected = 270L,
            actual = gif.height,
        )
        assertEquals(
            expected = GenericUri("https://k.gifs.bsky.app/ii/abc123/def456/mp4-slug.mp4"),
            actual = gif.mp4,
        )
        assertEquals(
            expected = GenericUri("https://k.gifs.bsky.app/ii/abc123/def456/webm-slug.webm"),
            actual = gif.webm,
        )
        assertEquals(
            expected = "A raccoon saying oh goodness",
            actual = gif.altText,
        )
        assertFalse(gif.isUserAltText)
    }

    @Test
    fun klipyLinkPreview_withUserAltText_roundTrips() {
        val gif = klipyGif()
            .asLinkPreview(altText = "Me, every morning")
            .embed
            .asEmbeddedGifOrNull()

        assertNotNull(gif)
        assertEquals(
            expected = "Me, every morning",
            actual = gif.altText,
        )
        assertTrue(gif.isUserAltText)
    }

    @Test
    fun klipy_withoutValidDimensions_fallsBackToUnknown() {
        listOf(
            "https://static.klipy.com/ii/abc123/def456/gif-slug.gif?hh=270",
            "https://static.klipy.com/ii/abc123/def456/gif-slug.gif?hh=0&ww=480",
        ).forEach { uri ->
            val gif = external(uri).asEmbeddedGifOrNull()

            assertNotNull(gif, uri)
            assertEquals(
                expected = Gif.Embedded.Provider.Unknown,
                actual = gif.provider,
                message = uri,
            )
            assertNull(gif.aspectRatio, uri)
        }
    }

    @Test
    fun klipy_outsideIiPath_fallsBackToUnknown() {
        val gif = external("https://static.klipy.com/other/gif-slug.gif?hh=270&ww=480").asEmbeddedGifOrNull()

        assertNotNull(gif)
        assertEquals(
            expected = Gif.Embedded.Provider.Unknown,
            actual = gif.provider,
        )
    }

    @Test
    fun tenor_isProxiedWithVideoVariants() {
        val gif = external(
            uri = "https://media.tenor.com/xyzAAAAC/raccoon.gif?hh=200&ww=300",
            description = "ALT: A raccoon",
        ).asEmbeddedGifOrNull()

        assertNotNull(gif)
        assertEquals(
            expected = Gif.Embedded.Provider.Tenor,
            actual = gif.provider,
        )
        assertEquals(
            expected = ImageUri("https://t.gifs.bsky.app/xyzAAAAM/raccoon.gif"),
            actual = gif.playbackUri,
        )
        assertEquals(
            expected = GenericUri("https://t.gifs.bsky.app/xyzAAAP1/raccoon.mp4"),
            actual = gif.mp4,
        )
        assertEquals(
            expected = GenericUri("https://t.gifs.bsky.app/xyzAAAP3/raccoon.webm"),
            actual = gif.webm,
        )
        assertEquals(
            expected = 1.5f,
            actual = gif.aspectRatio,
        )
        assertEquals(
            expected = "A raccoon",
            actual = gif.altText,
        )
    }

    @Test
    fun tenor_withoutGifMarker_fallsBackToUnknown() {
        val gif = external("https://media.tenor.com/xyzAAAAM/raccoon.gif?hh=200&ww=300").asEmbeddedGifOrNull()

        assertNotNull(gif)
        assertEquals(
            expected = Gif.Embedded.Provider.Unknown,
            actual = gif.provider,
        )
    }

    @Test
    fun giphy_urlsResolveToWebpPlayback() {
        listOf(
            "https://giphy.com/gifs/funny-raccoon-abc123",
            "https://media.giphy.com/media/abc123/giphy.gif",
            "https://media2.giphy.com/media/v1.tracking/abc123/giphy.gif",
            "https://i.giphy.com/media/abc123.webp",
            "https://i.giphy.com/abc123.gif",
        ).forEach { uri ->
            val gif = external(uri).asEmbeddedGifOrNull()

            assertNotNull(gif, uri)
            assertEquals(
                expected = Gif.Embedded.Provider.Giphy,
                actual = gif.provider,
                message = uri,
            )
            assertEquals(
                expected = ImageUri("https://i.giphy.com/media/abc123/200.webp"),
                actual = gif.playbackUri,
                message = uri,
            )
            assertNull(gif.aspectRatio, uri)
        }
    }

    @Test
    fun plainGifLink_isUnknownGif() {
        val gif = external(
            uri = "https://example.com/animations/wave.GIF",
            title = "Wave",
            description = "A waving hand",
        ).asEmbeddedGifOrNull()

        assertNotNull(gif)
        assertEquals(
            expected = Gif.Embedded.Provider.Unknown,
            actual = gif.provider,
        )
        assertEquals(
            expected = ImageUri("https://example.com/animations/wave.GIF"),
            actual = gif.playbackUri,
        )
        assertEquals(
            expected = "Wave",
            actual = gif.altText,
        )
    }

    @Test
    fun regularLinks_areNotGifs() {
        assertNull(external("https://example.com/article").asEmbeddedGifOrNull())
        assertNull(external("https://giphy.com/explore/raccoons").asEmbeddedGifOrNull())
        assertNull(external("not a url").asEmbeddedGifOrNull())
    }

    @Test
    fun displayUri_proxiesProviderCdns() {
        assertEquals(
            expected = ImageUri("https://k.gifs.bsky.app/ii/abc123/def456/tiny.gif"),
            actual = media("https://static.klipy.com/ii/abc123/def456/tiny.gif").displayUri,
        )
        assertEquals(
            expected = ImageUri("https://t.gifs.bsky.app/xyzAAAAM/raccoon.gif"),
            actual = media("https://media.tenor.com/xyzAAAAM/raccoon.gif").displayUri,
        )
        assertEquals(
            expected = ImageUri("https://example.com/tiny.gif"),
            actual = media("https://example.com/tiny.gif").displayUri,
        )
    }

    private fun external(
        uri: String,
        title: String = "",
        description: String = "",
    ) = ExternalEmbed(
        uri = GenericUri(uri),
        title = title,
        description = description,
        thumb = null,
    )

    private fun media(
        uri: String,
    ) = Gif.Media(
        uri = GenericUri(uri),
        width = 100,
        height = 100,
    )

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
