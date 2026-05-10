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

package com.tunjid.heron.timeline.ui.rocksky

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.tunjid.heron.data.core.models.RockSkyAlbum
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.LabelIconSize
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.apple_music
import heron.ui.timeline.generated.resources.spotify
import heron.ui.timeline.generated.resources.tidal
import heron.ui.timeline.generated.resources.youtube
import org.jetbrains.compose.resources.StringResource

internal val SpotifyIconUri = ImageUri(
    "https://storage.googleapis.com/pr-newsroom-wp/1/2023/05/Spotify_Primary_Logo_RGB_Green.png",
)
internal val TidalIconUri = ImageUri(
    "https://w7.pngwing.com/pngs/289/884/png-transparent-tidal-logos-brands-icon-thumbnail.png",
)
internal val YouTubeIconUri = ImageUri(
    "https://www.gstatic.com/marketing-cms/assets/images/96/ff/14b02dc0467e8875e062e9565cbd/external-icon-core-1.png=n-w1860-h1047-fcrop64=1,00000000ffffffff-rw",
)

internal val RockSkyAlbum.hasMusicServiceLink: Boolean
    get() = spotifyLink != null ||
        appleMusicLink != null ||
        tidalLink != null ||
        youtubeLink != null

internal inline fun RockSkyAlbum.forEachMusicServiceLink(
    block: (label: StringResource, iconUri: ImageUri?, url: String) -> Unit,
) {
    spotifyLink?.let { block(Res.string.spotify, SpotifyIconUri, it) }
    // Apple's brand guidelines do not allow for showing the icon alone
    appleMusicLink?.let { block(Res.string.apple_music, null, it) }
    tidalLink?.let { block(Res.string.tidal, TidalIconUri, it) }
    youtubeLink?.let { block(Res.string.youtube, YouTubeIconUri, it) }
}

@Composable
internal fun MusicServiceIcon(
    iconUri: ImageUri?,
) {
    if (iconUri != null) AsyncImage(
        modifier = Modifier
            .size(LabelIconSize),
        args = remember(iconUri) {
            ImageArgs(
                url = iconUri.uri,
                contentScale = ContentScale.Fit,
                contentDescription = null,
                shape = RoundedPolygonShape.Circle,
            )
        },
    )
}
