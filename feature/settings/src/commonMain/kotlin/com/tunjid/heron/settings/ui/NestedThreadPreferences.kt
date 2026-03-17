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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ThreadViewPreference
import com.tunjid.heron.data.core.models.ThreadViewPreference.Companion.order
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.timeline_thread_newest_first
import heron.ui.core.generated.resources.timeline_thread_oldest_first
import heron.ui.core.generated.resources.timeline_thread_order
import heron.ui.core.generated.resources.timeline_thread_top_first
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ThreadPreferencesSection(
    modifier: Modifier = Modifier,
    threadViewPreference: ThreadViewPreference?,
    onPreferenceUpdated: (ThreadViewPreference) -> Unit,
) {
    Column(
        modifier = modifier
            .padding(
                horizontal = 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(CommonStrings.timeline_thread_order),
        )
        SettingsRadioButtons(
            selectedItem = threadViewPreference.order(),
            items = TimelineItem.Threaded.Order.entries,
            itemStringResource = TimelineItem.Threaded.Order::stringResource,
            onItemClicked = {
                onPreferenceUpdated(ThreadViewPreference(sort = it.value))
            },
        )
    }
}

private fun TimelineItem.Threaded.Order.stringResource(): StringResource =
    when (this) {
        TimelineItem.Threaded.Order.Oldest -> CommonStrings.timeline_thread_oldest_first
        TimelineItem.Threaded.Order.Newest -> CommonStrings.timeline_thread_newest_first
        TimelineItem.Threaded.Order.Top -> CommonStrings.timeline_thread_top_first
    }
