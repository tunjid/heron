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

import androidx.compose.foundation.layout.padding
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

@Composable
fun ThreadPreferencesSection(
    threadViewPreference: ThreadViewPreference?,
    onPreferenceUpdated: (ThreadViewPreference) -> Unit,
) {
    SettingsRadioButtons(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        title = CommonStrings.timeline_thread_order,
        selectedItem = threadViewPreference.order(),
        items = TimelineItem.Threaded.Order.entries,
        itemStringResource = TimelineItem.Threaded.Order::stringResource,
        onItemClicked = {
            onPreferenceUpdated(ThreadViewPreference(sort = it.value))
        },
    )
}

private fun TimelineItem.Threaded.Order.stringResource(): StringResource =
    when (this) {
        TimelineItem.Threaded.Order.Oldest -> CommonStrings.timeline_thread_oldest_first
        TimelineItem.Threaded.Order.Newest -> CommonStrings.timeline_thread_newest_first
        TimelineItem.Threaded.Order.Top -> CommonStrings.timeline_thread_top_first
    }
