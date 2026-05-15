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
import com.tunjid.heron.data.core.models.RockskyTrack
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.RecordLayout
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.played_n_times
import heron.ui.timeline.generated.resources.track_by
import org.jetbrains.compose.resources.stringResource

@Composable
fun RockskyTrack(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    sharedElementPrefix: String,
    track: RockskyTrack,
) = with(paneTransitionScope) {
    RecordLayout(
        modifier = modifier,
        paneTransitionScope = paneTransitionScope,
        title = track.title,
        subtitle = stringResource(
            Res.string.track_by,
            track.artist,
        ),
        description = track.album,
        blurb = trackBlurb(track),
        sharedElementPrefix = sharedElementPrefix,
        sharedElementType = track.uri,
        avatar = {
            RockSkyAvatar(
                image = track.albumArt,
                uri = track.uri,
                sharedElementPrefix = sharedElementPrefix,
            )
        },
    )
}

@Composable
private fun trackBlurb(track: RockskyTrack): String? {
    track.duration?.let { return formatDuration(it) }
    return track.playCount?.let { count ->
        stringResource(Res.string.played_n_times, format(count))
    }
}

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val secondsString = if (seconds < 10) "0$seconds" else seconds.toString()
    return "$minutes:$secondsString"
}
