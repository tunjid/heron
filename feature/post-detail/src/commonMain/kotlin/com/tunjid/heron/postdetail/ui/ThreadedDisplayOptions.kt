package com.tunjid.heron.postdetail.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import heron.ui.core.generated.resources.timeline_thread_linear
import heron.ui.core.generated.resources.timeline_thread_newest_first
import heron.ui.core.generated.resources.timeline_thread_oldest_first
import heron.ui.core.generated.resources.timeline_thread_order
import heron.ui.core.generated.resources.timeline_thread_reply_sorting_title
import heron.ui.core.generated.resources.timeline_thread_show_replies_as_title
import heron.ui.core.generated.resources.timeline_thread_threaded
import heron.ui.core.generated.resources.timeline_thread_top_first
import org.jetbrains.compose.resources.stringResource

@Composable
fun ThreadDisplayOptions(
    modifier: Modifier = Modifier,
    order: TimelineItem.Threaded.Order?,
    viewMode: TimelineItem.Threaded.ViewMode,
    onOrderChanged: (TimelineItem.Threaded.Order) -> Unit,
    onViewModeChanged: (TimelineItem.Threaded.ViewMode) -> Unit,
) {
    Box(
        modifier = modifier,
    ) {
        var expanded by remember { mutableStateOf(false) }
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
                .padding(5.dp)
                .widthIn(min = 200.dp),
        ) {
            TimelineViewMode(
                replyViewMode = viewMode,
                onViewModeChanged = {
                    expanded = false
                    onViewModeChanged(it)
                },
            )
            order?.let { order ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                TimelineOrder(
                    order = order,
                    onOrderChanged = {
                        expanded = false
                        onOrderChanged(it)
                    },
                )
            }
        }
    }
}

@Composable
private fun TimelineViewMode(
    replyViewMode: TimelineItem.Threaded.ViewMode,
    onViewModeChanged: (TimelineItem.Threaded.ViewMode) -> Unit,
) {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        text = stringResource(CommonStrings.timeline_thread_show_replies_as_title),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
    )
    TimelineItem.Threaded.ViewMode.entries.forEach { mode ->
        DropdownMenuItem(
            onClick = { onViewModeChanged(mode) },
            text = {
                Text(
                    text = stringResource(
                        when (mode) {
                            TimelineItem.Threaded.ViewMode.Linear -> CommonStrings.timeline_thread_linear
                            TimelineItem.Threaded.ViewMode.Tree -> CommonStrings.timeline_thread_threaded
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            leadingIcon = {
                RadioButton(
                    selected = mode == replyViewMode,
                    onClick = null,
                )
            },
            modifier = Modifier.height(48.dp),
        )
    }
}

@Composable
private fun TimelineOrder(
    order: TimelineItem.Threaded.Order,
    onOrderChanged: (TimelineItem.Threaded.Order) -> Unit,
) {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        text = stringResource(CommonStrings.timeline_thread_reply_sorting_title),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
    )
    TimelineItem.Threaded.Order.entries.forEach { item ->
        DropdownMenuItem(
            onClick = { onOrderChanged(item) },
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
            modifier = Modifier.height(48.dp),
        )
    }
}
