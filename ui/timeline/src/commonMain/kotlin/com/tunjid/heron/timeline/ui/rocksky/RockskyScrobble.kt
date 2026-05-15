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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.core.models.RockskyScrobble
import com.tunjid.heron.timeline.utilities.roundComponent
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.RecordLayout
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.scrobbled_ago
import heron.ui.timeline.generated.resources.scrobbled_by
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource

@Composable
fun RockskyScrobble(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    sharedElementPrefix: String,
    scrobble: RockskyScrobble,
) = with(paneTransitionScope) {
    RecordLayout(
        modifier = modifier,
        paneTransitionScope = paneTransitionScope,
        title = scrobble.title,
        subtitle = scrobble.handle
            ?.let { stringResource(Res.string.scrobbled_by, it.id) }
            ?: scrobble.artist,
        description = dotSeparatedText(
            preText = scrobble.album,
            postText = stringResource(
                Res.string.scrobbled_ago,
                remember(scrobble.createdAt) {
                    (Clock.System.now() - scrobble.createdAt).roundComponent()
                },
            ),
        ),
        blurb = null,
        sharedElementPrefix = sharedElementPrefix,
        sharedElementType = scrobble.uri,
        avatar = {
            RockSkyAvatar(
                image = scrobble.albumArt,
                uri = scrobble.uri,
                sharedElementPrefix = sharedElementPrefix,
            )
        },
    )
}
