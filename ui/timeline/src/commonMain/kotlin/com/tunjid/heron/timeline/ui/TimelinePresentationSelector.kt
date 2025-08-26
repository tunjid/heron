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

package com.tunjid.heron.timeline.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.heron.ItemSelection
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.timeline.utilities.icon
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.condensed_media
import heron.ui.timeline.generated.resources.expanded_media
import heron.ui.timeline.generated.resources.text_and_embeds

@Composable
fun TimelinePresentationSelector(
    modifier: Modifier = Modifier,
    selected: Timeline.Presentation,
    available: List<Timeline.Presentation>,
    onPresentationSelected: (Timeline.Presentation) -> Unit,
) {
    ItemSelection(
        modifier = modifier,
        selectedItem = selected,
        availableItems = available,
        key = Timeline.Presentation::key,
        icon = Timeline.Presentation::icon,
        stringResource = Timeline.Presentation::textResource,
        onItemSelected = onPresentationSelected,
    )
}

private fun Timeline.Presentation.textResource() =
    when (this) {
        Timeline.Presentation.Text.WithEmbed -> Res.string.text_and_embeds
        Timeline.Presentation.Media.Condensed -> Res.string.condensed_media
        Timeline.Presentation.Media.Expanded -> Res.string.expanded_media
    }
