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

package com.tunjid.heron.postdetail.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.ui.AppBarButton
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.timeline_thread_newest_first
import heron.ui.core.generated.resources.timeline_thread_oldest_first
import heron.ui.core.generated.resources.timeline_thread_order
import heron.ui.core.generated.resources.timeline_thread_top_first
import org.jetbrains.compose.resources.stringResource

@Composable
fun TimelineOrder(
    order: TimelineItem.Threaded.Order?,
    onOrderChanged: (TimelineItem.Threaded.Order) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    order?.let {
        AppBarButton(
            icon = Icons.AutoMirrored.Rounded.Sort,
            iconDescription = stringResource(CommonStrings.timeline_thread_order),
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .widthIn(min = 200.dp),
        ) {
            TimelineItem.Threaded.Order.entries.forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onOrderChanged(item)
                    },
                    text = {
                        Text(
                            text = stringResource(
                                when (item) {
                                    TimelineItem.Threaded.Order.Oldest -> CommonStrings.timeline_thread_oldest_first
                                    TimelineItem.Threaded.Order.Newest -> CommonStrings.timeline_thread_newest_first
                                    TimelineItem.Threaded.Order.Top -> CommonStrings.timeline_thread_top_first
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    leadingIcon = {
                        RadioButton(
                            selected = item == order,
                            onClick = null,
                        )
                    },
                    modifier = Modifier
                        .height(48.dp),
                )
            }
        }
    }
}
