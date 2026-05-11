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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.core.models.RockSkyArtist
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.RecordLayout
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.artist_subtitle
import heron.ui.timeline.generated.resources.n_listeners
import org.jetbrains.compose.resources.stringResource

@Composable
fun RockskyArtist(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    sharedElementPrefix: String,
    artist: RockSkyArtist,
) = with(paneTransitionScope) {
    RecordLayout(
        modifier = modifier,
        paneTransitionScope = paneTransitionScope,
        title = artist.name,
        subtitle = stringResource(Res.string.artist_subtitle),
        description = artist.tags
            ?.takeIf(List<String>::isNotEmpty)
            ?.take(3)
            ?.joinToString(" • "),
        blurb = artist.uniqueListeners?.let { count ->
            stringResource(
                Res.string.n_listeners,
                format(count),
            )
        },
        sharedElementPrefix = sharedElementPrefix,
        sharedElementType = artist.uri,
        avatar = {
            RockSkyAvatar(
                image = artist.picture,
                uri = artist.uri,
                sharedElementPrefix = sharedElementPrefix,
            )
        },
    )
}
