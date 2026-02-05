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

package com.tunjid.heron.settings.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.core.models.Preferences
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.auto_play_timeline_videos
import heron.feature.settings.generated.resources.content_and_media
import heron.feature.settings.generated.resources.refresh_timelines_on_launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun ContentAndMediaItem(
    modifier: Modifier = Modifier,
    signedInProfilePreferences: Preferences,
    setRefreshHomeTimelineOnLaunch: (Boolean) -> Unit,
    setAutoplayTimelineVideos: (Boolean) -> Unit,
) {
    ExpandableSettingsItemRow(
        modifier = modifier
            .fillMaxWidth(),
        title = stringResource(Res.string.content_and_media),
        icon = Icons.Rounded.Newspaper,
    ) {
        SettingsToggleItem(
            modifier = Modifier
                .fillMaxWidth(),
            text = stringResource(Res.string.refresh_timelines_on_launch),
            enabled = true,
            checked = signedInProfilePreferences.local.refreshHomeTimelineOnLaunch,
            onCheckedChange = setRefreshHomeTimelineOnLaunch,
        )
        SettingsToggleItem(
            modifier = Modifier
                .fillMaxWidth(),
            text = stringResource(Res.string.auto_play_timeline_videos),
            enabled = true,
            checked = signedInProfilePreferences.local.autoPlayTimelineVideos,
            onCheckedChange = setAutoplayTimelineVideos,
        )
    }
}
